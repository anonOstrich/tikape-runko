package tikape.runko;

import java.sql.SQLException;
import java.util.*;
import spark.ModelAndView;
import static spark.Spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;
import tikape.runko.database.*;
import tikape.runko.domain.*;

public class Main {

    public static void main(String[] args) throws Exception {

        if (System.getenv("PORT") != null) {
            port(Integer.valueOf(System.getenv("PORT")));
        }

        Database database = new Database(selvitaJdbcOsoite());
        KeskustelualueDao keskustelualueDao = new KeskustelualueDao(database);
        KeskustelunavausDao keskustelunavausDao = new KeskustelunavausDao(database, keskustelualueDao);
        ViestiDao viestiDao = new ViestiDao(database);

        //ALUEIDEN LISTAAMINEN ETUSIVULLA
        get("/", (req, res) -> {
            HashMap<String, Object> data = new HashMap();
            List<List<Object>> nakyma = keskustelualueDao.createView();

            data.put("alueet", nakyma.get(0));
            data.put("viestienLukumaarat", nakyma.get(1));
            data.put("uusimmat", siistiPaivamaarat(nakyma.get(2)));

            return new ModelAndView(data, "index");
        }, new ThymeleafTemplateEngine());

        //UUDEN ALUEEN LISÄÄMINEN
        post("/", (req, res) -> {
            String nimi = req.queryParams("aluenimi");

            Keskustelualue alue = new Keskustelualue(99, nimi);
            boolean onnistui = keskustelualueDao.addNew(alue);

            if (!onnistui) {
                return aiheutaVirheViestilla("Alueella tulee olla nimi!");
            }

            res.redirect("/");
            return null;
        }, new ThymeleafTemplateEngine());

        //AVAUSTEN LISTAAMINEN YHDELLÄ ALUEELLA
        get("/alue/:id", (req, res) -> {
            HashMap<String, Object> data = new HashMap();

            //luodaan Alue-olio osoitteen perusteella, jos mahdollista
            int id = muunnaKokonaisluvuksi(req.params(":id"));
            if (id < 0) {
                return aiheutaVirheViestilla("Ei kelvollinen alueen tunnus.");
            }

            Keskustelualue alue = keskustelualueDao.findOne(id);
            data.put("alue", alue);

            if (alue == null) {
                return aiheutaVirheViestilla("Haettua keskustelualuetta ei löytynyt.");
            }

            // selvitetään, halutaanko näkymä rajata kymmeneen viimeisimpänä käytettyyn avaukseen
            // oletuksena halutaan rajata, joten uudelleenohjataan tähän vaihtoehtoon jos osoite ei sisällä kyseistä
            // parametria lainkaan. 0 = ei rajoiteta, 1 = rajoitetaan. 
            Set<String> urlParametrit = req.queryParams();
            if (!urlParametrit.contains("rajoita")) {
                res.redirect("/alue/" + id + "?rajoita=1");
                return null;
            }

            int rajoiteluku = muunnaKokonaisluvuksi(req.queryParams("rajoita"));
            if (rajoiteluku < 0 || rajoiteluku > 1) {
                return aiheutaVirheViestilla("");
            }
            boolean rajoitus = (rajoiteluku == 1);

            // tuotetaan näkymän luomista varten tarvittavat tiedot. 
            List<List<Object>> nakyma = keskustelunavausDao.createView(id, rajoitus);

            // lisätään Thymeleafin käyttöön tarvittavat viestit rajausmoodin vaihtamista varten. Jos joka tapauksessa
            // korkeintaan 10 avausta, ei tarvetta tarjota vaihtoehtoa. 
            int avauksiaAlueella = keskustelunavausDao.montakoAvaustaAlueella(id);
            lisaaOikeatThymeleafMuuttujat(data, rajoitus, avauksiaAlueella, id);

            data.put("avaukset", nakyma.get(0));
            data.put("viestienLukumaarat", nakyma.get(1));
            data.put("uusimmat", siistiPaivamaarat(nakyma.get(2)));
            return new ModelAndView(data, "avaukset");
        }, new ThymeleafTemplateEngine());

        //NÄYTETÄÄN SIVU VIESTIN LISÄÄMISELLE
        get("/alue/:id/lisaa", (req, res) -> {
            HashMap<String, Object> data = new HashMap();
            int alue_id = muunnaKokonaisluvuksi(req.params(":id"));
            if (alue_id < 0) {
                aiheutaVirheViestilla("Virheellinen aluetunnus");
            }

            data.put("alue", keskustelualueDao.findOne(alue_id));
            return new ModelAndView(data, "avauksenlisays");
        }, new ThymeleafTemplateEngine());

        //LISÄTÄÄN UUSI KESKUSTELUNAVAUS + SEN AVAUSVIESTI
        post("/alue/:id/lisaa", (req, res) -> {
            // alustetaan map mahdollisia virhetiloja varten
            HashMap<String, Object> data = new HashMap();

            int alue_id = muunnaKokonaisluvuksi(req.params(":id"));
            if (alue_id < 0) {
                aiheutaVirheViestilla("Virheellinen aluetunnus");
            }

            String otsikko = req.queryParams("otsikko");
            String sisalto = req.queryParams("sisalto");
            String nimimerkki = req.queryParams("nimimerkki");

            if (otsikko.trim().isEmpty() || sisalto.trim().isEmpty() || nimimerkki.trim().isEmpty()) {
                return aiheutaVirheViestilla("Et täyttänyt kaikkia pakollisia kenttiä.");
            }

            //Luodaan avaus ja viesti.  Palautetaan viestiä vastaavan avauksen id
            int avaus_id = keskustelunavausDao.createFirstMessage(alue_id, otsikko, sisalto, nimimerkki);
            res.redirect("/avaus/" + avaus_id);
            return null;
        }, new ThymeleafTemplateEngine());

        // NÄYTETÄÄN AVAUKSEN VIESTIT
        get("/avaus/:id", (req, res) -> {
            HashMap<String, Object> data = new HashMap();

            int avaus_id = muunnaKokonaisluvuksi(req.params(":id"));
            if (avaus_id < 0) {
                return aiheutaVirheViestilla("Virheellinen avaustunnus");
            }

            // Selvitetään parametrin sivu arvo. Jos parametriä ei ole osoitteessa, uudelleenohjataan sivulle jossa sivu=1.
            // Yhdellä sivulla näytetään korkeintaan 20 vuotta. 
            Set<String> urlParams = req.queryParams();
            if (!urlParams.contains("sivu")) {
                res.redirect("/avaus/" + avaus_id + "?sivu=1");
                return null;
            }

            int naytettavaSivu = muunnaKokonaisluvuksi(req.queryParams("sivu"));
            if (naytettavaSivu <= 0) {
                return aiheutaVirheViestilla("Virheellinen sivunumero");
            }

            Keskustelunavaus avaus = keskustelunavausDao.findOne(avaus_id);
            if (avaus == null) {
                return aiheutaVirheViestilla("Haettua viestiketjua ei löytynyt");
            }
            data.put("avaus", avaus);

            // haetaan halutut max 20 viestiä. 
            List<Viesti> viestit = viestiDao.find20WithAreaId(avaus_id, naytettavaSivu);
            data.put("viestit", viestit);

            int sivujaYhteensa = viestiDao.montakoSivuaAvauksessa(avaus_id);
            System.out.println();
            lisaaTiedotSelauslinkeille(data, sivujaYhteensa, avaus_id, naytettavaSivu);

            //Ei näytetä sivua, jonka numero on korkeampi kuin suurimman sivun. Ohjataan viimeiselle sivulle. 
            if (naytettavaSivu > sivujaYhteensa) {
                res.redirect("/avaus/" + avaus_id + "?sivu=" + sivujaYhteensa);
                return null;
            }

            // Numeroidaan viestit oikein (jotta esim. toisen sivun ensimmäinen viesti on 21, ei 1)
            data.put("ylimmanViestinNumero", 20 * (naytettavaSivu - 1) + 1);

            return new ModelAndView(data, "viestit");
        }, new ThymeleafTemplateEngine());

        // LISÄTÄÄN UUSI VIESTI AVAUKSEEN
        post("/avaus/:id", (req, res) -> {

            int avaus_id = muunnaKokonaisluvuksi(req.params(":id"));
            if (avaus_id < 0) {
                return aiheutaVirheViestilla("");
            }

            String sisalto = req.queryParams("sisalto");
            String nimimerkki = req.queryParams("nimimerkki");

            if (sisalto.trim().isEmpty() || nimimerkki.trim().isEmpty()) {
                res.redirect("/avaus/" + avaus_id);
                return aiheutaVirheViestilla("");
            }

            // lisätään viesti ja uudelleenohjataan avauksen viimeiselle sivulle, 
            // jotta juuri lisätty viesti näkyy varmasti. 
            viestiDao.createNewMessage(sisalto, nimimerkki, avaus_id);
            int viimeinenSivu = viestiDao.montakoSivuaAvauksessa(avaus_id);
            res.redirect("/avaus/" + avaus_id + "?sivu=" + viimeinenSivu);
            return null;
        }, new ThymeleafTemplateEngine());
    }

    public static List<String> siistiPaivamaarat(List<Object> lista) {
        List<String> palautettava = new ArrayList();

        for (int i = 0; i < lista.size(); i++) {
            String kasiteltava = (String) lista.get(i);
            if (kasiteltava.length() > 15) {
                kasiteltava = kasiteltava.substring(0, 16);
            }
            palautettava.add(kasiteltava);
        }

        return palautettava;
    }

    public static String selvitaJdbcOsoite() {
        if (System.getenv("DATABASE_URL") != null) {
            return System.getenv("DATABASE_URL");
        }
        return "jdbc:sqlite:keskustelupalsta.db";
    }

    public static int muunnaKokonaisluvuksi(String mjono) {
        try {
            return Integer.parseInt(mjono);
        } catch (Exception e) {
            return -1;
        }
    }

    public static void lisaaOikeatThymeleafMuuttujat(HashMap<String, Object> data, boolean rajoitus, int avauksiaAlueella, int alue_id) {
        if (!rajoitus) {
            if (avauksiaAlueella <= 10) {
                data.put("toisenNakymanOsoite", "");
                data.put("nakymanMuutosTeksti", "");
            } else {
                data.put("toisenNakymanOsoite", "/alue/" + alue_id + "?rajoita=1");
                data.put("nakymanMuutosTeksti", "Näytä vähemmän alueita");
            }
        } else {
            if (avauksiaAlueella <= 10) {
                data.put("toisenNakymanOsoite", "");
                data.put("nakymanMuutosTeksti", "");
            } else {
                data.put("tietoPiilotetuista", " (" + (avauksiaAlueella - 10) + " vanhinta piilotettu)");
                data.put("toisenNakymanOsoite", "/alue/" + alue_id + "?rajoita=0");
                data.put("nakymanMuutosTeksti", "Näytä enemmän alueita");
            }
        }

    }

    public static void lisaaTiedotSelauslinkeille(HashMap<String, Object> data, int sivujaYhteensa, int avaus_id, int naytettavaSivu) {

        // linkit edelliseen ja seuraavaan sivuun vain jos tällaisia oikeasti on näyttää
        if (naytettavaSivu > 1) {
            data.put("edellinen", "Edellinen sivu");
            data.put("edellisenOsoite", "/avaus/" + avaus_id + "?sivu=" + (naytettavaSivu - 1));
        } else {
            data.put("edellisenId", "hidden");
        }
        if (naytettavaSivu < sivujaYhteensa) {
            data.put("seuraava", "Seuraava sivu");
            data.put("seuraavanOsoite", "/avaus/" + avaus_id + "?sivu=" + (naytettavaSivu + 1));
        } else {
            data.put("seuraavanId", "hidden");
        }

        //viimeiselle ja ensimmäiselle aina linkit, vaikka oltaisiin jo. 
        data.put("viimeisenOsoite", "/avaus/" + avaus_id + "?sivu=" + sivujaYhteensa);
        data.put("ensimmaisenOsoite", "/avaus/" + avaus_id);
    }

    public static ModelAndView aiheutaVirheViestilla(String viesti) {
        HashMap<String, Object> data = new HashMap();
        data.put("virheviesti", viesti);
        return new ModelAndView(data, "error");
    }

}

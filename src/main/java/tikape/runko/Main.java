package tikape.runko;

import java.util.*;
import spark.ModelAndView;
import static spark.Spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;
import tikape.runko.database.Database;
import tikape.runko.database.KeskustelualueDao;
import tikape.runko.database.KeskustelunavausDao;
import tikape.runko.database.ViestiDao;
import tikape.runko.domain.Keskustelualue;
import tikape.runko.domain.Keskustelunavaus;
import tikape.runko.domain.Viesti;

public class Main {

    public static void main(String[] args) throws Exception {

        if (System.getenv("PORT") != null) {
            port(Integer.valueOf(System.getenv("PORT")));
        }

        String jdbcOsoite = "jdbc:sqlite:keskustelupalsta.db";
        if (System.getenv("DATABASE_URL") != null) {
            jdbcOsoite = System.getenv("DATABASE_URL");
        }

        Database database = new Database(jdbcOsoite);
        //database.init();
        //Onko yllä oleva kommenteissa turhaa?
        KeskustelualueDao keskustelualueDao = new KeskustelualueDao(database);
        KeskustelunavausDao keskustelunavausDao = new KeskustelunavausDao(database, keskustelualueDao);
        ViestiDao viestiDao = new ViestiDao(database);

        //hakee juurta, näyttää index-sivun
        get("/", (req, res) -> {
            HashMap<String, Object> data = new HashMap();

            List<List<Object>> nakyma = keskustelualueDao.createView();

            data.put("alueet", nakyma.get(0));
            data.put("viestienLukumaarat", nakyma.get(1));
            List<String> uusimmat = new ArrayList();

            for (int i = 0; i < nakyma.get(2).size(); i++) {
                String kasiteltava = (String) nakyma.get(2).get(i);
                if (kasiteltava.length() > 15) {
                    kasiteltava = kasiteltava.substring(0, 16);

                }
                uusimmat.add(kasiteltava);
            }

            data.put("uusimmat", uusimmat);

            return new ModelAndView(data, "index");
        }, new ThymeleafTemplateEngine());

        //lisää uuden alueen, päivittää sivun
        post("/", (req, res) -> {
            String nimi = req.queryParams("aluenimi");

            Keskustelualue alue = new Keskustelualue(99, nimi);
            keskustelualueDao.addNew(alue);

            // TODO: 
            // jos lisääminen epäonnistuu, tuota virheilmoitus? (oma sivu / indexissä huomautus)
            res.redirect("/");
            return "";
        });

        //listaa valitun alueen keskustelut
        get("/alue/:id", (req, res) -> {
            HashMap<String, Object> data = new HashMap();

            int id = 0;

            try {
                id = Integer.parseInt(req.params(":id"));
            } catch (Exception e) {
                data.put("virheviesti", "Alueen id ei ollut kokonaisluku.");
                return new ModelAndView(data, "error");
            }

            Keskustelualue alue = keskustelualueDao.findOne(id);

            if (alue == null) {
                data.put("virheviesti", "Haettua keskustelualuetta ei löytynyt.");
                return new ModelAndView(data, "error");
            }

            Set<String> urlParametrit = req.queryParams();
            if (!urlParametrit.contains("rajoita")) {
                res.redirect("/alue/" + id + "?rajoita=1");
            }

            boolean rajoitus = true;

            try {
                int rajoiteluku = Integer.parseInt(req.queryParams("rajoita"));
                rajoitus = (rajoiteluku == 1);
            } catch (Exception e) {
                data.put("virheviesti", "");
                return new ModelAndView(data, "error");
            }

            data.put("alue", alue);

            List<List<Object>> nakyma = keskustelunavausDao.createView(id, rajoitus);
            
            int avauksiaAlueella = keskustelunavausDao.montakoAvaustaAlueella(id);
            
            System.out.println("AVAUKSIA ALUUELLA: " + avauksiaAlueella);
            System.out.println("TULOS: " + (avauksiaAlueella - 10));

            //milloin näytetään mahdollisuus näkymän muuttamiseen: 
            if (!rajoitus) {
                if (avauksiaAlueella <= 10) {
                    data.put("toisenNakymanOsoite", "");
                    data.put("nakymanMuutosTeksti", "");
                } else {
                    data.put("toisenNakymanOsoite", "/alue/" + id + "?rajoita=1");
                    data.put("nakymanMuutosTeksti", "Näytä vähemmän alueita");
                }
            } else {
                if (avauksiaAlueella <= 10) {
                    data.put("toisenNakymanOsoite", "");
                    data.put("nakymanMuutosTeksti", "");
                } else {
                    data.put("tietoPiilotetuista"," (" + (avauksiaAlueella - 10)  +" vanhinta piilotettu)" );
                    data.put("toisenNakymanOsoite", "/alue/" + id + "?rajoita=0");
                    data.put("nakymanMuutosTeksti", "Näytä enemmän alueita");
                }
            }

            data.put("avaukset", nakyma.get(0));
            data.put("viestienLukumaarat", nakyma.get(1));

            List<String> lyhennetytPaivamaarat = new ArrayList();
            for (int i = 0; i < nakyma.get(2).size(); i++) {
                String kasiteltava = (String) nakyma.get(2).get(i);
                if (kasiteltava.length() > 11) {
                    kasiteltava = kasiteltava.substring(0, 10);
                }
                lyhennetytPaivamaarat.add(kasiteltava);
            }

            data.put("uusimmat", lyhennetytPaivamaarat);

            return new ModelAndView(data, "avaukset");
        }, new ThymeleafTemplateEngine());

        //näytettävä sivu, kun luodaan uusi keskustelu alueelle
        get("/alue/:id/lisaa", (req, res) -> {
            HashMap<String, Object> data = new HashMap();
            int alue_id = 0;

            try {
                alue_id = Integer.parseInt(req.params(":id"));
            } catch (Exception e) {
                data.put("virheviesti", "Alueen id ei ollut kokonaisluku.");
                return new ModelAndView(data, "error");
            }

            data.put("alue", keskustelualueDao.findOne(alue_id));

            return new ModelAndView(data, "avauksenlisays");
        }, new ThymeleafTemplateEngine());

        //lisää uuden keskustelunavauksen/viestiketjun
        post("/alue/:id/lisaa", (req, res) -> {
            int alue_id = 0;
            try {
                alue_id = Integer.parseInt(req.params(":id"));
            } catch (Exception e) {
                res.redirect("/");
                return null;
            }

            String otsikko = req.queryParams("otsikko");
            String sisalto = req.queryParams("sisalto");
            String nimimerkki = req.queryParams("nimimerkki");
                       
            if (otsikko.trim().isEmpty() || sisalto.trim().isEmpty() || nimimerkki.trim().isEmpty()) {
                

                HashMap<String, Object> data = new HashMap(); 
                data.put("virheviesti", "VIRHE");
                return new ModelAndView(data, "error"); 
            }

            //luo viestin ja palauttaa viestiä vastaavan avauksen id:n
            int avaus_id = keskustelunavausDao.createFirstMessage(alue_id, otsikko, sisalto, nimimerkki);
            res.redirect("/avaus/" + avaus_id);

            return null;
        }, new ThymeleafTemplateEngine());

        //lisää avattuun viestiketjuun uuden viestin
        post("/avaus/:id", (req, res) -> {

            int avaus_id = 0;

            try {
                avaus_id = Integer.parseInt(req.params(":id"));
            } catch (Exception e) {
                res.redirect("/");
                return null;
            }

            String sisalto = req.queryParams("sisalto");
            String nimimerkki = req.queryParams("nimimerkki");

            if (sisalto.trim().isEmpty() || nimimerkki.trim().isEmpty()) {
                res.redirect("/avaus/" + avaus_id);
            }

            viestiDao.createNewMessage(sisalto, nimimerkki, avaus_id);
            int viestejaAlueella = viestiDao.montakoViestiaAvauksessa(avaus_id); // haettava tieto
            int sivujaYhteensa = viestejaAlueella / 20;

            if (viestejaAlueella % 20 != 0) {
                sivujaYhteensa++;
            }

            res.redirect("/avaus/" + avaus_id + "?sivu=" + sivujaYhteensa);

            return null;
        });

        //hakee valitun viestiketjun
        get("/avaus/:id", (req, res) -> {
            HashMap<String, Object> data = new HashMap();

            int avaus_id = 0;

            try {
                avaus_id = Integer.parseInt(req.params(":id"));
            } catch (Exception e) {
                res.redirect("/");
            }

            //Selvitetään url-parametrit: onko parametria sivu? Jos ei ole, ohjataan oletuksena
            //sivulle /?sivu=1, jolloin näytetään ensimmäiset 20 viestiä. 
            Set<String> urlParams = req.queryParams();
            if (!urlParams.contains("sivu")) {
                res.redirect("/avaus/" + avaus_id + "?sivu=1");
            }

            int naytettavaSivu = -1;

            try {
                naytettavaSivu = Integer.parseInt(req.queryParams("sivu"));
            } catch (Exception e) {
                data.put("virheviesti", "");
                return new ModelAndView(data, "error");
            }

            Keskustelunavaus avaus = keskustelunavausDao.findOne(avaus_id);

            if (avaus == null) {
                data.put("virheviesti", "Haettua viestiketjua ei löytynyt.");
                return new ModelAndView(data, "error");
            }

            data.put("avaus", avaus);

            List<Viesti> viestit = viestiDao.find20WithAreaId(avaus_id, naytettavaSivu);
            data.put("viestit", viestit);

            //muodostetaan jo seuraavan viestisivun osoite, 
            //jos käyttäjä päättää painaa 'seuraava'-nappia viestisivulla. 
            data.put("seuraavanOsoite", "/avaus/" + avaus_id + "?sivu=" + (naytettavaSivu + 1));
            data.put("edellisenOsoite", "/avaus/" + avaus_id + "?sivu=" + (naytettavaSivu - 1));

            //muodostetaan myös viimeisen osoite viimeiselle sivulle. 
            //Siis sivu, jolla on vielä jonkin verran viestejä näytettävänä. 
            int viestejaAlueella = viestiDao.montakoViestiaAvauksessa(avaus_id); // haettava tieto
            int sivujaYhteensa = viestejaAlueella / 20;

            if (viestejaAlueella % 20 != 0) {
                sivujaYhteensa++;
            }

            if (naytettavaSivu > 1) {
                data.put("edellinen", "Edellinen sivu");
            }
            if (naytettavaSivu < sivujaYhteensa) {
                data.put("seuraava", "Seuraava sivu");
            }

            data.put("viimeisenOsoite", "/avaus/" + avaus_id + "?sivu=" + sivujaYhteensa);
            data.put("ensimmaisenOsoite", "/avaus/" + avaus_id);

            //kerrotaan, mistä numeroinnin pitäisi lähteä 
            //(monesko viesti on ensimmäiseksi näytettävä
            data.put("ylimmanViestinNumero", 20 * (naytettavaSivu - 1) + 1);

            //jos yritetään näyttää sivua, joka olisi tyhjä keskusteluista. 
            if (naytettavaSivu > sivujaYhteensa) {
                res.redirect("/avaus/" + avaus_id + "?sivu=" + sivujaYhteensa);
            }

            return new ModelAndView(data, "viestit");
        }, new ThymeleafTemplateEngine());
    }
}

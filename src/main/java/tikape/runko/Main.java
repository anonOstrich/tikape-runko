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

            //id:tä ei käytetä tässä; pitäisikö lisätä konstruktori joka ei tarvitse sitä?
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

            //Halutaan samanlainen näkymä kuin ylempänä, mutta eri tiedoilla??
            int id = 0;
            //Varmistetaan, että int!! 

            try {
                id = Integer.parseInt(req.params(":id"));
            } catch (Exception e) {
                res.redirect("/");
                return null;
            }

            Keskustelualue alue = keskustelualueDao.findOne(id);
            data.put("alue", alue);

            List<List<Object>> nakyma = keskustelunavausDao.createView(id);

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

            for (String pvm : lyhennetytPaivamaarat) {
                System.out.println(pvm);
            }

            data.put("uusimmat", lyhennetytPaivamaarat);

            return new ModelAndView(data, "avaukset");
        }, new ThymeleafTemplateEngine());

        //näytettävä sivu, kun luodaan uusi keskustelu alueelle
        get("/alue/:id/lisaa", (req, res) -> {
            int alue_id = 0;

            try {
                alue_id = Integer.parseInt(req.params(":id"));
            } catch (Exception e) {
                res.redirect("/");
                return null;
            }

            HashMap<String, Object> data = new HashMap();

            //Pitäisikö varautua siihen, että osoitteessa on luku joka ei ole minkään alueen id? Esim. virhesivun tuottaminen omalla virheellään. 
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
                res.redirect("/alue/" + alue_id);
                // tähän jokin virheviesti. 
            }

            //luo viestin ja palauttaa viestiä vastaavan avauksen id:n
            int avaus_id = keskustelunavausDao.createFirstMessage(alue_id, otsikko, sisalto, nimimerkki);
            res.redirect("/avaus/" + avaus_id);

            return null;
        });

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
            res.redirect("/avaus/" + avaus_id);

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

            //Selvitetään url-parametrit: onko parametria sivu? JOs ei ole, ohjataan oletuksena
            //sivulle /?sivu=1, jolloin näytetään ensimmäiset 20 viestiä. 
            Set<String> urlParams = req.queryParams();
            if (!urlParams.contains("sivu")) {
                res.redirect("/avaus/" + avaus_id + "?sivu=1");
            }

            int naytettavaSivu = -1;

            try {
                naytettavaSivu = Integer.parseInt(req.queryParams("sivu"));
            } catch (Exception e) {
                res.redirect("/");
            }

            Keskustelunavaus avaus = keskustelunavausDao.findOne(avaus_id);
            data.put("avaus", avaus);
            
            //yritetään korvata alla oleva kommentoitu metodilla, joka palauttaa vain 20 viestiä halutulta sivulta. 
            List<Viesti> viestit = viestiDao.find20WithAreaId(avaus_id, naytettavaSivu);

            //List<Viesti> viestit = viestiDao.findAllWithAreaId(avaus_id);
            data.put("viestit", viestit);

            return new ModelAndView(data, "viestit");
        }, new ThymeleafTemplateEngine());
    }
}

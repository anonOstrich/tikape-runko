package tikape.runko;

import java.util.*;
import spark.ModelAndView;
import static spark.Spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;
import tikape.runko.database.Database;
import tikape.runko.database.KeskustelualueDao;
import tikape.runko.database.KeskustelunavausDao;
import tikape.runko.domain.Keskustelualue;

public class Main {

    public static void main(String[] args) throws Exception {

        //postgrekamaa
        if (System.getenv("PORT") != null) {
            port(Integer.valueOf(System.getenv("PORT")));
        }

        String jdbcOsoite = "jdbc:sqlite:keskustelupalsta.db";
        if (System.getenv("DATABASE_URL") != null) {
            jdbcOsoite = System.getenv("DATABASE_URL");
        }
        //postgrekamaa ends
        
        Database database = new Database(jdbcOsoite);
        //database.init();
        KeskustelualueDao keskustelualueDao = new KeskustelualueDao(database);
        KeskustelunavausDao keskustelunavausDao = new KeskustelunavausDao(database);

        get("/", (req, res) -> {
            HashMap<String, Object> data = new HashMap();
            // kyseyn tulos: lista, joka sisältää kolme listaa: 
            //jotka muodostavat sarakkeet näkymään.

            List<List<Object>> nakyma = keskustelualueDao.createView();

            data.put("alueet", nakyma.get(0));
            data.put("viestienLukumaarat", nakyma.get(1));
            data.put("uusimmat", nakyma.get(2));

            return new ModelAndView(data, "index");
        }, new ThymeleafTemplateEngine());

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
            data.put("uusimmat", nakyma.get(2));

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

            //TODO: Lisää tietokantaan nämä tiedot. Ohjaa viestiketjuun (vaiheessa 2 ehkä ko. keskustelualueelle?) 
            //ajatus: max(id) ja transaction saattavat auttaa (pitää lisätä sekä alueeksi että ko. alueelle aloitusviesti)
            //Lisätään keskustelualuetauluun ja viestitauluun
            //Ensin lisätään toinen, sitten haetaan? Transaktiolla? Ei voi lisätä vain toista, mikä plussa
            //Ensin lisättäisiin keskustelunavaukseen -> SELECT MAX ID, antaa äsken lisätyn id.n
            //
            keskustelunavausDao.createFirstMessage(alue_id, otsikko, sisalto, nimimerkki);
            res.redirect("/alue/" + alue_id);
            return null;
        });

    }
}

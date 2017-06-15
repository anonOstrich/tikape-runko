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
        Database database = new Database("jdbc:sqlite:keskustelupalsta.db");
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
            data.put("aluenimi", alue.getNimi());

            List<List<Object>> nakyma = keskustelunavausDao.createView(id);

            data.put("avaukset", nakyma.get(0));
            data.put("viestienLukumaarat", nakyma.get(1));
            data.put("uusimmat", nakyma.get(2));

            return new ModelAndView(data, "avaukset");
        }, new ThymeleafTemplateEngine());

    }
}

package tikape.runko;

import java.util.*;
import spark.ModelAndView;
import static spark.Spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;
import tikape.runko.database.Database;
import tikape.runko.database.KeskustelualueDao;
import tikape.runko.database.OpiskelijaDao;
import tikape.runko.domain.Keskustelualue;

public class Main {

    public static void main(String[] args) throws Exception {
        Database database = new Database("jdbc:sqlite:keskustelupalsta.db");
        //database.init();
        KeskustelualueDao keskustelualueDao = new KeskustelualueDao(database);
        

        
        get("/", (req, res) -> {
            HashMap<String, Object> data = new HashMap(); 
            // kyseyn tulos: lista, joka sisältää kolme listaa: 
            //jotka muodostavat sarakkeet näkymään.
            
            List<List<String>> nakyma = keskustelualueDao.createView();
            
            data.put("alueet", nakyma.get(0));
            data.put("viestienLukumaarat", nakyma.get(1));
            data.put("uusimmat", nakyma.get(2));
 
            return new ModelAndView(data, "index");
        }, new ThymeleafTemplateEngine()); 
    }
}

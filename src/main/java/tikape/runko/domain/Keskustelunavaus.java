
package tikape.runko.domain;

import java.util.*;

public class Keskustelunavaus {
    
    private int id;
    private String nimi;
    private Keskustelualue keskustelualue;
    private List<Viesti> viestit;
    
    public Keskustelunavaus(int id, String nimi, Keskustelualue keskustelualue) {
        this.id = id;
        this.nimi = nimi;
        this.keskustelualue = keskustelualue;
        this.viestit = new ArrayList();
    }
    
        public Keskustelunavaus(int id, String nimi) {
            this(id, nimi, null);
    }
        
        public void lisaaViesti(Viesti viesti) {
            this.viestit.add(viesti);
        }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNimi() {
        return nimi;
    }

    public void setNimi(String nimi) {
        this.nimi = nimi;
    }

    public Keskustelualue getKeskustelualue() {
        return keskustelualue;
    }

    public void setKeskustelualue(Keskustelualue keskustelualue) {
        this.keskustelualue = keskustelualue;
    }

    public List<Viesti> getViestit() {
        return viestit;
    }

    public void setViestit(List<Viesti> viestit) {
        this.viestit = viestit;
    }
        
        
}

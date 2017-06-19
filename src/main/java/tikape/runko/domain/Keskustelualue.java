
package tikape.runko.domain;

public class Keskustelualue {
    
    private int id;
    private String nimi;
    
    public Keskustelualue(int id, String nimi) {
        this.id = id;
        this.nimi = nimi;
    }

    //setterit ja getterit alla
    
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
}

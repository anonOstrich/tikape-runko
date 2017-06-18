package tikape.runko.domain;

public class Keskustelunavaus {

    private int id;
    private String nimi;
    private Keskustelualue keskustelualue;

    public Keskustelunavaus(int id, String nimi, Keskustelualue keskustelualue) {
        this.id = id;
        this.nimi = nimi;
        this.keskustelualue = keskustelualue;
    }

    public Keskustelunavaus(int id, String nimi) {
        this(id, nimi, null);
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
}

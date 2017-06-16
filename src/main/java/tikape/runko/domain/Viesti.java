
package tikape.runko.domain;

import java.sql.*;

public class Viesti {
    
    private String sisalto;
    private String nimimerkki;
    private Timestamp aika;
    private Keskustelunavaus keskustelunavaus;

    public Viesti(String sisalto, String nimimerkki, Timestamp aika, Keskustelunavaus keskustelunavaus) {
        this.sisalto = sisalto;
        this.nimimerkki = nimimerkki;
        this.aika = aika;
        this.keskustelunavaus = keskustelunavaus;
    }
    
        public Viesti(String sisalto, String nimimerkki, Timestamp aika) {
        this.sisalto = sisalto;
        this.nimimerkki = nimimerkki;
        this.aika = aika;
        this.keskustelunavaus = null;
    }

    public String getSisalto() {
        return sisalto;
    }

    public void setSisalto(String sisalto) {
        this.sisalto = sisalto;
    }

    public Timestamp getAika() {
        return aika;
    }

    public void setAika(Timestamp aika) {
        this.aika = aika;
    }

    public String getNimimerkki() {
        return nimimerkki;
    }

    public void setNimimerkki(String nimimerkki) {
        this.nimimerkki = nimimerkki;
    }

    public Keskustelunavaus getKeskustelunavaus() {
        return keskustelunavaus;
    }

    public void setKeskustelunavaus(Keskustelunavaus keskustelunavaus) {
        this.keskustelunavaus = keskustelunavaus;
    }   
    
    @Override
    public String toString(){
        return this.sisalto + " t. " + this.nimimerkki;
    }
}

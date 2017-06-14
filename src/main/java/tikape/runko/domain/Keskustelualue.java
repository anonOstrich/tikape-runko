/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tikape.runko.domain;

import java.util.*;

public class Keskustelualue {
    
    private int id;
    private String nimi;
    private List<Keskustelunavaus> keskustelut;
    
    public Keskustelualue(int id, String nimi) {
        this.id = id;
        this.nimi = nimi;
        this.keskustelut = new ArrayList();
    }
    
    public void lisaaKeskustelu(Keskustelunavaus keskustelunavaus) {
        this.keskustelut.add(keskustelunavaus);
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

    public List<Keskustelunavaus> getKeskustelut() {
        return keskustelut;
    }

    public void setKeskustelut(List<Keskustelunavaus> keskustelut) {
        this.keskustelut = keskustelut;
    }
    
}

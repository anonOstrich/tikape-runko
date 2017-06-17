package tikape.runko.database;

import java.sql.Connection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import tikape.runko.domain.Keskustelualue;
import tikape.runko.domain.Keskustelunavaus;

public class KeskustelunavausDao implements Dao<Keskustelunavaus, Integer> {

    private Database database;
    private KeskustelualueDao keskustelualueDao;

    public KeskustelunavausDao(Database database, KeskustelualueDao keskustelualueDao) {
        this.database = database;
        this.keskustelualueDao = keskustelualueDao; 
    }

    @Override
    public List findAll() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    public int createFirstMessage(int alue_id, String avaus_nimi, String sisalto, String nimimerkki) throws SQLException { 
        database.update("BEGIN TRANSACTION;");
        database.update("INSERT INTO Keskustelunavaus(keskustelualue, nimi) VALUES (?, ?);",
                alue_id, avaus_nimi);
        database.update("INSERT INTO Viesti (keskustelunavaus, sisalto, nimimerkki) VALUES((SELECT MAX(id) FROM Keskustelunavaus), ?, ?);",
                sisalto, nimimerkki);
        List<Integer> palautettava =database.queryAndCollect("SELECT MAX(id) from Keskustelunavaus;", rs -> rs.getInt(1));
        database.update("COMMIT;");
        
        return palautettava.get(0);

    }

    public List createView(int id) throws SQLException {
        String query = "SELECT avaus.nimi, COUNT(viesti.sisalto) Viestejä, MAX(viesti.aika) Viimeisin_viesti, avaus.id "
                + "FROM Keskustelunavaus avaus, Keskustelualue alue, Viesti "
                + "WHERE avaus.id = viesti.keskustelunavaus "
                + "AND alue.id = avaus.keskustelualue "
                + "AND alue.id = " + id + " "
                + "GROUP BY avaus.id "
                + "ORDER BY Viimeisin_viesti DESC "
                + "LIMIT 10;";
        //Ylle kenties jonkinlainen muutos kohtaan LIMIT? Johonkin nappi, näytetäänkö kaikki keskustelut 
        //vai esimerkiksi 5, 10, 25, 100, tms? ja tämä arvo annetaan metodille parametrina?
        //(ei käyttäjän syöte)

        Connection conn = database.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        List<List<Object>> listat = new ArrayList();
        List<Object> avaukset = new ArrayList();
        List<Object> lukumaarat = new ArrayList();
        List<Object> uusimmat = new ArrayList();

        while (rs.next()) {
            String avaus_nimi = rs.getString(1);
            int avaus_id = rs.getInt(4);
            Keskustelunavaus avaus = new Keskustelunavaus(avaus_id, avaus_nimi);
            String lkm = "" + rs.getInt(2);
            String uusin = "" + rs.getString(3);

            avaukset.add(avaus);
            lukumaarat.add(lkm);

            if (uusin == null || uusin.length() < 6) {
                uusimmat.add("Ei viestejä");
            } else {
                uusimmat.add(uusin);
            }
        }

        conn.close();

        listat.add(avaukset);
        listat.add(lukumaarat);
        listat.add(uusimmat);

        return listat;
    }

    @Override
    public Keskustelunavaus findOne(Integer key) throws SQLException {
        Connection conn = database.getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Keskustelunavaus WHERE id = ?");
        stmt.setInt(1, key);
        ResultSet rs = stmt.executeQuery();
        rs.next(); 
        int avaus_id = rs.getInt("id");
        String avaus_nimi = rs.getString("nimi");
        int alue_id = rs.getInt("keskustelualue");
        
        rs.close(); 
        stmt.close(); 
        conn.close(); 
        
        Keskustelualue keskustelualue = keskustelualueDao.findOne(alue_id);
        return new Keskustelunavaus(avaus_id, avaus_nimi, keskustelualue);
    }

    @Override
    public void delete(Integer key) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

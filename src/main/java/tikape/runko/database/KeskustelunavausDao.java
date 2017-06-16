package tikape.runko.database;

import java.sql.Connection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KeskustelunavausDao implements Dao {

    private Database database;

    public KeskustelunavausDao(Database database) {
        this.database = database;
    }

    @Override
    public Object findOne(Object key) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List findAll() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(Object key) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void createFirstMessage(int alue_id, String avaus_nimi, String sisalto, String nimimerkki) throws SQLException {
//        String query = "BEGIN TRANSACTION; "
//                + "INSERT INTO Keskustelunavaus(keskustelualue, nimi) VALUES (?, ?);"
//                + "INSERT INTO Viesti VALUES((SELECT MAX(id) FROM Keskustelunavaus), ?, ?, datetime('now'));"
//                + "COMMIT;";   

        // database.update(query, alue_id, avaus_nimi, sisalto, nimimerkki);
        database.update("BEGIN TRANSACTION;");
        database.update("INSERT INTO Keskustelunavaus(keskustelualue, nimi) VALUES (?, ?);",
                alue_id, avaus_nimi);
        database.update("INSERT INTO Viesti VALUES((SELECT MAX(id) FROM Keskustelunavaus), ?, ?, datetime('now'));",
                sisalto, nimimerkki);
        database.update("COMMIT;");

    }

    public List createView(int id) throws SQLException {
        String query = "SELECT avaus.nimi, COUNT(viesti.sisalto) Viestejä, MAX(viesti.aika) Viimeisin_viesti "
                + "FROM Keskustelunavaus avaus, Keskustelualue alue, Viesti "
                + "WHERE avaus.id = viesti.keskustelunavaus "
                + "AND alue.id = avaus.keskustelualue "
                + "AND alue.id = " + id + " "
                + "GROUP BY avaus.id "
                + "ORDER BY Viimeisin_viesti DESC "
                + "LIMIT 10;";

        Connection conn = database.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        List<List<Object>> listat = new ArrayList();
        List<Object> avaukset = new ArrayList();
        List<Object> lukumaarat = new ArrayList();
        List<Object> uusimmat = new ArrayList();

        while (rs.next()) {
            String avaus = rs.getString(1);
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

}

package tikape.runko.database;

import java.util.*;
import java.sql.*;
import tikape.runko.domain.Keskustelualue;

public class KeskustelualueDao implements Dao<Keskustelualue, Integer> {

    private Database database;

    public KeskustelualueDao(Database database) {
        this.database = database;
    }

    @Override
    public Keskustelualue findOne(Integer key) throws SQLException {
        List<Keskustelualue> tulos = database.queryAndCollect("SELECT * FROM Keskustelualue WHERE id = ?;",
                rs -> {
                    Integer id = rs.getInt("id");
                    String nimi = rs.getString("nimi");
                    return new Keskustelualue(id, nimi);
                }, key);

        if (tulos.isEmpty()) {
            return null;
        }
        return tulos.get(0);
    }

    @Override
    public List<Keskustelualue> findAll() throws SQLException {
        return database.queryAndCollect("SELECT * FROM Keskustelualue;",
                rs -> new Keskustelualue(rs.getInt("id"), rs.getString("nimi")));
    }

    @Override
    public void delete(Integer key) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<List<Object>> createView() throws SQLException {
        String query = "SELECT alue.nimi AS alue, alue.id AS id, COUNT(viesti.sisalto) viestejä, MAX(viesti.aika) viimeisin_viesti "
                + "FROM Keskustelualue alue "
                + "LEFT JOIN Keskustelunavaus avaus ON alue.id = avaus.keskustelualue "
                + "LEFT JOIN Viesti ON avaus.id = Viesti.keskustelunavaus "
                + "GROUP BY alue.nimi ORDER BY alue.nimi ASC;";

        Connection connection = database.getConnection();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        List<List<Object>> listat = new ArrayList();
        List<Object> alueet = new ArrayList();
        List<Object> lukumaarat = new ArrayList();
        List<Object> uusimmat = new ArrayList();

        while (rs.next()) {
            // MUUTOS
            String aluenimi = rs.getString("alue");
            int id = rs.getInt("id");
            Keskustelualue alue = new Keskustelualue(id, aluenimi);
            
            // MUUTOS
            
            String lkm = "" + rs.getInt(3);
            String uusin = "" + rs.getString("viimeisin_viesti");

            alueet.add(alue);
            lukumaarat.add(lkm);
            
            if (uusin == null || uusin.length() < 6) {
                uusimmat.add("Ei viestejä");
            } else {
                uusimmat.add(uusin);
            }

        }
        rs.close();
        stmt.close();

        listat.add(alueet);
        listat.add(lukumaarat);
        listat.add(uusimmat);

        return listat;
    }

    public boolean addNew(Keskustelualue alue) {
        if (alue.getNimi().isEmpty()) return false; 
        try {
            database.update("INSERT INTO Keskustelualue(nimi) VALUES (?);", alue.getNimi());
            return true; 
        } catch (Exception e) {
            return false;
        }
    }
}

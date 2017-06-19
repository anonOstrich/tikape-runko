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

    public int createFirstMessage(int alue_id, String avaus_nimi, String sisalto, String nimimerkki) throws SQLException {
        int palautettava = -1;
        if (database.usesPostgres()) {
            palautettava = createFirstMessagePostgres(alue_id, avaus_nimi, sisalto, nimimerkki);
        } else {
            palautettava = createFirstMessageSQLite(alue_id, avaus_nimi, sisalto, nimimerkki);
        }
        return palautettava;
    }
    
    public int createFirstMessagePostgres(int alue_id, String avaus_nimi, String sisalto, String nimimerkki) throws SQLException {
        database.update("BEGIN TRANSACTION;");
        database.update("INSERT INTO Keskustelunavaus(keskustelualue, nimi) VALUES (?, ?);",
                alue_id, avaus_nimi);
        database.update("INSERT INTO Viesti (keskustelunavaus, sisalto, nimimerkki) VALUES((SELECT MAX(id) FROM Keskustelunavaus), ?, ?);",
                sisalto, nimimerkki);
        List<Integer> palautettava = database.queryAndCollect("SELECT MAX(id) from Keskustelunavaus;", rs -> rs.getInt(1));
        database.update("COMMIT;");
        return palautettava.get(0);
    }

    public int createFirstMessageSQLite(int alue_id, String avaus_nimi, String sisalto, String nimimerkki) throws SQLException {
        database.update("INSERT INTO Keskustelunavaus(keskustelualue, nimi) VALUES (?, ?);",
                alue_id, avaus_nimi);
        database.update("INSERT INTO Viesti (keskustelunavaus, sisalto, nimimerkki, aika) VALUES((SELECT MAX(id) FROM Keskustelunavaus), ?, ?, datetime('now'));",
                sisalto, nimimerkki);
        List<Integer> palautettava = database.queryAndCollect("SELECT MAX(id) from Keskustelunavaus;", rs -> rs.getInt(1));
        return palautettava.get(0);
    }

    public List createView(int id, boolean onRajoitettu) throws SQLException {
        String query = "SELECT avaus.nimi, COUNT(viesti.sisalto) Viestejä, MAX(viesti.aika) Viimeisin_viesti, avaus.id "
                + "FROM Keskustelunavaus avaus, Keskustelualue alue, Viesti "
                + "WHERE avaus.id = viesti.keskustelunavaus "
                + "AND alue.id = avaus.keskustelualue "
                + "AND alue.id = " + id + " "
                + "GROUP BY avaus.id "
                + "ORDER BY Viimeisin_viesti DESC ";
        if (onRajoitettu) {
            query += "LIMIT 10";
        }
        
        query += ";";

        Connection conn = database.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        //luodaan lista, ja sille kolme listaa talletettaviksi
        List<List<Object>> listat = new ArrayList();
        List<Object> avaukset = new ArrayList();
        List<Object> lukumaarat = new ArrayList();
        List<Object> uusimmat = new ArrayList();

        //käydään ResultSet läpi ja talletetaan tiedot listoihin
        while (rs.next()) {
            String avaus_nimi = rs.getString(1);
            int avaus_id = rs.getInt(4);
            Keskustelunavaus avaus = new Keskustelunavaus(avaus_id, avaus_nimi);
            String lkm = "" + rs.getInt(2);
            String uusin = "" + rs.getString(3);

            avaukset.add(avaus);
            lukumaarat.add(lkm);

            //jos kolmannen listan tieto on null tai merkkijonon pituus on lyhyempi kuin kuusi
            //on tämä käytännössä sama kuin "ei viestejä", jolloin syötetään listalle tämä arvo
            if (uusin == null || uusin.length() < 6) {
                uusimmat.add("Ei viestejä");
            } else {
                uusimmat.add(uusin);
            }
        }
        rs.close();
        stmt.close();
        conn.close();

        //lisätään kollektiiviseen listaan kolme alilistaa ja palautetaan kollektiivinen
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

        if (!rs.next()) {
            return null;
        }

        int avaus_id = rs.getInt("id");
        String avaus_nimi = rs.getString("nimi");
        int alue_id = rs.getInt("keskustelualue");

        rs.close();
        stmt.close();
        conn.close();

        Keskustelualue keskustelualue = keskustelualueDao.findOne(alue_id);
        return new Keskustelunavaus(avaus_id, avaus_nimi, keskustelualue);
    }

    public int montakoAvaustaAlueella(int alue_id) throws SQLException {
        List<Integer> tulos = database.queryAndCollect("SELECT COUNT(*) FROM Keskustelunavaus WHERE keskustelualue = ?;", rs -> rs.getInt(1), alue_id);
        if (tulos == null || tulos.isEmpty()) {
            return -1;
        }
        return tulos.get(0);
    }

    @Override
    public List findAll() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

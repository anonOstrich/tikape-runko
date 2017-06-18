package tikape.runko.database;

import java.sql.SQLException;
import java.util.List;
import tikape.runko.domain.Viesti;

public class ViestiDao implements Dao<Viesti, Integer> {

    private Database database;

    public ViestiDao(Database database) {
        this.database = database;
    }

    //palauttaa listan sivun page viesteistä, kun viestejä sivulla k kappaletta. 
    public List<Viesti> findkWithAreaId(int area_id,int k,  int page) throws SQLException {
        if (page < 1) {
            page = 1;
        }
        int offset = (page - 1) * k;

        return database.queryAndCollect("SELECT * FROM Viesti WHERE keskustelunavaus = ? LIMIT " + k + " OFFSET " + offset + ";",
                rs -> new Viesti(rs.getString("sisalto"), rs.getString("nimimerkki"), null),
                area_id);
    }

    public void createNewMessage(String sisalto, String nimimerkki, int avaus_id) throws SQLException {
        String query = "";
        if (database.usesPostgres()) {
            query = "INSERT INTO Viesti (keskustelunavaus, sisalto, nimimerkki) VALUES (?, ?, ?)";
        } else {
            query = "INSERT INTO Viesti VALUES(?, ?, ?, datetime('now'));";
        }
        database.update(query, avaus_id, sisalto, nimimerkki);
    }

    public int montakoViestiaAvauksessa(int avaus_id) throws SQLException {
        List<Integer> lkm = database.queryAndCollect("SELECT COUNT(*) AS lukumaara FROM Viesti WHERE keskustelunavaus = ?;",
                rs -> rs.getInt("lukumaara"), avaus_id);

        if (lkm == null) {
            return -1;
        }
        return lkm.get(0);
    }

    public int montakoSivuaAvauksessa(int avaus_id, int k) throws SQLException {
        int viestejaAlueella = montakoViestiaAvauksessa(avaus_id);
        int sivujaYhteensa = viestejaAlueella / k;
        if (viestejaAlueella % k != 0) {
            sivujaYhteensa++;
        }
        return sivujaYhteensa;
    }

    @Override
    public List findAll() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Viesti findOne(Integer key) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

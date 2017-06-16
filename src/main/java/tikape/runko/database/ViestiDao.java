
package tikape.runko.database;

import java.sql.SQLException;
import java.util.List;
import tikape.runko.domain.Keskustelunavaus;
import tikape.runko.domain.Viesti;


public class ViestiDao implements Dao<Viesti, Integer> {
    private Database database; 
    
    public ViestiDao(Database database){
        this.database = database; 
    }



    @Override
    public List findAll() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public List<Viesti> findAllWithAreaId(int area_id) throws SQLException{
        return database.queryAndCollect("SELECT * FROM Viesti WHERE keskustelunavaus = ?;",
                rs -> new Viesti(rs.getString("sisalto"), rs.getString("nimimerkki"), null),
                area_id);   
    }
    
    public void createNewMessage(String sisalto, String nimimerkki, int avaus_id) throws SQLException {
            String query = "INSERT INTO Viesti (keskustelunavaus, sisalto, nimimerkki) VALUES (?, ?, ?)";
            database.update(query, avaus_id, sisalto, nimimerkki);
    }


    @Override
    public Viesti findOne(Integer key) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(Integer key) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

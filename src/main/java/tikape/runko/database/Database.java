package tikape.runko.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private String databaseAddress;
    private Connection connection; 

    public Database(String databaseAddress) throws ClassNotFoundException, SQLException {
        this.databaseAddress = databaseAddress;
        this.connection = getConnection(); 
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseAddress);
    }
    
    public <T> List<T> queryAndCollect(String query, Collector<T> collector, Object... params) throws SQLException{
        List<T> palautus = new ArrayList(); 
        PreparedStatement stmt = this.connection.prepareStatement(query);
        
        for (int i = 0; i < params.length;i++){
            stmt.setObject(i + 1, params[i]);
        }
        
        ResultSet rs = stmt.executeQuery();
        
        while(rs.next()){
            palautus.add(collector.collect(rs));
        }
        
        rs.close(); 
        stmt.close(); 
        
        return palautus; 
    }
    
    public int update(String query, Object... params) throws SQLException{
        PreparedStatement stmt = connection.prepareStatement(query);
        
        for (int i = 0; i < params.length; i++){
            stmt.setObject(i + 1, params[i]);
        }
        
        int palautettava = stmt.executeUpdate();
        stmt.close();         
        return palautettava; 
    }
    
    

    
// Turha alla? 
    
//    public void init() {
//        List<String> lauseet = sqliteLauseet();
//
//        // "try with resources" sulkee resurssin automaattisesti lopuksi
//        try (Connection conn = getConnection()) {
//            Statement st = conn.createStatement();
//
//            // suoritetaan komennot
//            for (String lause : lauseet) {
//                System.out.println("Running command >> " + lause);
//                st.executeUpdate(lause);
//            }
//
//        } catch (Throwable t) {
//            // jos tietokantataulu on jo olemassa, ei komentoja suoriteta
//            System.out.println("Error >> " + t.getMessage());
//        }
//    }
//
//    private List<String> sqliteLauseet() {
//        ArrayList<String> lista = new ArrayList<>();
//
//        // tietokantataulujen luomiseen tarvittavat komennot suoritusjärjestyksessä
//        lista.add("CREATE TABLE Opiskelija (id integer PRIMARY KEY, nimi varchar(255));");
//        lista.add("INSERT INTO Opiskelija (nimi) VALUES ('Platon');");
//        lista.add("INSERT INTO Opiskelija (nimi) VALUES ('Aristoteles');");
//        lista.add("INSERT INTO Opiskelija (nimi) VALUES ('Homeros');");
//
//        return lista;
//    }
}

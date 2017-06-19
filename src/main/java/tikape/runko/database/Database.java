package tikape.runko.database;

import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private String databaseAddress;

    public Database(String databaseAddress) throws ClassNotFoundException, SQLException {
        this.databaseAddress = databaseAddress;
        init();
    }
    
    public boolean usesPostgres(){
        return this.databaseAddress.contains("postgres");
    }

    private void init() {
        List<String> lauseet = null;
        if (this.usesPostgres()) {
            lauseet = postgreLauseet();
        } else {
            lauseet = sqliteLauseet();
        }

        try (Connection conn = getConnection()) {
            Statement st = conn.createStatement();

            for (String lause : lauseet) {
                System.out.println("Running command >> " + lause);
                st.executeUpdate(lause);
            }

        } catch (Throwable t) {
            System.out.println("Error >> " + t.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (this.databaseAddress.contains("postgres")) {
            try {
                URI dbUri = new URI(databaseAddress);

                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

                return DriverManager.getConnection(dbUrl, username, password);
            } catch (Throwable t) {
                System.out.println("Error: " + t.getMessage());
                t.printStackTrace();
            }
        }

        return DriverManager.getConnection(databaseAddress);
    }

    public <T> List<T> queryAndCollect(String query, Collector<T> collector, Object... params) throws SQLException {
        Connection conn = getConnection();
        List<T> palautus = new ArrayList();
        PreparedStatement stmt = conn.prepareStatement(query);

        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }

        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            palautus.add(collector.collect(rs));
        }

        rs.close();
        stmt.close();
        conn.close();

        return palautus;
    }

    public int update(String query, Object... params) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);

        for (int i = 0; i < params.length; i++) {
            System.out.println("Indeksi: " + i);
            stmt.setObject(i + 1, params[i]);
        }

        int palautettava = stmt.executeUpdate();
        stmt.close();
        conn.close();
        return palautettava;
    }

    //Ei DROP TABLE -komentoja, koska tietokanta halutaan pitää yllä.
    //Nämä tulisi lisätä ennen CREATE TABLE -lauseita, jos niitä haluttaisiin käyttää.
    private List<String> postgreLauseet() {
        ArrayList<String> lista = new ArrayList<>();

        lista.add("CREATE TABLE Keskustelualue (id SERIAL PRIMARY KEY, nimi varchar(30) NOT NULL);");
        lista.add("CREATE TABLE Keskustelunavaus (id SERIAL PRIMARY KEY, keskustelualue int references Keskustelualue (id), nimi varchar(60) NOT NULL);");
        lista.add("CREATE TABLE Viesti (keskustelunavaus int references Keskustelunavaus (id), sisalto varchar(2000) NOT NULL, nimimerkki varchar (20) NOT NULL, aika timestamp DEFAULT now());");
        
        return lista;
    }

    private List<String> sqliteLauseet() {
        ArrayList<String> lista = new ArrayList<>();
        lista.add("CREATE TABLE Keskustelualue(id integer PRIMARY KEY, nimi varchar(30) UNIQUE NOT NULL);");
        lista.add("CREATE TABLE Keskustelunavaus(id integer PRIMARY KEY, keskustelualue integer NOT NULL, nimi varchar(60) NOT NULL, FOREIGN KEY(keskustelualue) REFERENCES Keskustelualue(id));");
        lista.add("CREATE TABLE Viesti(keskustelunavaus integer NOT NULL, sisalto varchar(2000) NOT NULL, nimimerkki varchar(20) NOT NULL, aika datetime NOT NULL, FOREIGN KEY(keskustelunavaus) REFERENCES Keskustelunavaus(id));");
        lista.add("INSERT INTO Keskustelualue(nimi) VALUES ('Ohjelmointi');");
        lista.add("INSERT INTO Keskustelualue(nimi) VALUES ('Elokuvat');");
        lista.add("INSERT INTO Keskustelunavaus(keskustelualue) VALUES (1, 'Java on kivaa');");
        lista.add("INSERT INTO Keskustelunavaus(keskustelualue) VALUES (1, 'Python ei ole kivaa');");
        lista.add("INSERT INTO Keskustelunavaus(keskustelualue) VALUES (2, 'Elokuvasuosituksia');");
        lista.add("INSERT INTO Viesti(keskustelunavaus, sisalto, nimimerkki, aika) VALUES (1, 'Java on totuus, tie ja elämä.', 'Javaaja', datetime('now'));");
        lista.add("INSERT INTO Viesti(keskustelunavaus, sisalto, nimimerkki, aika) VALUES (2, 'Python ei ole totuus, tie ja elämä.', 'Javaaja', datetime('now'));");
        lista.add("INSERT INTO Viesti(keskustelunavaus, sisalto, nimimerkki, aika) VALUES (3, 'Shawshank Redemption. Ihan paras.', 'Javaaja', datetime('now'));");
        
        return lista;
    }
}

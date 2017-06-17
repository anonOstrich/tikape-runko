package tikape.runko.database;

import java.sql.*;
import java.util.*;

public interface Dao<T, K> {

    T findOne(K key) throws SQLException;

    List<T> findAll() throws SQLException;
    
    //List<List<Object>> createView() throws SQLException; 
}

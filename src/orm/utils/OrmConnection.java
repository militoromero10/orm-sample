package orm.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class OrmConnection {

    private static Connection connection;

    public static Connection getConnection(){
        final String URL ="jdbc:mysql://localhost:3306/orm?serverTimezone=UTC";
        final String USER ="root";
        final String PASSWORD ="Prueba123";
        if(connection!=null){
            return connection;
        }
        try {
            connection = DriverManager.getConnection(URL,USER,PASSWORD);
        }catch (SQLException e){
            System.err.println("ERROR CONECNTANDO LA BASE DE DATOS");
        }
        return connection;
    }
}

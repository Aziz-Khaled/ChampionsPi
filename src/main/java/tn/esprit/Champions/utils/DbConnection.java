package tn.esprit.Champions.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {

    private static final String USER ="root" ;
    private static final String PASSWORD = "" ;

    private static final String URL = "jdbc:mysql://localhost:3306/newfintech" ;

    private Connection cnx  ;
    private static  DbConnection instance ;

    public Connection getCnx() {
        return cnx;
    }

    private DbConnection (){
        try {
            cnx = DriverManager.getConnection(URL , USER , PASSWORD) ;
            System.out.println("connection with success");
        }catch (SQLException e){
            System.err.println(e.getMessage());
        }
    }

    public static DbConnection getInstance () {
        if (instance == null) {
            instance = new DbConnection() ;
        }
        return instance ;
    }
}
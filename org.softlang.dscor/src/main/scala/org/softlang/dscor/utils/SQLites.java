package org.softlang.dscor.utils;

import java.sql.*;

/**
 * Created by Johannes on 19.10.2017.
 */
public class SQLites {

    static{
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Connection connect(String dbpath) {
        // SQLites connection string
        String url = "jdbc:sqlite:" + dbpath;
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return connection;
    }

    public static void execute(String dbpath, String sql) {
        Connection connection = connect(dbpath);
        try {
            Statement statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}

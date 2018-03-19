package org.softlang.dscor.process;

import java.sql.*;

/**
 * Created by Johannes on 13.10.2017. TODO: Delete!
 */
public class UploadData {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");

        Connection con = DriverManager.getConnection(
                "jdbc:mysql://mysqlhost.uni-koblenz.de:3306/apiclustering", "johanneshaertel", "api?0");

        // Drop.
        Statement stmt = con.createStatement();
        stmt.execute("DELETE FROM Nodes");

        // Insert.
        Statement stmt2 = con.createStatement();
        stmt2.execute("INSERT INTO Nodes ( Name ,Configuration, Size ) VALUES" +
                "( 'Ant', 1,10 ), ( 'Bnt', 1,40 )");


        // Query.
        query(con);
    }

    private static void query(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Nodes");
        while (rs.next()) System.out.println(rs.getString(1) + "  " + rs.getString(2) + "  " + rs.getString(3));
        con.close();
    }
}

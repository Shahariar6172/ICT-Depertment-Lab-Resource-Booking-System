package com.mbstu.labbooking.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Singleton JDBC connection helper. Change USER/PASS to match your MySQL. */
public class DBConnection {

    private static final String URL  = "jdbc:mysql://localhost:3306/lab_booking";
    private static final String USER = "root";
    private static final String PASS = "";   // <-- set your MySQL password here

    private static Connection connection;

    private DBConnection() {}

    public static synchronized Connection get() throws SQLException {
        if (connection == null || connection.isClosed()) {
            Connection candidate = DriverManager.getConnection(URL, USER, PASS);
            try {
                StudentSchema.ensure(candidate);
                connection = candidate;
            } catch (SQLException e) {
                try { candidate.close(); } catch (SQLException closeError) { e.addSuppressed(closeError); }
                connection = null;
                throw e;
            }
        }
        return connection;
    }
}

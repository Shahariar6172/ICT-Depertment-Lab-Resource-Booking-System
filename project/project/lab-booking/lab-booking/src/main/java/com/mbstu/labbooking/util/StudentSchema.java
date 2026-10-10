package com.mbstu.labbooking.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;

/** Repeatable upgrade for the existing lab database, before any dashboard queries run. */
public final class StudentSchema {
    private StudentSchema() { }

    public static void ensure(Connection con) throws SQLException {
        try {
            requireTable(con, "users");
            requireTable(con, "devices");
            requireTable(con, "bookings");
            // Old databases use ENUM values that omit categories offered by the admin UI.
            String categoryType = columnType(con, "devices", "category");
            // A manually upgraded database needs no DDL permission on subsequent launches.
            if (categoryType != null && categoryType.equalsIgnoreCase("VARCHAR")
                    && columnType(con, "bookings", "request_batch_id") != null
                    && tableExists(con, "lab_subjects") && tableExists(con, "material_variants")
                    && tableExists(con, "subject_materials") && tableExists(con, "request_batches")) {
                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT 1 FROM lab_subjects s JOIN subject_materials sm ON sm.subject_name=s.name " +
                             "JOIN material_variants v ON v.material_name=sm.material_name LIMIT 1")) {
                    if (rs.next()) return;
                }
            }
            if (categoryType != null && categoryType.toUpperCase(java.util.Locale.ROOT).startsWith("ENUM")) {
                execute(con, "ALTER TABLE devices MODIFY category VARCHAR(30) NOT NULL DEFAULT 'OTHER'");
            }
            String script;
            try (InputStream in = StudentSchema.class.getResourceAsStream("/sql/student_panel_catalog.sql")) {
                if (in == null) throw new SQLException("Missing packaged SQL resource. Run mvn clean javafx:run.");
                script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new SQLException("Cannot read the student catalog setup.", e);
            }
            // This bundled file contains simple CREATE/INSERT statements only.
            script = script.lines().filter(line -> !line.stripLeading().startsWith("--")).collect(java.util.stream.Collectors.joining("\n"));
            for (String statement : script.split(";")) {
                if (!statement.isBlank()) execute(con, statement.trim());
            }
            if (columnType(con, "bookings", "request_batch_id") == null) {
                try {
                    execute(con, "ALTER TABLE bookings ADD COLUMN request_batch_id INT NULL, " +
                            "ADD CONSTRAINT fk_booking_batch FOREIGN KEY (request_batch_id) REFERENCES request_batches(id)");
                } catch (SQLException e) {
                    // Another app instance may have finished this same upgrade meanwhile.
                    if (e.getErrorCode() != 1060 || columnType(con, "bookings", "request_batch_id") == null) throw e;
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Student database setup could not finish. " +
                    "Check that your MySQL account can CREATE and ALTER tables, or ask the database owner " +
                    "to run sql/upgrade_student_panel.sql. Details: " + e.getMessage(),
                    e.getSQLState(), e.getErrorCode(), e);
        }
    }

    private static void requireTable(Connection con, String table) throws SQLException {
        if (!tableExists(con, table)) throw new SQLException("Missing base table " + table +
                ". For a fresh installation, create the database using sql/database.sql first.");
    }

    private static boolean tableExists(Connection con, String table) throws SQLException {
        try (ResultSet rs = con.getMetaData().getTables(con.getCatalog(), null, table, new String[]{"TABLE"})) {
            while (rs.next()) if (table.equalsIgnoreCase(rs.getString("TABLE_NAME"))) return true;
            return false;
        }
    }

    private static String columnType(Connection con, String table, String column) throws SQLException {
        // JDBC metadata uses LIKE patterns; escape underscores to match the literal column name.
        DatabaseMetaData metadata = con.getMetaData();
        String escape = metadata.getSearchStringEscape();
        String pattern = column.replace("_", escape + "_");
        try (ResultSet rs = metadata.getColumns(con.getCatalog(), null, table, pattern)) {
            return rs.next() ? rs.getString("TYPE_NAME") : null;
        }
    }

    private static void execute(Connection con, String sql) throws SQLException {
        try (Statement statement = con.createStatement()) { statement.execute(sql); }
    }
}

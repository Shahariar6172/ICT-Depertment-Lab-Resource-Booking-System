package com.mbstu.labbooking.dao;

import com.mbstu.labbooking.model.Device;
import com.mbstu.labbooking.model.MaterialOption;
import com.mbstu.labbooking.model.DeviceGroup;
import com.mbstu.labbooking.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import com.mbstu.labbooking.util.MaterialCatalog;

public class DeviceDAO {

    public List<Device> findAll() throws SQLException {
        List<Device> list = new ArrayList<>();
        String sql = "SELECT * FROM devices ORDER BY name";
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Search by name/serial and optional category filter ("ALL" = no filter). */
    public List<Device> search(String keyword, String category) throws SQLException {
        List<Device> list = new ArrayList<>();
        String sql = "SELECT * FROM devices WHERE (name LIKE ? OR serial_no LIKE ?)" +
                     ("ALL".equals(category) ? "" : " AND category = ?") +
                     " ORDER BY name";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            if (!"ALL".equals(category)) ps.setString(3, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public boolean insert(String name, String category, String serialNo) throws SQLException {
        String sql = "INSERT INTO devices (name, category, serial_no) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setString(3, serialNo);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateStatus(int deviceId, String status) throws SQLException {
        String sql = "UPDATE devices SET status = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, deviceId);
            return ps.executeUpdate() == 1;
        }
    }


    // ---------- grouped (stock) views ----------

    /**
     * One row per distinct device name+category with unit counts.
     * keyword may be empty; category "ALL" means no category filter.
     */
    public List<DeviceGroup> findGroups(String keyword, String category) throws SQLException {
        String sql =
            "SELECT name, category, " +
            "       COUNT(*) AS total, " +
            "       SUM(status = 'AVAILABLE') AS available, " +
            "       SUM(status IN ('BOOKED','ISSUED')) AS issued, " +
            "       SUM(status = 'DAMAGED') AS damaged " +
            "FROM devices WHERE name LIKE ?" +
            ("ALL".equals(category) ? "" : " AND category = ?") +
            " GROUP BY name, category ORDER BY name";

        List<DeviceGroup> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            if (!"ALL".equals(category)) ps.setString(2, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DeviceGroup(
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getInt("total"),
                            rs.getInt("available"),
                            rs.getInt("issued"),
                            rs.getInt("damaged")));
                }
            }
        }
        return list;
    }

    /** All units under one device name -- used to show serials on demand. */
    public List<Device> findByName(String name) throws SQLException {
        List<Device> list = new ArrayList<>();
        String sql = "SELECT * FROM devices WHERE name = ? ORDER BY serial_no";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Picks one free unit of this device for booking.
     * Returns -1 if every unit is taken.
     */
    public int findFirstAvailableId(String name) throws SQLException {
        String sql = "SELECT id FROM devices WHERE name = ? AND status = 'AVAILABLE' " +
                     "ORDER BY serial_no LIMIT 1";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }

    public List<String> findSubjects() throws SQLException {
        List<String> subjects = new ArrayList<>();
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM lab_subjects ORDER BY name")) {
            while (rs.next()) subjects.add(rs.getString(1));
        }
        return subjects;
    }

    /** Stock comes from actual devices; configured but unstocked variants show zero availability. */
    public List<MaterialOption> findMaterialOptions(String subject) throws SQLException {
        List<MaterialOption> definitions = new ArrayList<>();
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM material_variants ORDER BY material_name,variant_name")) {
            while (rs.next()) definitions.add(new MaterialOption(rs.getString("material_name"),
                    rs.getString("variant_name"), rs.getString("device_name"), rs.getString("category"), 0));
        }
        Set<String> materials = null;
        if (subject != null) {
            materials = new HashSet<>();
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT material_name FROM subject_materials WHERE subject_name=?")) {
                ps.setString(1, subject);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) materials.add(rs.getString(1));
                }
            }
        }
        return MaterialCatalog.build(findGroups("", "ALL"), definitions, materials);
    }

    private Device map(ResultSet rs) throws SQLException {
        return new Device(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("serial_no"),
                rs.getString("status"));
    }
}

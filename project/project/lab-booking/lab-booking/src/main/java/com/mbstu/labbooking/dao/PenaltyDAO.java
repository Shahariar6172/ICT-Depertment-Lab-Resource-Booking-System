package com.mbstu.labbooking.dao;

import com.mbstu.labbooking.model.Penalty;
import com.mbstu.labbooking.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PenaltyDAO {

    public boolean insert(int bookingId, String type, double amount) throws SQLException {
        String sql = "INSERT INTO penalties (booking_id, type, amount) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            return ps.executeUpdate() == 1;
        }
    }

    public List<Penalty> findAll() throws SQLException {
        String sql = "SELECT p.*, u.name AS student_name, d.name AS device_name " +
                     "FROM penalties p " +
                     "JOIN bookings b ON b.id = p.booking_id " +
                     "JOIN users u ON u.id = b.user_id " +
                     "JOIN devices d ON d.id = b.device_id " +
                     "ORDER BY p.created_at DESC";
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    public List<Penalty> findByStudent(int userId) throws SQLException {
        String sql = "SELECT p.*, u.name AS student_name, d.name AS device_name " +
                     "FROM penalties p " +
                     "JOIN bookings b ON b.id = p.booking_id " +
                     "JOIN users u ON u.id = b.user_id " +
                     "JOIN devices d ON d.id = b.device_id " +
                     "WHERE b.user_id = ? ORDER BY p.created_at DESC";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return mapList(ps.executeQuery());
        }
    }

    public boolean markPaid(int penaltyId) throws SQLException {
        String sql = "UPDATE penalties SET paid = TRUE WHERE id = ?";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setInt(1, penaltyId);
            return ps.executeUpdate() == 1;
        }
    }

    private List<Penalty> mapList(ResultSet rs) throws SQLException {
        List<Penalty> list = new ArrayList<>();
        while (rs.next()) {
            Penalty p = new Penalty(
                    rs.getInt("id"),
                    rs.getInt("booking_id"),
                    rs.getString("type"),
                    rs.getDouble("amount"),
                    rs.getBoolean("paid"),
                    rs.getTimestamp("created_at").toLocalDateTime());
            p.setStudentName(rs.getString("student_name"));
            p.setDeviceName(rs.getString("device_name"));
            list.add(p);
        }
        return list;
    }
}

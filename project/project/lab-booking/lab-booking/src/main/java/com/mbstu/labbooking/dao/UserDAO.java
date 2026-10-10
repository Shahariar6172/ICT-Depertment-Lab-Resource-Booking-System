package com.mbstu.labbooking.dao;

import com.mbstu.labbooking.model.User;
import com.mbstu.labbooking.util.DBConnection;
import com.mbstu.labbooking.util.PasswordUtil;

import java.sql.*;

public class UserDAO {

    /** Returns the user if email+password match, otherwise null. */
    public User login(String email, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND password_hash = ?";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, PasswordUtil.hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public boolean register(String name, String studentId, String email, String password)
            throws SQLException {
        String sql = "INSERT INTO users (name, student_id, email, password_hash, role) " +
                     "VALUES (?, ?, ?, ?, 'STUDENT')";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, studentId);
            ps.setString(3, email);
            ps.setString(4, PasswordUtil.hash(password));
            return ps.executeUpdate() == 1;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("student_id"),
                rs.getString("email"),
                rs.getString("role"));
    }
}

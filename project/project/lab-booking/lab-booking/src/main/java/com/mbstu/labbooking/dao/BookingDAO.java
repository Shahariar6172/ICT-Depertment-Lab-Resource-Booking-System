package com.mbstu.labbooking.dao;

import com.mbstu.labbooking.model.Booking;
import com.mbstu.labbooking.model.RequestItem;
import java.util.UUID;
import java.util.Comparator;
import com.mbstu.labbooking.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    /** Student creates a PENDING request; device becomes BOOKED. */
    public boolean createRequest(int userId, int deviceId, LocalDate dueDate, String purpose)
            throws SQLException {
        Connection con = DBConnection.get();
        con.setAutoCommit(false);
        try {
            String sql = "INSERT INTO bookings (user_id, device_id, due_date, purpose) " +
                         "VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, deviceId);
                ps.setDate(3, Date.valueOf(dueDate));
                ps.setString(4, purpose);
                ps.executeUpdate();
            }
            // only book if still AVAILABLE (prevents double booking)
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE devices SET status='BOOKED' WHERE id=? AND status='AVAILABLE'")) {
                ps.setInt(1, deviceId);
                if (ps.executeUpdate() != 1) {
                    con.rollback();
                    return false;
                }
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    /** Reserve and submit the complete list in ONE transaction. No partial requests. */
    public String createBatchRequest(int userId, String subject, String purpose,
                                    LocalDate dueDate, List<RequestItem> items) throws SQLException {
        if (items == null || items.isEmpty() || subject == null || subject.isBlank()
                || subject.length() > 100 || purpose == null || purpose.isBlank()
                || purpose.length() > 255 || dueDate == null || !dueDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Choose a subject/work, purpose, future return date and items.");
        }
        List<RequestItem> ordered = new ArrayList<>(items);
        ordered.sort(Comparator.comparing((RequestItem i) -> i.getOption().getDeviceName())
                .thenComparing(i -> i.getOption().getCategory()));
        Connection con = DBConnection.get();
        boolean autoCommit = con.getAutoCommit();
        con.setAutoCommit(false);
        String code = UUID.randomUUID().toString();
        try {
            int batchId;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO request_batches (request_code,user_id,subject_name,purpose,due_date) " +
                    "VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, code);
                ps.setInt(2, userId);
                ps.setString(3, subject);
                ps.setString(4, purpose);
                ps.setDate(5, Date.valueOf(dueDate));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Could not create the request.");
                    batchId = rs.getInt(1);
                }
            }
            String displayPurpose = "[" + code.substring(0, 8) + "] " + subject + " - " + purpose;
            if (displayPurpose.length() > 255) displayPurpose = displayPurpose.substring(0, 255);
            for (RequestItem item : ordered) {
                List<Integer> ids = new ArrayList<>();
                // Lock physical units; another student cannot reserve them during this transaction.
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT id FROM devices WHERE name=? AND category=? AND status='AVAILABLE' " +
                        "ORDER BY id LIMIT ? FOR UPDATE")) {
                    ps.setString(1, item.getOption().getDeviceName());
                    ps.setString(2, item.getCategory());
                    ps.setInt(3, item.getQuantity());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) ids.add(rs.getInt(1));
                    }
                }
                if (ids.size() != item.getQuantity()) {
                    throw new SQLException("Not enough stock for " + item.getMaterialName() + " / " +
                            item.getVariantName() + ". Requested " + item.getQuantity() +
                            ", available " + ids.size() + ". No items were submitted.");
                }
                for (int id : ids) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE devices SET status='BOOKED' WHERE id=? AND status='AVAILABLE'")) {
                        ps.setInt(1, id);
                        if (ps.executeUpdate() != 1) throw new SQLException("Stock changed. Retry the request.");
                    }
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO bookings (user_id,device_id,due_date,purpose,request_batch_id) " +
                            "VALUES (?,?,?,?,?)")) {
                        ps.setInt(1, userId);
                        ps.setInt(2, id);
                        ps.setDate(3, Date.valueOf(dueDate));
                        ps.setString(4, displayPurpose);
                        ps.setInt(5, batchId);
                        ps.executeUpdate();
                    }
                }
            }
            con.commit();
            return code;
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(autoCommit);
        }
    }

    public List<Booking> findByStudent(int userId) throws SQLException {
        String sql = baseSelect() + " WHERE b.user_id = ? ORDER BY b.request_date DESC";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return mapList(ps.executeQuery());
        }
    }

    public List<Booking> findByStatus(String status) throws SQLException {
        String sql = baseSelect() + " WHERE b.status = ? ORDER BY b.request_date";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setString(1, status);
            return mapList(ps.executeQuery());
        }
    }

    public boolean updateStatus(int bookingId, String status) throws SQLException {
        String sql = "UPDATE bookings SET status = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, bookingId);
            return ps.executeUpdate() == 1;
        }
    }

    /** APPROVED -> ISSUED, sets issue_date, device -> ISSUED. */
    public boolean issue(Booking b) throws SQLException {
        Connection con = DBConnection.get();
        con.setAutoCommit(false);
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE bookings SET status='ISSUED', issue_date=NOW() WHERE id=?")) {
                ps.setInt(1, b.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE devices SET status='ISSUED' WHERE id=?")) {
                ps.setInt(1, b.getDeviceId());
                ps.executeUpdate();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    /** ISSUED -> RETURNED, sets return_date; device AVAILABLE (or DAMAGED). */
    public boolean processReturn(Booking b, boolean damaged) throws SQLException {
        Connection con = DBConnection.get();
        con.setAutoCommit(false);
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE bookings SET status='RETURNED', return_date=NOW() WHERE id=?")) {
                ps.setInt(1, b.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE devices SET status=? WHERE id=?")) {
                ps.setString(1, damaged ? "DAMAGED" : "AVAILABLE");
                ps.setInt(2, b.getDeviceId());
                ps.executeUpdate();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    /** On REJECT, free the device again. */
    public boolean reject(Booking b) throws SQLException {
        Connection con = DBConnection.get();
        con.setAutoCommit(false);
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE bookings SET status='REJECTED' WHERE id=?")) {
                ps.setInt(1, b.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE devices SET status='AVAILABLE' WHERE id=? AND status='BOOKED'")) {
                ps.setInt(1, b.getDeviceId());
                ps.executeUpdate();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    // ---------- helpers ----------

    private String baseSelect() {
        return "SELECT b.*, u.name AS student_name, d.name AS device_name, rb.request_code " +
               "FROM bookings b " +
               "JOIN users u ON u.id = b.user_id " +
               "JOIN devices d ON d.id = b.device_id " +
               "LEFT JOIN request_batches rb ON rb.id = b.request_batch_id";
    }

    private List<Booking> mapList(ResultSet rs) throws SQLException {
        List<Booking> list = new ArrayList<>();
        while (rs.next()) {
            Booking b = new Booking(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getInt("device_id"),
                    rs.getTimestamp("request_date").toLocalDateTime(),
                    rs.getDate("due_date").toLocalDate(),
                    rs.getTimestamp("issue_date") == null ? null
                            : rs.getTimestamp("issue_date").toLocalDateTime(),
                    rs.getTimestamp("return_date") == null ? null
                            : rs.getTimestamp("return_date").toLocalDateTime(),
                    rs.getString("purpose"),
                    rs.getString("status"));
            b.setRequestCode(rs.getString("request_code"));
            b.setStudentName(rs.getString("student_name"));
            b.setDeviceName(rs.getString("device_name"));
            list.add(b);
        }
        return list;
    }
}

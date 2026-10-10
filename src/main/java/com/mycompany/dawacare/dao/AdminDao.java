package com.mycompany.dawacare.dao;

import com.mycompany.dawacare.model.*;
import com.mycompany.dawacare.util.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminDao {

    public List<PendingDoctor> findPendingDoctors() throws SQLException {
        String sql = "SELECT u.user_id, u.full_name, u.username, u.email, u.phone, u.created_at, "
                   + "d.license_number, d.specialization, d.hospital_name "
                   + "FROM users u JOIN doctors d ON d.user_id = u.user_id "
                   + "WHERE u.role = 'DOCTOR' AND u.status = 'PENDING' "
                   + "ORDER BY u.created_at";
        List<PendingDoctor> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PendingDoctor(
                    rs.getInt("user_id"),
                    rs.getString("full_name"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("license_number"),
                    rs.getString("specialization"),
                    rs.getString("hospital_name"),
                    rs.getObject("created_at", LocalDateTime.class)));
            }
        }
        return list;
    }

    public List<UserSummary> findAllUsers() throws SQLException {
        String sql = "SELECT user_id, username, full_name, role, status, created_at, last_login_at "
                   + "FROM users ORDER BY role, full_name";
        List<UserSummary> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new UserSummary(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    Role.valueOf(rs.getString("role")),
                    UserStatus.valueOf(rs.getString("status")),
                    rs.getObject("created_at", LocalDateTime.class),
                    rs.getObject("last_login_at", LocalDateTime.class)));
            }
        }
        return list;
    }

    /** Returns false if the doctor was not pending (e.g. already handled). */
    public boolean approveDoctor(int doctorUserId, int adminUserId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE users SET status = 'ACTIVE' "
                      + "WHERE user_id = ? AND role = 'DOCTOR' AND status = 'PENDING'")) {
                    ps.setInt(1, doctorUserId);
                    rows = ps.executeUpdate();
                }
                if (rows == 0) {
                    c.rollback();
                    return false;
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE doctors SET verified_by = ?, verified_at = NOW() WHERE user_id = ?")) {
                    ps.setInt(1, adminUserId);
                    ps.setInt(2, doctorUserId);
                    ps.executeUpdate();
                }
                audit(c, adminUserId, "APPROVE_DOCTOR", doctorUserId);
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public boolean rejectDoctor(int doctorUserId, int adminUserId) throws SQLException {
        return updateWithAudit(
            "UPDATE users SET status = 'INACTIVE' "
          + "WHERE user_id = ? AND role = 'DOCTOR' AND status = 'PENDING'",
            doctorUserId, adminUserId, "REJECT_DOCTOR");
    }

    public boolean activateUser(int userId, int adminUserId) throws SQLException {
        // Never activates admins, pending doctors, or doctors who were never verified
        return updateWithAudit(
            "UPDATE users u LEFT JOIN doctors d ON d.user_id = u.user_id "
          + "SET u.status = 'ACTIVE' "
          + "WHERE u.user_id = ? AND u.role <> 'ADMIN' AND u.status = 'INACTIVE' "
          + "AND (u.role <> 'DOCTOR' OR d.verified_at IS NOT NULL)",
            userId, adminUserId, "ACTIVATE_USER");
    }

    public boolean deactivateUser(int userId, int adminUserId) throws SQLException {
        return updateWithAudit(
            "UPDATE users SET status = 'INACTIVE' "
          + "WHERE user_id = ? AND role <> 'ADMIN' AND status = 'ACTIVE'",
            userId, adminUserId, "DEACTIVATE_USER");
    }

    private boolean updateWithAudit(String sql, int targetUserId, int adminUserId, String action)
            throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, targetUserId);
                    rows = ps.executeUpdate();
                }
                if (rows == 0) {
                    c.rollback();
                    return false;
                }
                audit(c, adminUserId, action, targetUserId);
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void audit(Connection c, int adminUserId, String action, int targetUserId)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO audit_log (user_id, action, entity, entity_id) VALUES (?, ?, 'users', ?)")) {
            ps.setInt(1, adminUserId);
            ps.setString(2, action);
            ps.setInt(3, targetUserId);
            ps.executeUpdate();
        }
    }
    
    public List<AuditEntry> findAuditLog(int limit) throws SQLException {
        String sql = "SELECT a.log_id, a.created_at, u.username, u.role, a.action, a.entity, a.entity_id, a.details, "
                   + "CASE a.entity "
                   + "  WHEN 'users' THEN tu.username "
                   + "  WHEN 'medications' THEN CONCAT(m.medication_name, ' ', m.strength) "
                   + "  WHEN 'prescriptions' THEN pu.full_name "
                   + "  WHEN 'patients' THEN ppu.full_name "
                   + "END AS target_name "
                   + "FROM audit_log a "
                   + "LEFT JOIN users u ON u.user_id = a.user_id "
                   + "LEFT JOIN users tu ON a.entity = 'users' AND tu.user_id = a.entity_id "
                   + "LEFT JOIN medications m ON a.entity = 'medications' AND m.medication_id = a.entity_id "
                   + "LEFT JOIN prescriptions pr ON a.entity = 'prescriptions' AND pr.prescription_id = a.entity_id "
                   + "LEFT JOIN patients p ON p.patient_id = pr.patient_id "
                   + "LEFT JOIN users pu ON pu.user_id = p.user_id "
                   + "LEFT JOIN patients p2 ON a.entity = 'patients' AND p2.patient_id = a.entity_id "
                   + "LEFT JOIN users ppu ON ppu.user_id = p2.user_id "
                   + "ORDER BY a.created_at DESC, a.log_id DESC "
                   + "LIMIT ?";
        List<AuditEntry> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AuditEntry(
                        rs.getLong("log_id"),
                        rs.getObject("created_at", LocalDateTime.class),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("action"),
                        rs.getString("entity"),
                        rs.getObject("entity_id", Integer.class),
                        rs.getString("target_name"),
                        rs.getString("details")));
                }
            }
        }
        return list;
    }
}
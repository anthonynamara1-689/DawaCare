package com.mycompany.dawacare.dao;

import com.mycompany.dawacare.model.ReminderCandidate;
import com.mycompany.dawacare.util.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReminderDao extends BaseDao {

    /** The patient's PENDING doses due between from and to. */
    public List<ReminderCandidate> findDueDoses(int patientId, LocalDateTime from, LocalDateTime to)
            throws SQLException {
        return query(
            "SELECT dose_id, scheduled_at, CONCAT(medication_name, ' ', strength) AS medication, "
          + "dosage_amount, dosage_unit, NULL AS patient_name "
          + "FROM v_dose_details "
          + "WHERE patient_id = ? AND status = 'PENDING' AND scheduled_at BETWEEN ? AND ? "
          + "ORDER BY scheduled_at",
            patientId, from, to);
    }

    /** MISSED doses since a time, for every patient actively linked to this caregiver. */
    public List<ReminderCandidate> findRecentMissedForCaregiver(int caregiverId, LocalDateTime since)
            throws SQLException {
        return query(
            "SELECT v.dose_id, v.scheduled_at, CONCAT(v.medication_name, ' ', v.strength) AS medication, "
          + "v.dosage_amount, v.dosage_unit, u.full_name AS patient_name "
          + "FROM v_dose_details v "
          + "JOIN caregiver_patients cp ON cp.patient_id = v.patient_id "
          + "JOIN patients p ON p.patient_id = v.patient_id "
          + "JOIN users u ON u.user_id = p.user_id "
          + "WHERE cp.caregiver_id = ? AND cp.status = 'ACTIVE' "
          + "AND v.status = 'MISSED' AND v.scheduled_at >= ? "
          + "ORDER BY v.scheduled_at",
            caregiverId, since);
    }

    /** Returns 1 if the notification is new, 0 if this user was already notified about this dose. */
    public int insertNotification(int userId, String type, String message, int doseId) throws SQLException {
        return update("INSERT IGNORE INTO notifications (user_id, type, message, dose_id) VALUES (?, ?, ?, ?)",
                      userId, type, message, doseId);
    }

    private List<ReminderCandidate> query(String sql, Object... params) throws SQLException {
        List<ReminderCandidate> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, params);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new ReminderCandidate(
                    rs.getInt("dose_id"),
                    rs.getString("patient_name"),
                    rs.getString("medication"),
                    rs.getBigDecimal("dosage_amount"),
                    rs.getString("dosage_unit"),
                    rs.getObject("scheduled_at", LocalDateTime.class)));
            }
        }
        return list;
    }
}
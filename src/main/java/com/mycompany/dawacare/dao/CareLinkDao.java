package com.mycompany.dawacare.dao;

import com.mycompany.dawacare.model.CareLink;
import com.mycompany.dawacare.model.DoctorOption;
import com.mycompany.dawacare.util.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CareLinkDao {

    // ===================== Doctor <-> Patient =====================

    /** Approved, active doctors not already on this patient's care team. */
    public List<DoctorOption> findAvailableDoctors(int patientId) throws SQLException {
        String sql = "SELECT d.doctor_id, u.full_name, d.specialization, d.hospital_name "
                   + "FROM doctors d JOIN users u ON u.user_id = d.user_id "
                   + "WHERE u.status = 'ACTIVE' AND d.verified_at IS NOT NULL "
                   + "AND NOT EXISTS (SELECT 1 FROM doctor_patients dp "
                   + "  WHERE dp.doctor_id = d.doctor_id AND dp.patient_id = ? AND dp.status = 'ACTIVE') "
                   + "ORDER BY u.full_name";
        List<DoctorOption> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, patientId);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DoctorOption(
                    rs.getInt("doctor_id"),
                    rs.getString("full_name"),
                    rs.getString("specialization"),
                    rs.getString("hospital_name")));
            }
        }
        return list;
    }

    public boolean isAvailableDoctor(int doctorId) throws SQLException {
        return queryValue("SELECT 1 FROM doctors d JOIN users u ON u.user_id = d.user_id "
                        + "WHERE d.doctor_id = ? AND u.status = 'ACTIVE' AND d.verified_at IS NOT NULL",
                          Integer.class, doctorId) != null;
    }

    public List<CareLink> findDoctorsOfPatient(int patientId) throws SQLException {
        return queryLinks(
            "SELECT dp.doctor_patient_id AS link_id, u.full_name, u.phone, "
          + "CONCAT_WS(' - ', d.specialization, d.hospital_name) AS info, dp.status, dp.created_at "
          + "FROM doctor_patients dp "
          + "JOIN doctors d ON d.doctor_id = dp.doctor_id "
          + "JOIN users u ON u.user_id = d.user_id "
          + "WHERE dp.patient_id = ? AND dp.status = 'ACTIVE' "
          + "ORDER BY u.full_name", patientId);
    }

    public String doctorLinkStatus(int doctorId, int patientId) throws SQLException {
        return queryValue("SELECT status FROM doctor_patients WHERE doctor_id = ? AND patient_id = ?",
                          String.class, doctorId, patientId);
    }

    public int insertDoctorLink(int doctorId, int patientId) throws SQLException {
        return update("INSERT INTO doctor_patients (doctor_id, patient_id, status) VALUES (?, ?, 'ACTIVE')",
                      doctorId, patientId);
    }

    public int reactivateDoctorLink(int doctorId, int patientId) throws SQLException {
        return update("UPDATE doctor_patients SET status = 'ACTIVE', created_at = NOW() "
                    + "WHERE doctor_id = ? AND patient_id = ? AND status <> 'ACTIVE'",
                      doctorId, patientId);
    }

    public int endDoctorLink(int linkId, int patientId) throws SQLException {
        return update("UPDATE doctor_patients SET status = 'ENDED' "
                    + "WHERE doctor_patient_id = ? AND patient_id = ? AND status = 'ACTIVE'",
                      linkId, patientId);
    }

    // ===================== Caregiver <-> Patient =====================

    public Integer findActivePatientIdByUsername(String username) throws SQLException {
        return queryValue("SELECT p.patient_id FROM patients p JOIN users u ON u.user_id = p.user_id "
                        + "WHERE u.username = ? AND u.role = 'PATIENT' AND u.status = 'ACTIVE'",
                          Integer.class, username);
    }

    public List<CareLink> findCaregiversOfPatient(int patientId) throws SQLException {
        return queryLinks(
            "SELECT cp.caregiver_patient_id AS link_id, u.full_name, u.phone, "
          + "cp.relationship_to_patient AS info, cp.status, cp.created_at "
          + "FROM caregiver_patients cp "
          + "JOIN caregivers c ON c.caregiver_id = cp.caregiver_id "
          + "JOIN users u ON u.user_id = c.user_id "
          + "WHERE cp.patient_id = ? AND cp.status IN ('PENDING', 'ACTIVE') "
          + "ORDER BY FIELD(cp.status, 'PENDING', 'ACTIVE'), u.full_name", patientId);
    }

    public List<CareLink> findPatientsOfCaregiver(int caregiverId) throws SQLException {
        return queryLinks(
            "SELECT cp.caregiver_patient_id AS link_id, u.full_name, u.phone, "
          + "cp.relationship_to_patient AS info, cp.status, cp.created_at "
          + "FROM caregiver_patients cp "
          + "JOIN patients p ON p.patient_id = cp.patient_id "
          + "JOIN users u ON u.user_id = p.user_id "
          + "WHERE cp.caregiver_id = ? AND cp.status IN ('PENDING', 'ACTIVE') "
          + "ORDER BY FIELD(cp.status, 'ACTIVE', 'PENDING'), u.full_name", caregiverId);
    }

    public String caregiverLinkStatus(int caregiverId, int patientId) throws SQLException {
        return queryValue("SELECT status FROM caregiver_patients WHERE caregiver_id = ? AND patient_id = ?",
                          String.class, caregiverId, patientId);
    }

    public int insertCaregiverRequest(int caregiverId, int patientId, String relationship) throws SQLException {
        return update("INSERT INTO caregiver_patients "
                    + "(caregiver_id, patient_id, relationship_to_patient, status, requested_by) "
                    + "VALUES (?, ?, ?, 'PENDING', 'CAREGIVER')",
                      caregiverId, patientId, relationship);
    }

    /** Re-opens a previously revoked link as a new pending request. */
    public int renewCaregiverRequest(int caregiverId, int patientId, String relationship) throws SQLException {
        return update("UPDATE caregiver_patients SET status = 'PENDING', requested_by = 'CAREGIVER', "
                    + "relationship_to_patient = ?, approved_at = NULL, created_at = NOW() "
                    + "WHERE caregiver_id = ? AND patient_id = ? AND status = 'REVOKED'",
                      relationship, caregiverId, patientId);
    }

    public int approveCaregiver(int linkId, int patientId) throws SQLException {
        return update("UPDATE caregiver_patients SET status = 'ACTIVE', approved_at = NOW() "
                    + "WHERE caregiver_patient_id = ? AND patient_id = ? AND status = 'PENDING'",
                      linkId, patientId);
    }

    public int revokeByPatient(int linkId, int patientId) throws SQLException {
        return update("UPDATE caregiver_patients SET status = 'REVOKED' "
                    + "WHERE caregiver_patient_id = ? AND patient_id = ? AND status IN ('PENDING', 'ACTIVE')",
                      linkId, patientId);
    }

    public int revokeByCaregiver(int linkId, int caregiverId) throws SQLException {
        return update("UPDATE caregiver_patients SET status = 'REVOKED' "
                    + "WHERE caregiver_patient_id = ? AND caregiver_id = ? AND status IN ('PENDING', 'ACTIVE')",
                      linkId, caregiverId);
    }

    // ===================== Helpers =====================

    private List<CareLink> queryLinks(String sql, int id) throws SQLException {
        List<CareLink> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, id);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new CareLink(
                    rs.getInt("link_id"),
                    rs.getString("full_name"),
                    rs.getString("phone"),
                    rs.getString("info"),
                    rs.getString("status"),
                    rs.getObject("created_at", LocalDateTime.class)));
            }
        }
        return list;
    }

    private <T> T queryValue(String sql, Class<T> type, Object... params) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, params);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getObject(1, type) : null;
        }
    }

    private int update(String sql, Object... params) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, params)) {
            return ps.executeUpdate();
        }
    }

    private PreparedStatement prepare(Connection c, String sql, Object... params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps;
    }
}
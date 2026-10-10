package com.mycompany.dawacare.dao;

import com.mycompany.dawacare.model.*;
import com.mycompany.dawacare.util.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DoseDao extends BaseDao {

    public List<DoseItem> findDosesBetween(int patientId, LocalDateTime from, LocalDateTime to)
            throws SQLException {
        String sql = "SELECT v.dose_id, v.scheduled_at, CONCAT(v.medication_name, ' ', v.strength) AS medication, "
                   + "v.dosage_amount, v.dosage_unit, v.instructions, v.status, v.taken_at, "
                   + "r.full_name AS recorded_by_name "
                   + "FROM v_dose_details v "
                   + "LEFT JOIN users r ON r.user_id = v.recorded_by "
                   + "WHERE v.patient_id = ? AND v.scheduled_at >= ? AND v.scheduled_at < ? "
                   + "AND v.status <> 'CANCELLED' "
                   + "ORDER BY v.scheduled_at, medication";
        List<DoseItem> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, patientId, from, to);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DoseItem(
                    rs.getInt("dose_id"),
                    rs.getObject("scheduled_at", LocalDateTime.class),
                    rs.getString("medication"),
                    rs.getBigDecimal("dosage_amount"),
                    rs.getString("dosage_unit"),
                    rs.getString("instructions"),
                    rs.getString("status"),
                    rs.getObject("taken_at", LocalDateTime.class),
                    rs.getString("recorded_by_name")));
            }
        }
        return list;
    }

    /** Overall adherence for completed doses in the period. */
    public AdherenceStats stats(int patientId, LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT COALESCE(SUM(status = 'TAKEN'), 0) AS taken, "
                   + "COALESCE(SUM(status = 'MISSED'), 0) AS missed, "
                   + "COALESCE(SUM(status = 'SKIPPED'), 0) AS skipped "
                   + "FROM v_dose_details "
                   + "WHERE patient_id = ? AND scheduled_at >= ? AND scheduled_at <= ? "
                   + "AND status IN ('TAKEN', 'MISSED', 'SKIPPED')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, patientId, from, to);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new AdherenceStats("All medications",
                    rs.getInt("taken"), rs.getInt("missed"), rs.getInt("skipped"));
        }
    }

    /** Adherence per medication for completed doses in the period. */
    public List<AdherenceStats> statsByMedication(int patientId, LocalDateTime from, LocalDateTime to)
            throws SQLException {
        String sql = "SELECT CONCAT(medication_name, ' ', strength) AS medication, "
                   + "SUM(status = 'TAKEN') AS taken, "
                   + "SUM(status = 'MISSED') AS missed, "
                   + "SUM(status = 'SKIPPED') AS skipped "
                   + "FROM v_dose_details "
                   + "WHERE patient_id = ? AND scheduled_at >= ? AND scheduled_at <= ? "
                   + "AND status IN ('TAKEN', 'MISSED', 'SKIPPED') "
                   + "GROUP BY medication_name, strength "
                   + "ORDER BY medication";
        List<AdherenceStats> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, patientId, from, to);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new AdherenceStats(rs.getString("medication"),
                        rs.getInt("taken"), rs.getInt("missed"), rs.getInt("skipped")));
            }
        }
        return list;
    }

    /**
     * Records TAKEN or SKIPPED, only if the dose belongs to this patient,
     * is still PENDING or MISSED, and its time is inside the allowed window.
     */
    public int markDose(int doseId, int patientId, String newStatus, String skipReason,
                        int recordedByUserId, LocalDateTime earliest, LocalDateTime latest)
            throws SQLException {
        return update(
            "UPDATE dose_records d "
          + "JOIN prescription_items pi ON pi.prescription_item_id = d.prescription_item_id "
          + "JOIN prescriptions p ON p.prescription_id = pi.prescription_id "
          + "SET d.status = ?, d.taken_at = IF(? = 'TAKEN', NOW(), NULL), d.skip_reason = ?, "
          + "    d.recorded_by = ?, d.recorded_at = NOW() "
          + "WHERE d.dose_id = ? AND p.patient_id = ? "
          + "AND d.status IN ('PENDING', 'MISSED') "
          + "AND d.scheduled_at >= ? AND d.scheduled_at <= ?",
            newStatus, newStatus, skipReason, recordedByUserId, doseId, patientId, earliest, latest);
    }

    /** Used by the background job. Returns how many doses were marked. */
    public int markOverdueAsMissed(int graceMinutes) throws SQLException {
        return update("UPDATE dose_records SET status = 'MISSED' "
                    + "WHERE status = 'PENDING' AND scheduled_at < NOW() - INTERVAL ? MINUTE",
                      graceMinutes);
    }

    public List<PatientOption> findActivePatientsOfCaregiver(int caregiverId) throws SQLException {
        String sql = "SELECT p.patient_id, u.full_name "
                   + "FROM caregiver_patients cp "
                   + "JOIN patients p ON p.patient_id = cp.patient_id "
                   + "JOIN users u ON u.user_id = p.user_id "
                   + "WHERE cp.caregiver_id = ? AND cp.status = 'ACTIVE' AND u.status = 'ACTIVE' "
                   + "ORDER BY u.full_name";
        List<PatientOption> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, caregiverId);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PatientOption(rs.getInt("patient_id"), rs.getString("full_name")));
            }
        }
        return list;
    }

    public boolean isActiveCaregiverOf(int caregiverId, int patientId) throws SQLException {
        return queryValue("SELECT 1 FROM caregiver_patients "
                        + "WHERE caregiver_id = ? AND patient_id = ? AND status = 'ACTIVE'",
                          Integer.class, caregiverId, patientId) != null;
    }
    
    public boolean isActiveDoctorOf(int doctorId, int patientId) throws SQLException {
        return queryValue("SELECT 1 FROM doctor_patients "
                        + "WHERE doctor_id = ? AND patient_id = ? AND status = 'ACTIVE'",
                          Integer.class, doctorId, patientId) != null;
    }
    
        /** Returns {items completed, prescriptions completed}. */
    public int[] completeFinishedPrescriptions() throws SQLException {
        int items = update(
            "UPDATE prescription_items pi SET pi.status = 'COMPLETED' "
          + "WHERE pi.status = 'ACTIVE' AND pi.end_date < CURDATE() "
          + "AND NOT EXISTS (SELECT 1 FROM dose_records d "
          + "                WHERE d.prescription_item_id = pi.prescription_item_id AND d.status = 'PENDING')");
        int prescriptions = update(
            "UPDATE prescriptions pr SET pr.status = 'COMPLETED' "
          + "WHERE pr.status = 'ACTIVE' "
          + "AND NOT EXISTS (SELECT 1 FROM prescription_items pi "
          + "                WHERE pi.prescription_id = pr.prescription_id AND pi.status = 'ACTIVE')");
        return new int[]{items, prescriptions};
    }
    
    public PatientInfo findPatientInfo(int patientId) throws SQLException {
        String sql = "SELECT u.full_name, p.gender, p.date_of_birth, u.phone "
                   + "FROM patients p JOIN users u ON u.user_id = p.user_id WHERE p.patient_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, patientId);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            return new PatientInfo(
                rs.getString("full_name"),
                rs.getString("gender"),
                rs.getObject("date_of_birth", java.time.LocalDate.class),
                rs.getString("phone"));
        }
    }

    public List<MedicationLine> findCurrentMedications(int patientId) throws SQLException {
        String sql = "SELECT CONCAT(m.medication_name, ' ', m.strength) AS medication, "
                   + "pi.dosage_amount, pi.dosage_unit, pi.interval_days, pi.start_date, pi.end_date, "
                   + "pi.instructions, du.full_name AS doctor_name, "
                   + "GROUP_CONCAT(TIME_FORMAT(dt.dose_time, '%H:%i') ORDER BY dt.dose_time SEPARATOR ', ') AS times "
                   + "FROM prescription_items pi "
                   + "JOIN prescriptions pr ON pr.prescription_id = pi.prescription_id "
                   + "JOIN medications m ON m.medication_id = pi.medication_id "
                   + "JOIN doctors d ON d.doctor_id = pr.doctor_id "
                   + "JOIN users du ON du.user_id = d.user_id "
                   + "LEFT JOIN dose_times dt ON dt.prescription_item_id = pi.prescription_item_id "
                   + "WHERE pr.patient_id = ? AND pr.status = 'ACTIVE' AND pi.status = 'ACTIVE' "
                   + "GROUP BY pi.prescription_item_id, m.medication_name, m.strength, pi.dosage_amount, "
                   + "pi.dosage_unit, pi.interval_days, pi.start_date, pi.end_date, pi.instructions, du.full_name "
                   + "ORDER BY m.medication_name";
        List<MedicationLine> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, patientId);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new MedicationLine(
                    rs.getString("medication"),
                    rs.getBigDecimal("dosage_amount"),
                    rs.getString("dosage_unit"),
                    rs.getString("times"),
                    rs.getInt("interval_days"),
                    rs.getObject("start_date", java.time.LocalDate.class),
                    rs.getObject("end_date", java.time.LocalDate.class),
                    rs.getString("instructions"),
                    rs.getString("doctor_name")));
            }
        }
        return list;
    }

    /** Records that someone exported this patient's report. */
    public void logReport(int actorUserId, int patientId, int days) throws SQLException {
        update("INSERT INTO audit_log (user_id, action, entity, entity_id, details) "
             + "VALUES (?, 'GENERATE_REPORT', 'patients', ?, ?)",
               actorUserId, patientId, "Adherence report PDF, last " + days + " days");
    }
}
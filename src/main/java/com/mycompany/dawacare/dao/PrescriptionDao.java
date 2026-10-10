package com.mycompany.dawacare.dao;

import com.mycompany.dawacare.model.*;
import com.mycompany.dawacare.util.DBConnection;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionDao extends BaseDao {

    public List<DoctorPatient> findPatientsOfDoctor(int doctorId) throws SQLException {
        String sql = "SELECT p.patient_id, u.full_name, p.gender, p.date_of_birth, u.phone, "
                   + "dp.created_at AS linked_since, "
                   + "(SELECT COUNT(*) FROM prescriptions pr WHERE pr.patient_id = p.patient_id "
                   + "  AND pr.doctor_id = dp.doctor_id AND pr.status = 'ACTIVE') AS active_rx "
                   + "FROM doctor_patients dp "
                   + "JOIN patients p ON p.patient_id = dp.patient_id "
                   + "JOIN users u ON u.user_id = p.user_id "
                   + "WHERE dp.doctor_id = ? AND dp.status = 'ACTIVE' AND u.status = 'ACTIVE' "
                   + "ORDER BY u.full_name";
        List<DoctorPatient> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, doctorId);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DoctorPatient(
                    rs.getInt("patient_id"),
                    rs.getString("full_name"),
                    rs.getString("gender"),
                    rs.getObject("date_of_birth", LocalDate.class),
                    rs.getString("phone"),
                    rs.getObject("linked_since", LocalDateTime.class),
                    rs.getInt("active_rx")));
            }
        }
        return list;
    }

    public List<MedicationOption> findMedications() throws SQLException {
        String sql = "SELECT medication_id, medication_name, generic_name, strength, dosage_form "
                   + "FROM medications ORDER BY medication_name, strength";
        List<MedicationOption> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new MedicationOption(
                    rs.getInt("medication_id"),
                    rs.getString("medication_name"),
                    rs.getString("generic_name"),
                    rs.getString("strength"),
                    rs.getString("dosage_form")));
            }
        }
        return list;
    }

    public boolean isActivePatientOfDoctor(int doctorId, int patientId) throws SQLException {
        return queryValue("SELECT 1 FROM doctor_patients dp "
                        + "JOIN patients p ON p.patient_id = dp.patient_id "
                        + "JOIN users u ON u.user_id = p.user_id "
                        + "WHERE dp.doctor_id = ? AND dp.patient_id = ? "
                        + "AND dp.status = 'ACTIVE' AND u.status = 'ACTIVE'",
                          Integer.class, doctorId, patientId) != null;
    }

    /**
     * Saves the prescription, its items, dose times and all dose records in ONE transaction.
     * dosesPerItem.get(i) holds the scheduled dose times for items.get(i).
     */
    public int createPrescription(int patientId, int doctorId, String diagnosis, String notes,
                                  List<PrescriptionItemRequest> items,
                                  List<List<LocalDateTime>> dosesPerItem,
                                  int actorUserId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int prescriptionId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO prescriptions (patient_id, doctor_id, prescription_date, diagnosis, notes) "
                      + "VALUES (?, ?, CURDATE(), ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, patientId);
                    ps.setInt(2, doctorId);
                    ps.setString(3, diagnosis);
                    ps.setString(4, notes);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        prescriptionId = keys.getInt(1);
                    }
                }

                for (int i = 0; i < items.size(); i++) {
                    PrescriptionItemRequest it = items.get(i);
                    int itemId = insertItem(c, prescriptionId, it);
                    insertDoseTimes(c, itemId, it.doseTimes());
                    insertDoseRecords(c, itemId, dosesPerItem.get(i));
                }

                audit(c, actorUserId, "CREATE_PRESCRIPTION", "prescriptions", prescriptionId);
                c.commit();
                return prescriptionId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private int insertItem(Connection c, int prescriptionId, PrescriptionItemRequest it) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO prescription_items (prescription_id, medication_id, dosage_amount, dosage_unit, "
              + "start_date, end_date, interval_days, instructions) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, prescriptionId);
            ps.setInt(2, it.medication().medicationId());
            ps.setBigDecimal(3, it.dosageAmount());
            ps.setString(4, it.dosageUnit());
            ps.setObject(5, it.startDate());
            ps.setObject(6, it.endDate());
            ps.setInt(7, it.intervalDays());
            ps.setString(8, it.instructions());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void insertDoseTimes(Connection c, int itemId, List<LocalTime> times) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO dose_times (prescription_item_id, dose_time) VALUES (?, ?)")) {
            for (LocalTime t : times) {
                ps.setInt(1, itemId);
                ps.setObject(2, t);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertDoseRecords(Connection c, int itemId, List<LocalDateTime> doses) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO dose_records (prescription_item_id, scheduled_at) VALUES (?, ?)")) {
            for (LocalDateTime at : doses) {
                ps.setInt(1, itemId);
                ps.setObject(2, at);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<PrescriptionSummary> findPrescriptionsOfDoctor(int doctorId) throws SQLException {
        String sql = "SELECT pr.prescription_id, u.full_name, pr.prescription_date, pr.diagnosis, pr.status, "
                   + "COUNT(pi.prescription_item_id) AS item_count "
                   + "FROM prescriptions pr "
                   + "JOIN patients p ON p.patient_id = pr.patient_id "
                   + "JOIN users u ON u.user_id = p.user_id "
                   + "LEFT JOIN prescription_items pi ON pi.prescription_id = pr.prescription_id "
                   + "WHERE pr.doctor_id = ? "
                   + "GROUP BY pr.prescription_id, u.full_name, pr.prescription_date, pr.diagnosis, pr.status "
                   + "ORDER BY pr.prescription_date DESC, pr.prescription_id DESC";
        List<PrescriptionSummary> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, doctorId);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PrescriptionSummary(
                    rs.getInt("prescription_id"),
                    rs.getString("full_name"),
                    rs.getObject("prescription_date", LocalDate.class),
                    rs.getString("diagnosis"),
                    rs.getString("status"),
                    rs.getInt("item_count")));
            }
        }
        return list;
    }

    /** Items of a prescription, only if it belongs to this doctor. */
    public List<PrescriptionItemView> findItems(int prescriptionId, int doctorId) throws SQLException {
        String sql = "SELECT CONCAT(m.medication_name, ' ', m.strength) AS medication, "
                   + "pi.dosage_amount, pi.dosage_unit, pi.interval_days, pi.start_date, pi.end_date, "
                   + "pi.instructions, pi.status, "
                   + "GROUP_CONCAT(TIME_FORMAT(dt.dose_time, '%H:%i') ORDER BY dt.dose_time SEPARATOR ', ') AS times "
                   + "FROM prescription_items pi "
                   + "JOIN prescriptions pr ON pr.prescription_id = pi.prescription_id "
                   + "JOIN medications m ON m.medication_id = pi.medication_id "
                   + "LEFT JOIN dose_times dt ON dt.prescription_item_id = pi.prescription_item_id "
                   + "WHERE pi.prescription_id = ? AND pr.doctor_id = ? "
                   + "GROUP BY pi.prescription_item_id, m.medication_name, m.strength, pi.dosage_amount, "
                   + "pi.dosage_unit, pi.interval_days, pi.start_date, pi.end_date, pi.instructions, pi.status";
        List<PrescriptionItemView> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, prescriptionId, doctorId);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PrescriptionItemView(
                    rs.getString("medication"),
                    rs.getBigDecimal("dosage_amount"),
                    rs.getString("dosage_unit"),
                    rs.getString("times"),
                    rs.getInt("interval_days"),
                    rs.getObject("start_date", LocalDate.class),
                    rs.getObject("end_date", LocalDate.class),
                    rs.getString("instructions"),
                    rs.getString("status")));
            }
        }
        return list;
    }

    /** Cancels an ACTIVE prescription and all its future pending doses. */
    public boolean cancelPrescription(int prescriptionId, int doctorId, int actorUserId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int rows = update(c, "UPDATE prescriptions SET status = 'CANCELLED' "
                                   + "WHERE prescription_id = ? AND doctor_id = ? AND status = 'ACTIVE'",
                                  prescriptionId, doctorId);
                if (rows == 0) {
                    c.rollback();
                    return false;
                }
                update(c, "UPDATE prescription_items SET status = 'DISCONTINUED', discontinued_at = NOW() "
                        + "WHERE prescription_id = ? AND status = 'ACTIVE'", prescriptionId);
                update(c, "UPDATE dose_records d "
                        + "JOIN prescription_items pi ON pi.prescription_item_id = d.prescription_item_id "
                        + "SET d.status = 'CANCELLED' "
                        + "WHERE pi.prescription_id = ? AND d.status = 'PENDING' AND d.scheduled_at > NOW()",
                       prescriptionId);
                audit(c, actorUserId, "CANCEL_PRESCRIPTION", "prescriptions", prescriptionId);
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
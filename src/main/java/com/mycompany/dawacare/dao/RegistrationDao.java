package com.mycompany.dawacare.dao;

import com.mycompany.dawacare.model.RegistrationRequest;
import com.mycompany.dawacare.model.UserStatus;
import com.mycompany.dawacare.util.DBConnection;
import java.sql.*;
import java.time.LocalDate;

public class RegistrationDao {

    public int register(RegistrationRequest r, String passwordHash, UserStatus status)
            throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int userId = insertUser(c, r, passwordHash, status);
                switch (r.role()) {
                    case PATIENT   -> insertPatient(c, userId, r);
                    case DOCTOR    -> insertDoctor(c, userId, r);
                    case CAREGIVER -> insertCaregiver(c, userId);
                    case ADMIN     -> throw new SQLException("Admins cannot self-register");
                }
                c.commit();
                return userId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private int insertUser(Connection c, RegistrationRequest r, String hash, UserStatus status)
            throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, phone, role, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.username().trim());
            ps.setString(2, hash);
            ps.setString(3, r.fullName().trim());
            ps.setString(4, blankToNull(r.email()));   // NULL, not "", so UNIQUE works
            ps.setString(5, blankToNull(r.phone()));
            ps.setString(6, r.role().name());
            ps.setString(7, status.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void insertPatient(Connection c, int userId, RegistrationRequest r) throws SQLException {
        String sql = "INSERT INTO patients (user_id, date_of_birth, gender, address, "
                   + "emergency_contact_name, emergency_contact_phone) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setObject(2, LocalDate.parse(r.dateOfBirth().trim()));
            ps.setString(3, r.gender());
            ps.setString(4, blankToNull(r.address()));
            ps.setString(5, blankToNull(r.emergencyContactName()));
            ps.setString(6, blankToNull(r.emergencyContactPhone()));
            ps.executeUpdate();
        }
    }

    private void insertDoctor(Connection c, int userId, RegistrationRequest r) throws SQLException {
        String sql = "INSERT INTO doctors (user_id, license_number, specialization, hospital_name) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, r.licenseNumber().trim());
            ps.setString(3, blankToNull(r.specialization()));
            ps.setString(4, blankToNull(r.hospitalName()));
            ps.executeUpdate();
        }
    }

    private void insertCaregiver(Connection c, int userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO caregivers (user_id) VALUES (?)")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
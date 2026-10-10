package com.mycompany.dawacare.dao;

import com.mycompany.dawacare.util.DBConnection;
import java.sql.*;

public class ProfileDao {

    public Integer patientIdOf(int userId) throws SQLException {
        return idOf("SELECT patient_id FROM patients WHERE user_id = ?", userId);
    }

    public Integer caregiverIdOf(int userId) throws SQLException {
        return idOf("SELECT caregiver_id FROM caregivers WHERE user_id = ?", userId);
    }

    public Integer doctorIdOf(int userId) throws SQLException {
        return idOf("SELECT doctor_id FROM doctors WHERE user_id = ?", userId);
    }

    private Integer idOf(String sql, int userId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }
}
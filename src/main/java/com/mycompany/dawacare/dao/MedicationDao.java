package com.mycompany.dawacare.dao;

import com.mycompany.dawacare.model.CategoryOption;
import com.mycompany.dawacare.model.Medication;
import com.mycompany.dawacare.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicationDao extends BaseDao {

    private static final String SELECT =
        "SELECT m.medication_id, m.category_id, c.category_name, m.medication_name, m.generic_name, "
      + "m.dosage_form, m.strength, m.manufacturer, m.description, "
      + "(SELECT COUNT(*) FROM prescription_items pi WHERE pi.medication_id = m.medication_id) AS usage_count "
      + "FROM medications m LEFT JOIN medication_categories c ON c.category_id = m.category_id ";

    public List<Medication> findAll() throws SQLException {
        List<Medication> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "ORDER BY m.medication_name, m.strength");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Medication findById(int medicationId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, SELECT + "WHERE m.medication_id = ?", medicationId);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? map(rs) : null;
        }
    }

    public List<CategoryOption> findCategories() throws SQLException {
        List<CategoryOption> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT category_id, category_name FROM medication_categories ORDER BY category_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new CategoryOption(rs.getInt(1), rs.getString(2)));
        }
        return list;
    }

    public int insert(Medication m, int actorUserId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO medications (category_id, medication_name, generic_name, dosage_form, "
                      + "strength, manufacturer, description) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setObject(1, m.categoryId());
                    ps.setString(2, m.name());
                    ps.setString(3, m.genericName());
                    ps.setString(4, m.dosageForm());
                    ps.setString(5, m.strength());
                    ps.setString(6, m.manufacturer());
                    ps.setString(7, m.description());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getInt(1);
                    }
                }
                audit(c, actorUserId, "ADD_MEDICATION", "medications", id);
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public int update(Medication m, int actorUserId) throws SQLException {
        return changeWithAudit(
            "UPDATE medications SET category_id = ?, medication_name = ?, generic_name = ?, "
          + "dosage_form = ?, strength = ?, manufacturer = ?, description = ? WHERE medication_id = ?",
            new Object[]{m.categoryId(), m.name(), m.genericName(), m.dosageForm(), m.strength(),
                         m.manufacturer(), m.description(), m.medicationId()},
            actorUserId, "EDIT_MEDICATION", m.medicationId());
    }

    /** Deletes only if no prescription item uses it. */
    public int delete(int medicationId, int actorUserId) throws SQLException {
        return changeWithAudit(
            "DELETE FROM medications WHERE medication_id = ? "
          + "AND NOT EXISTS (SELECT 1 FROM prescription_items WHERE medication_id = ?)",
            new Object[]{medicationId, medicationId},
            actorUserId, "DELETE_MEDICATION", medicationId);
    }

    private int changeWithAudit(String sql, Object[] params, int actorUserId, String action, int entityId)
            throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int rows = update(c, sql, params);
                if (rows > 0) audit(c, actorUserId, action, "medications", entityId);
                c.commit();
                return rows;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private Medication map(ResultSet rs) throws SQLException {
        return new Medication(
            rs.getInt("medication_id"),
            rs.getObject("category_id", Integer.class),
            rs.getString("category_name"),
            rs.getString("medication_name"),
            rs.getString("generic_name"),
            rs.getString("dosage_form"),
            rs.getString("strength"),
            rs.getString("manufacturer"),
            rs.getString("description"),
            rs.getInt("usage_count"));
    }
}
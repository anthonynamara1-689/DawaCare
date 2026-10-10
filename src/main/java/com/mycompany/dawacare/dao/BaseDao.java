package com.mycompany.dawacare.dao;

import com.mycompany.dawacare.util.DBConnection;
import java.sql.*;

public abstract class BaseDao {

    protected PreparedStatement prepare(Connection c, String sql, Object... params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps;
    }

    /** Update inside an existing transaction. */
    protected int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, params)) {
            return ps.executeUpdate();
        }
    }

    /** Standalone update with its own connection. */
    protected int update(String sql, Object... params) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return update(c, sql, params);
        }
    }

    protected <T> T queryValue(String sql, Class<T> type, Object... params) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = prepare(c, sql, params);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getObject(1, type) : null;
        }
    }

    protected void audit(Connection c, int actorUserId, String action, String entity, int entityId)
            throws SQLException {
        update(c, "INSERT INTO audit_log (user_id, action, entity, entity_id) VALUES (?, ?, ?, ?)",
               actorUserId, action, entity, entityId);
    }
}
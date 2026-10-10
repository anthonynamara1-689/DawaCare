package com.mycompany.dawacare.dao;

import com.mycompany.dawacare.model.*;
import com.mycompany.dawacare.util.DBConnection;
import java.sql.*;

public class UserDao extends BaseDao {

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, full_name, email, phone, role, status "
                   + "FROM users WHERE username = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE users SET last_login_at = NOW() WHERE user_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("user_id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("full_name"),
            rs.getString("email"),
            rs.getString("phone"),
            Role.valueOf(rs.getString("role")),
            UserStatus.valueOf(rs.getString("status")));
    }
    
    public void updatePassword(int userId, String newHash) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                update(c, "UPDATE users SET password_hash = ? WHERE user_id = ?", newHash, userId);
                audit(c, userId, "CHANGE_PASSWORD", "users", userId);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
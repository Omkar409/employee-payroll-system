package com.srm.payroll.dao.impl;

import com.srm.payroll.config.DatabaseConnection;
import com.srm.payroll.dao.DataAccessException;
import com.srm.payroll.dao.UserDAO;
import com.srm.payroll.model.Role;
import com.srm.payroll.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAOImpl implements UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAOImpl.class.getName());

    @Override
    public int insert(User user) throws DataAccessException {
        String sql = "INSERT INTO users (username, password_hash, password_salt, full_name, email, role, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getPasswordSalt());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getRole().name());
            ps.setInt(7, user.isActive() ? 1 : 0);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    user.setUserId(id);
                    return id;
                }
            }
            throw new DataAccessException("User creation succeeded but no generated ID was obtained.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error inserting user: " + user.getUsername(), e);
            if ("23000".equals(e.getSQLState())) {
                throw new DataAccessException("A user with this username or email already exists.", e);
            }
            throw new DataAccessException("Database error occurred while creating user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) throws DataAccessException {
        String sql = "SELECT user_id, username, password_hash, password_salt, full_name, email, role, " +
                     "is_active, created_at, last_login_at FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error querying user by username: " + username, e);
            throw new DataAccessException("Database error occurred while looking up user.", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) throws DataAccessException {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error checking username existence: " + username, e);
            throw new DataAccessException("Database error occurred while checking username.", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) throws DataAccessException {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error checking email existence: " + email, e);
            throw new DataAccessException("Database error occurred while checking email.", e);
        }
    }

    @Override
    public void updateLastLogin(int userId) throws DataAccessException {
        String sql = "UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error updating last login for user ID: " + userId, e);
            throw new DataAccessException("Database error occurred while updating last login.", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setPasswordSalt(rs.getString("password_salt"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setActive(rs.getInt("is_active") == 1);

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            user.setCreatedAt(createdTs.toLocalDateTime());
        }
        Timestamp loginTs = rs.getTimestamp("last_login_at");
        if (loginTs != null) {
            user.setLastLoginAt(loginTs.toLocalDateTime());
        }
        return user;
    }
}


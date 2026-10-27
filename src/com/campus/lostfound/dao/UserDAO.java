package com.campus.lostfound.dao;

import com.campus.lostfound.config.DBConnection;
import com.campus.lostfound.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO (Data Access Object)
 * 
 * Handles all database operations for the 'users' table using PreparedStatements.
 * Ensures SQL injection safety and robust connection management.
 */
public class UserDAO {

    /**
     * Registers a new user in the 'users' table.
     * The password must already be hashed using PasswordUtil.hashPassword() before calling this.
     * 
     * @param user User object containing name, email, hashed password, and role
     * @return true if registration succeeded, false otherwise
     */
    public boolean register(User user) {
        String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getRole().name()); // Matches ENUM('STUDENT','ADMIN')

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error registering user: " + e.getMessage());
        }
        return false;
    }

    /**
     * Finds a user by their registered email address.
     * 
     * @param email User's email
     * @return User object if found, or null if not found
     */
    public User findByEmail(String email) {
        String sql = "SELECT user_id, name, email, password, role, created_at FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error finding user by email: " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds a user by their unique primary key ID.
     * 
     * @param userId The ID of the user
     * @return User object if found, or null if not found
     */
    public User findById(int userId) {
        String sql = "SELECT user_id, name, email, password, role, created_at FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error finding user by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Checks if an email is already registered in the system.
     * 
     * @param email Email to verify
     * @return true if already in use, false otherwise
     */
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error checking email existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves all registered users (for admin management).
     * 
     * @return List of all users
     */
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        String sql = "SELECT user_id, name, email, password, role, created_at FROM users ORDER BY user_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                userList.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error fetching all users: " + e.getMessage());
        }
        return userList;
    }

    /**
     * Updates a user's role (e.g. promoting a user to ADMIN).
     * 
     * @param userId ID of the user
     * @param newRole New Role to set
     * @return true if updated, false otherwise
     */
    public boolean updateUserRole(int userId, User.Role newRole) {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRole.name());
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updating user role: " + e.getMessage());
        }
        return false;
    }

    /**
     * Deletes a user by their ID.
     * 
     * @param userId ID of the user to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error deleting user: " + e.getMessage());
        }
        return false;
    }

    /**
     * Counts how many users currently have the ADMIN role.
     * Used to prevent deleting or demoting the last active administrator.
     * 
     * @return Number of active admin accounts
     */
    public int countAdminUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error counting admins: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Checks if a user has any existing reported items or submitted claims.
     * Used to prevent foreign key constraint violations upon deletion.
     * 
     * @param userId User ID to check
     * @return true if related records exist, false if safe to delete
     */
    public boolean hasUserRelatedRecords(int userId) {
        String sql = "SELECT " +
                     "  (SELECT COUNT(*) FROM items WHERE reported_by = ?) + " +
                     "  (SELECT COUNT(*) FROM claims WHERE claimed_by = ?) AS total_rel";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_rel") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error checking user related records: " + e.getMessage());
        }
        return false;
    }

    /**
     * Returns the total number of registered users.
     */
    public int getTotalUsersCount() {
        String sql = "SELECT COUNT(*) FROM users";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error getting user count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Helper method to convert a ResultSet row into a User model.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(User.Role.fromString(rs.getString("role")));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}

package com.campus.lostfound.dao;

import com.campus.lostfound.config.DBConnection;
import com.campus.lostfound.models.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationDAO (Data Access Object)
 * 
 * Handles database operations for the 'notifications' table using PreparedStatements.
 * Features:
 * - Scoped queries ensuring users can only read and modify their own notifications.
 * - Concurrency-safe deduplication via unique dedup_key and INSERT IGNORE.
 * - In-transaction insertion helper for business workflows.
 * - Safe missing-table handling that preserves primary workflows.
 */
public class NotificationDAO {

    private static volatile Boolean tableExists = null;

    /**
     * Checks if the notifications table exists in the connected database.
     * Caches affirmative result to avoid repeated metadata lookups.
     */
    public static boolean isTableAvailable(Connection conn) {
        if (Boolean.TRUE.equals(tableExists)) {
            return true;
        }
        try {
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "notifications", new String[]{"TABLE"})) {
                if (rs.next()) {
                    tableExists = true;
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] Metadata check failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Inserts a notification into an existing transaction connection.
     * Uses INSERT IGNORE with a unique dedup_key constraint for concurrency-safe deduplication.
     * If the table is missing or already deduplicated, safely logs and does not throw so outer tx succeeds.
     */
    public boolean insertInTransaction(Connection conn, Notification notification) {
        if (!isTableAvailable(conn)) {
            System.out.println("[NotificationDAO] Notice: 'notifications' table not found. Notification skipped safely.");
            return false;
        }

        String sql = "INSERT IGNORE INTO notifications (user_id, title, message, notification_type, reference_type, reference_id, dedup_key, is_read) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, notification.getUserId());
            pstmt.setString(2, notification.getTitle());
            pstmt.setString(3, notification.getMessage());
            pstmt.setString(4, notification.getNotificationType().name());
            pstmt.setString(5, notification.getReferenceType());
            if (notification.getReferenceId() != null) {
                pstmt.setInt(6, notification.getReferenceId());
            } else {
                pstmt.setNull(6, java.sql.Types.INTEGER);
            }
            pstmt.setString(7, notification.getDedupKey());
            pstmt.setBoolean(8, notification.isRead());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        notification.setNotificationId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            // Error code 1146 = ER_NO_SUCH_TABLE
            if (e.getErrorCode() == 1146 || e.getMessage().toLowerCase().contains("doesn't exist")) {
                tableExists = false;
                System.err.println("[NotificationDAO] Table 'notifications' is missing. Please run sql/migration_feature7_notifications.sql.");
            } else {
                System.err.println("[NotificationDAO] Error inserting notification in transaction: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Creates a new notification using a standalone connection.
     */
    public boolean createNotification(Notification notification) {
        try (Connection conn = DBConnection.getConnection()) {
            return insertInTransaction(conn, notification);
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] Database connection error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all notifications for a specific recipient user, newest first.
     * Scoped strictly by user_id to enforce user-level data isolation.
     */
    public List<Notification> getNotificationsByUser(int userId, boolean unreadOnly) {
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            if (!isTableAvailable(conn)) {
                return list;
            }

            StringBuilder sql = new StringBuilder(
                    "SELECT notification_id, user_id, title, message, notification_type, reference_type, reference_id, dedup_key, is_read, created_at " +
                    "FROM notifications WHERE user_id = ? "
            );
            if (unreadOnly) {
                sql.append("AND is_read = FALSE ");
            }
            sql.append("ORDER BY created_at DESC, notification_id DESC");

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                pstmt.setInt(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToNotification(rs));
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1146 || e.getMessage().toLowerCase().contains("doesn't exist")) {
                tableExists = false;
            } else {
                System.err.println("[NotificationDAO] Error fetching notifications: " + e.getMessage());
            }
        }
        return list;
    }

    /**
     * Gets count of unread notifications for a specific user.
     */
    public int getUnreadCount(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isTableAvailable(conn)) {
                return 0;
            }

            String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1146 || e.getMessage().toLowerCase().contains("doesn't exist")) {
                tableExists = false;
            } else {
                System.err.println("[NotificationDAO] Error counting unread notifications: " + e.getMessage());
            }
        }
        return 0;
    }

    /**
     * Marks a specific notification as read, ensuring it belongs to the given user.
     */
    public boolean markAsRead(int notificationId, int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isTableAvailable(conn)) {
                return false;
            }

            String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ? AND user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, notificationId);
                pstmt.setInt(2, userId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] Error marking notification as read: " + e.getMessage());
            return false;
        }
    }

    /**
     * Marks all notifications as read for a specific user.
     */
    public boolean markAllAsRead(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isTableAvailable(conn)) {
                return false;
            }

            String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] Error marking all notifications as read: " + e.getMessage());
            return false;
        }
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getInt("notification_id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setNotificationType(Notification.NotificationType.fromString(rs.getString("notification_type")));
        n.setReferenceType(rs.getString("reference_type"));
        int refId = rs.getInt("reference_id");
        n.setReferenceId(rs.wasNull() ? null : refId);
        n.setDedupKey(rs.getString("dedup_key"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(rs.getTimestamp("created_at"));
        return n;
    }
}

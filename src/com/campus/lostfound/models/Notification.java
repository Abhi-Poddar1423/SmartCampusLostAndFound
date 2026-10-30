package com.campus.lostfound.models;

import java.sql.Timestamp;

/**
 * Notification Model
 * 
 * Represents an in-app system notification for a specific recipient user.
 * Linked to the MySQL 'notifications' table.
 */
public class Notification {

    public enum NotificationType {
        CLAIM_SUBMITTED,
        CLAIM_APPROVED,
        CLAIM_REJECTED,
        ITEM_RETURNED,
        ITEM_STATUS_CHANGED,
        GENERAL;

        public static NotificationType fromString(String str) {
            if (str == null) return GENERAL;
            try {
                return NotificationType.valueOf(str.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return GENERAL;
            }
        }
    }

    private int notificationId;
    private int userId;
    private String title;
    private String message;
    private NotificationType notificationType;
    private String referenceType;
    private Integer referenceId;
    private String dedupKey;
    private boolean isRead;
    private Timestamp createdAt;

    public Notification() {
        this.notificationType = NotificationType.GENERAL;
        this.isRead = false;
    }

    public Notification(int userId, String title, String message, NotificationType notificationType,
                        String referenceType, Integer referenceId, String dedupKey) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType != null ? notificationType : NotificationType.GENERAL;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.dedupKey = dedupKey;
        this.isRead = false;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", type=" + notificationType +
                ", dedupKey='" + dedupKey + '\'' +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}

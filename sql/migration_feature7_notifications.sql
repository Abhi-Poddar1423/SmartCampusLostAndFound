-- ====================================================================
-- Feature 7: Notification System Database Migration Script
-- Database Target: MySQL 8.0+
-- File: sql/migration_feature7_notifications.sql
-- (DO NOT EXECUTE AUTOMATICALLY - STANDALONE REVIEW SCRIPT ONLY)
-- ====================================================================

-- 1. Create table for system notifications linked to recipient users
CREATE TABLE IF NOT EXISTS notifications (
    notification_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NOT NULL,
    title             VARCHAR(150) NOT NULL,
    message           TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    reference_type    VARCHAR(50) NULL,
    reference_id      INT NULL,
    dedup_key         VARCHAR(191) NULL,
    is_read           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_notifications_dedup UNIQUE (dedup_key),
    INDEX idx_notifications_user_read (user_id, is_read),
    INDEX idx_notifications_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================================
-- Feature 8: Student Profiles & Contact Coordination Migration Script
-- Database Target: MySQL 8.0+
-- File: sql/migration_feature8_student_profiles.sql
-- (DO NOT EXECUTE AUTOMATICALLY - STANDALONE REVIEW SCRIPT ONLY)
-- ====================================================================

-- 1. Extend 'users' table to support student campus profile details
-- All new fields are NULLable with phone_public defaulting to FALSE
-- Ensuring 100% backward compatibility for all existing user accounts.

ALTER TABLE users
    ADD COLUMN roll_number VARCHAR(50) NULL AFTER email,
    ADD COLUMN department VARCHAR(100) NULL AFTER roll_number,
    ADD COLUMN semester VARCHAR(20) NULL AFTER department,
    ADD COLUMN phone_number VARCHAR(20) NULL AFTER semester,
    ADD COLUMN phone_public BOOLEAN NOT NULL DEFAULT FALSE AFTER phone_number;

-- 2. Add unique constraint on roll_number (NULL values are ignored by MySQL unique indexes)
ALTER TABLE users ADD CONSTRAINT uq_users_roll_number UNIQUE (roll_number);

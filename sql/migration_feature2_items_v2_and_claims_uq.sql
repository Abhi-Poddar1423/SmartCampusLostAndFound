-- ====================================================================
-- Smart Campus Lost & Found: Database Schema Completion Migration
-- Target Database: smart_campus_db (MySQL 8.0+)
-- File: sql/migration_feature2_items_v2_and_claims_uq.sql
-- ====================================================================

-- --------------------------------------------------------------------
-- STEP 1: Extend 'items' table for Image & Structured Location
-- All fields are NULLable to ensure 100% backward compatibility
-- with existing records (image binary is NEVER stored in database).
-- --------------------------------------------------------------------
ALTER TABLE items
    ADD COLUMN image_path VARCHAR(255) NULL AFTER location,
    ADD COLUMN building VARCHAR(100) NULL AFTER image_path,
    ADD COLUMN floor_room VARCHAR(100) NULL AFTER building;

-- --------------------------------------------------------------------
-- STEP 2: Enforce Database-Level Duplicate Claim Prevention
-- Guarantees a student can submit at most ONE claim per item.
-- --------------------------------------------------------------------
ALTER TABLE claims
    ADD CONSTRAINT uq_claims_item_claimant UNIQUE (item_id, claimed_by);

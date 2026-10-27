package com.campus.lostfound.models;

import java.sql.Timestamp;

/**
 * Claim Model
 * 
 * Represents an ownership claim record from the MySQL 'claims' table.
 * Matches exact database schema:
 * - claim_id    INT (Primary Key, Auto Increment)
 * - item_id     INT NOT NULL (Foreign Key referencing items.item_id)
 * - claimed_by  INT NOT NULL (Foreign Key referencing users.user_id)
 * - message     VARCHAR(500)
 * - status      ENUM('PENDING', 'APPROVED', 'REJECTED')
 * - claim_date  TIMESTAMP
 */
public class Claim {

    // Enum matching MySQL: enum('PENDING', 'APPROVED', 'REJECTED')
    public enum ClaimStatus {
        PENDING,
        APPROVED,
        REJECTED;

        public static ClaimStatus fromString(String statusStr) {
            if (statusStr == null) return PENDING;
            try {
                return ClaimStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return PENDING;
            }
        }
    }

    private int claimId;
    private int itemId;
    private int claimedBy;
    private String message;
    private ClaimStatus status;
    private Timestamp claimDate;

    // Joined fields for convenient GUI table display
    private String itemTitle;
    private String claimantName;
    private String claimantEmail;

    public Claim() {
        this.status = ClaimStatus.PENDING;
    }

    // Constructor for submitting a new claim
    public Claim(int itemId, int claimedBy, String message) {
        this.itemId = itemId;
        this.claimedBy = claimedBy;
        this.message = message;
        this.status = ClaimStatus.PENDING;
    }

    // Full constructor (retrieval from database)
    public Claim(int claimId, int itemId, int claimedBy, String message, ClaimStatus status, Timestamp claimDate) {
        this.claimId = claimId;
        this.itemId = itemId;
        this.claimedBy = claimedBy;
        this.message = message;
        this.status = status != null ? status : ClaimStatus.PENDING;
        this.claimDate = claimDate;
    }

    // Getters and Setters
    public int getClaimId() {
        return claimId;
    }

    public void setClaimId(int claimId) {
        this.claimId = claimId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(int claimedBy) {
        this.claimedBy = claimedBy;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public Timestamp getClaimDate() {
        return claimDate;
    }

    public void setClaimDate(Timestamp claimDate) {
        this.claimDate = claimDate;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public String getClaimantName() {
        return claimantName;
    }

    public void setClaimantName(String claimantName) {
        this.claimantName = claimantName;
    }

    public String getClaimantEmail() {
        return claimantEmail;
    }

    public void setClaimantEmail(String claimantEmail) {
        this.claimantEmail = claimantEmail;
    }

    @Override
    public String toString() {
        return "Claim{" +
                "claimId=" + claimId +
                ", itemId=" + itemId +
                ", claimedBy=" + claimedBy +
                ", status=" + status +
                ", claimDate=" + claimDate +
                '}';
    }
}

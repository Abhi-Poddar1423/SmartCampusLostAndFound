package com.campus.lostfound.dao;

import com.campus.lostfound.config.DBConnection;
import com.campus.lostfound.models.Claim;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * ClaimDAO (Data Access Object)
 * 
 * Handles database operations for the 'claims' table using PreparedStatements.
 * Allows students to submit claims and admins to approve or reject them.
 */
public class ClaimDAO {

    /**
     * Submits a new claim for an item.
     * 
     * @param claim Claim object containing itemId, claimedBy, and proof message
     * @return true if claim was submitted successfully, false otherwise
     */
    public boolean submitClaim(Claim claim) {
        String sql = "INSERT INTO claims (item_id, claimed_by, message, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, claim.getItemId());
            pstmt.setInt(2, claim.getClaimedBy());
            pstmt.setString(3, claim.getMessage());
            pstmt.setString(4, claim.getStatus().name()); // Matches ENUM('PENDING','APPROVED','REJECTED')

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        claim.setClaimId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ClaimDAO] Error submitting claim: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if a user has already submitted a claim for a given item.
     * 
     * @param itemId Item ID
     * @param userId User ID
     * @return true if an existing claim is found, false otherwise
     */
    public boolean hasUserClaimed(int itemId, int userId) {
        String sql = "SELECT 1 FROM claims WHERE item_id = ? AND claimed_by = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ClaimDAO] Error checking duplicate claim: " + e.getMessage());
        }
        return false;
    }

    /**
     * Finds a claim by its primary key ID.
     * 
     * @param claimId Claim ID
     * @return Claim object if found, or null
     */
    public Claim findById(int claimId) {
        String sql = "SELECT c.claim_id, c.item_id, c.claimed_by, c.message, c.status, c.claim_date, " +
                     "       i.title AS item_title, u.name AS claimant_name, u.email AS claimant_email " +
                     "FROM claims c " +
                     "JOIN items i ON c.item_id = i.item_id " +
                     "JOIN users u ON c.claimed_by = u.user_id " +
                     "WHERE c.claim_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, claimId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClaim(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ClaimDAO] Error finding claim by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all claims with joined item and claimant details (for Admin Dashboard).
     * 
     * @return List of all claims
     */
    public List<Claim> getAllClaims() {
        List<Claim> claims = new ArrayList<>();
        String sql = "SELECT c.claim_id, c.item_id, c.claimed_by, c.message, c.status, c.claim_date, " +
                     "       i.title AS item_title, u.name AS claimant_name, u.email AS claimant_email " +
                     "FROM claims c " +
                     "JOIN items i ON c.item_id = i.item_id " +
                     "JOIN users u ON c.claimed_by = u.user_id " +
                     "ORDER BY c.claim_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                claims.add(mapResultSetToClaim(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ClaimDAO] Error fetching all claims: " + e.getMessage());
        }
        return claims;
    }

    /**
     * Retrieves all claims submitted by a specific student.
     * 
     * @param userId Student's user ID
     * @return List of claims submitted by this student
     */
    public List<Claim> getClaimsByUser(int userId) {
        List<Claim> claims = new ArrayList<>();
        String sql = "SELECT c.claim_id, c.item_id, c.claimed_by, c.message, c.status, c.claim_date, " +
                     "       i.title AS item_title, u.name AS claimant_name, u.email AS claimant_email " +
                     "FROM claims c " +
                     "JOIN items i ON c.item_id = i.item_id " +
                     "JOIN users u ON c.claimed_by = u.user_id " +
                     "WHERE c.claimed_by = ? " +
                     "ORDER BY c.claim_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    claims.add(mapResultSetToClaim(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ClaimDAO] Error fetching claims by user: " + e.getMessage());
        }
        return claims;
    }

    /**
     * Updates the status of a claim (PENDING, APPROVED, REJECTED).
     * 
     * @param claimId Claim ID
     * @param newStatus New status to set
     * @return true if updated, false otherwise
     */
    public boolean updateClaimStatus(int claimId, Claim.ClaimStatus newStatus) {
        String sql = "UPDATE claims SET status = ? WHERE claim_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, claimId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ClaimDAO] Error updating claim status: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if an item already has an approved claim.
     */
    public boolean hasApprovedClaimForItem(int itemId) {
        String sql = "SELECT 1 FROM claims WHERE item_id = ? AND status = 'APPROVED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ClaimDAO] Error checking approved claims: " + e.getMessage());
        }
        return false;
    }

    /**
     * Approves a claim in a single atomic database transaction:
     * 1. Verifies the item doesn't already have an approved claim.
     * 2. Sets claim status to 'APPROVED'.
     * 3. Sets item status to 'CLAIMED'.
     * 4. Rejects all other remaining PENDING claims on this same item.
     * 5. Commits the transaction or rolls back on any failure.
     * 
     * @param claimId ID of claim to approve
     * @param itemId ID of the item being claimed
     * @return true if approval transaction succeeded
     * @throws IllegalStateException with specific reason if state is invalid
     */
    public boolean approveClaimTransaction(int claimId, int itemId) throws Exception {
        String checkApprovedSql = "SELECT claim_id FROM claims WHERE item_id = ? AND status = 'APPROVED'";
        String updateClaimSql = "UPDATE claims SET status = 'APPROVED' WHERE claim_id = ?";
        String updateItemSql = "UPDATE items SET status = 'CLAIMED' WHERE item_id = ?";
        String rejectOthersSql = "UPDATE claims SET status = 'REJECTED' WHERE item_id = ? AND claim_id != ? AND status = 'PENDING'";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Conflict check: Has an approved claim already been recorded?
            try (PreparedStatement checkStmt = conn.prepareStatement(checkApprovedSql)) {
                checkStmt.setInt(1, itemId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int existingApprovedId = rs.getInt(1);
                        if (existingApprovedId != claimId) {
                            throw new IllegalStateException("Item already has an approved claim (#" + existingApprovedId + "). Cannot approve multiple conflicting claims.");
                        }
                    }
                }
            }

            // 2. Set this claim to APPROVED
            try (PreparedStatement pClaim = conn.prepareStatement(updateClaimSql)) {
                pClaim.setInt(1, claimId);
                int r1 = pClaim.executeUpdate();
                if (r1 == 0) throw new SQLException("Claim #" + claimId + " not found.");
            }

            // 3. Set item status to CLAIMED
            try (PreparedStatement pItem = conn.prepareStatement(updateItemSql)) {
                pItem.setInt(1, itemId);
                pItem.executeUpdate();
            }

            // 4. Reject other competing pending claims
            try (PreparedStatement pOthers = conn.prepareStatement(rejectOthersSql)) {
                pOthers.setInt(1, itemId);
                pOthers.setInt(2, claimId);
                pOthers.executeUpdate();
            }

            conn.commit(); // Atomic commit
            return true;

        } catch (Exception ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[ClaimDAO] Rollback failed: " + rollbackEx.getMessage());
                }
            }
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) { }
            }
        }
    }

    /**
     * Rejects a claim. If the item was not approved by another claim, leaves item state intact.
     * 
     * @param claimId Claim ID
     * @return true if updated to REJECTED
     */
    public boolean rejectClaim(int claimId) {
        return updateClaimStatus(claimId, Claim.ClaimStatus.REJECTED);
    }

    /**
     * Counts all claims currently in PENDING review status.
     */
    public int getPendingClaimsCount() {
        String sql = "SELECT COUNT(*) FROM claims WHERE status = 'PENDING'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ClaimDAO] Error counting pending claims: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Helper method to map a ResultSet row to a Claim model.
     */
    private Claim mapResultSetToClaim(ResultSet rs) throws SQLException {
        Claim claim = new Claim();
        claim.setClaimId(rs.getInt("claim_id"));
        claim.setItemId(rs.getInt("item_id"));
        claim.setClaimedBy(rs.getInt("claimed_by"));
        claim.setMessage(rs.getString("message"));
        claim.setStatus(Claim.ClaimStatus.fromString(rs.getString("status")));
        claim.setClaimDate(rs.getTimestamp("claim_date"));

        try {
            claim.setItemTitle(rs.getString("item_title"));
            claim.setClaimantName(rs.getString("claimant_name"));
            claim.setClaimantEmail(rs.getString("claimant_email"));
        } catch (SQLException ignored) { }

        return claim;
    }
}

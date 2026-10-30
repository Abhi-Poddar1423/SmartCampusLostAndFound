package com.campus.lostfound.dao;

import com.campus.lostfound.config.DBConnection;
import com.campus.lostfound.models.Claim;
import com.campus.lostfound.utils.SessionManager;

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
                notifyAdminOnClaimSubmitted(conn, claim);
                return true;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.err.println("[ClaimDAO] Duplicate claim prevented by database constraint (uq_claims_item_claimant): User " + 
                        claim.getClaimedBy() + " has already submitted a claim for item " + claim.getItemId());
            } else {
                System.err.println("[ClaimDAO] Error submitting claim: " + e.getMessage());
            }
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
     * Enforces backend administrator authorization.
     * 
     * @param claimId Claim ID
     * @param newStatus New status to set
     * @return true if updated, false otherwise
     */
    public boolean updateClaimStatus(int claimId, Claim.ClaimStatus newStatus) {
        int currentUserId = SessionManager.getInstance().getUserId();
        if (currentUserId <= 0 || !new UserDAO().isActiveAdmin(currentUserId)) {
            throw new SecurityException("Unauthorized: Only an active Administrator may update claim status.");
        }

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
     * Retrieves the most relevant or recent claim for an item.
     * Prioritizes APPROVED, then PENDING, then REJECTED.
     * 
     * @param itemId Item ID
     * @return Claim object or null if none
     */
    public Claim getLatestClaimForItem(int itemId) {
        String sql = "SELECT c.claim_id, c.item_id, c.claimed_by, c.message, c.status, c.claim_date, " +
                     "       i.title AS item_title, u.name AS claimant_name, u.email AS claimant_email " +
                     "FROM claims c " +
                     "JOIN items i ON c.item_id = i.item_id " +
                     "JOIN users u ON c.claimed_by = u.user_id " +
                     "WHERE c.item_id = ? " +
                     "ORDER BY CASE c.status WHEN 'APPROVED' THEN 1 WHEN 'PENDING' THEN 2 ELSE 3 END, c.claim_date DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClaim(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ClaimDAO] Error finding claim by item ID: " + e.getMessage());
        }
        return null;
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
        int currentUserId = SessionManager.getInstance().getUserId();
        if (currentUserId <= 0 || !new UserDAO().isActiveAdmin(currentUserId)) {
            throw new SecurityException("Unauthorized: Only an active Administrator may approve ownership claims.");
        }

        String checkApprovedSql = "SELECT claim_id FROM claims WHERE item_id = ? AND status = 'APPROVED'";
        String getClaimDetailsSql = "SELECT c.claimed_by, i.title, i.reported_by FROM claims c JOIN items i ON c.item_id = i.item_id WHERE c.claim_id = ?";
        String getCompetingClaimsSql = "SELECT claim_id, claimed_by FROM claims WHERE item_id = ? AND claim_id != ? AND status = 'PENDING'";
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

            // Retrieve details for notifications
            int claimantId = -1;
            int reportedBy = -1;
            String itemTitle = "Item #" + itemId;
            try (PreparedStatement pDetails = conn.prepareStatement(getClaimDetailsSql)) {
                pDetails.setInt(1, claimId);
                try (ResultSet rs = pDetails.executeQuery()) {
                    if (rs.next()) {
                        claimantId = rs.getInt("claimed_by");
                        itemTitle = rs.getString("title");
                        reportedBy = rs.getInt("reported_by");
                    }
                }
            }

            // Fetch competing claims to notify them of rejection
            List<int[]> competingClaims = new ArrayList<>(); // [competingClaimId, competingClaimantId]
            try (PreparedStatement pComp = conn.prepareStatement(getCompetingClaimsSql)) {
                pComp.setInt(1, itemId);
                pComp.setInt(2, claimId);
                try (ResultSet rs = pComp.executeQuery()) {
                    while (rs.next()) {
                        competingClaims.add(new int[]{rs.getInt("claim_id"), rs.getInt("claimed_by")});
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

            // 5. In-transaction notifications with concurrency-safe dedup
            NotificationDAO notifDAO = new NotificationDAO();
            if (claimantId > 0) {
                com.campus.lostfound.models.Notification nClaimant = new com.campus.lostfound.models.Notification(
                        claimantId,
                        "Claim Approved",
                        "Your claim for '" + itemTitle + "' has been approved by campus administration.",
                        com.campus.lostfound.models.Notification.NotificationType.CLAIM_APPROVED,
                        "CLAIM",
                        claimId,
                        "CLAIM_APPROVED:" + claimId
                );
                notifDAO.insertInTransaction(conn, nClaimant);
            }

            if (reportedBy > 0 && reportedBy != claimantId) {
                com.campus.lostfound.models.Notification nReporter = new com.campus.lostfound.models.Notification(
                        reportedBy,
                        "Claim Approved on Your Reported Item",
                        "A claim for your reported item '" + itemTitle + "' has been approved. Status changed to CLAIMED.",
                        com.campus.lostfound.models.Notification.NotificationType.ITEM_STATUS_CHANGED,
                        "ITEM",
                        itemId,
                        "ITEM_CLAIMED_BY_APPROVE:" + itemId + ":" + claimId
                );
                notifDAO.insertInTransaction(conn, nReporter);
            }

            // Notify competing claimants of rejection
            for (int[] comp : competingClaims) {
                int compClaimId = comp[0];
                int compClaimantId = comp[1];
                if (compClaimantId > 0) {
                    com.campus.lostfound.models.Notification nComp = new com.campus.lostfound.models.Notification(
                            compClaimantId,
                            "Claim Not Selected",
                            "Another claim was approved for '" + itemTitle + "'. Your claim #" + compClaimId + " has been closed.",
                            com.campus.lostfound.models.Notification.NotificationType.CLAIM_REJECTED,
                            "CLAIM",
                            compClaimId,
                            "CLAIM_REJECTED_COMPETING:" + compClaimId
                    );
                    notifDAO.insertInTransaction(conn, nComp);
                }
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
        Claim claim = findById(claimId);
        boolean updated = updateClaimStatus(claimId, Claim.ClaimStatus.REJECTED);
        if (updated && claim != null && claim.getClaimedBy() > 0) {
            NotificationDAO notifDAO = new NotificationDAO();
            String title = claim.getItemTitle() != null ? claim.getItemTitle() : "Item #" + claim.getItemId();
            com.campus.lostfound.models.Notification n = new com.campus.lostfound.models.Notification(
                    claim.getClaimedBy(),
                    "Claim Rejected",
                    "Your claim for '" + title + "' was reviewed and not approved.",
                    com.campus.lostfound.models.Notification.NotificationType.CLAIM_REJECTED,
                    "CLAIM",
                    claimId,
                    "CLAIM_REJECTED:" + claimId
            );
            notifDAO.createNotification(n);
        }
        return updated;
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

    private void notifyAdminOnClaimSubmitted(Connection conn, Claim claim) {
        if (claim == null || claim.getClaimId() <= 0) return;
        try {
            String itemTitle = "Item #" + claim.getItemId();
            int reportedBy = -1;
            String sql = "SELECT title, reported_by FROM items WHERE item_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, claim.getItemId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        itemTitle = rs.getString("title");
                        reportedBy = rs.getInt("reported_by");
                    }
                }
            }

            NotificationDAO notifDAO = new NotificationDAO();

            // 1. Notify Admin
            int adminId = new UserDAO().getActiveAdminUserId();
            if (adminId > 0 && adminId != claim.getClaimedBy()) {
                com.campus.lostfound.models.Notification n = new com.campus.lostfound.models.Notification(
                        adminId,
                        "New Claim Pending Review",
                        "A student submitted a claim for item '" + itemTitle + "'. Review required in Manage Claims.",
                        com.campus.lostfound.models.Notification.NotificationType.CLAIM_SUBMITTED,
                        "CLAIM",
                        claim.getClaimId(),
                        "ADMIN_CLAIM_PENDING:" + claim.getClaimId()
                );
                notifDAO.createNotification(n);
            }

            // 2. Notify Item Reporter (if not the claimant themselves)
            if (reportedBy > 0 && reportedBy != claim.getClaimedBy()) {
                com.campus.lostfound.models.Notification nReporter = new com.campus.lostfound.models.Notification(
                        reportedBy,
                        "New Claim Submitted",
                        "A student submitted an ownership claim for your reported item '" + itemTitle + "'.",
                        com.campus.lostfound.models.Notification.NotificationType.CLAIM_SUBMITTED,
                        "CLAIM",
                        claim.getClaimId(),
                        "CLAIM_SUBMITTED:" + claim.getClaimId()
                );
                notifDAO.createNotification(nReporter);
            }
        } catch (Exception e) {
            System.err.println("[ClaimDAO] Notice: Failed to dispatch notifications on claim submission: " + e.getMessage());
        }
    }
}

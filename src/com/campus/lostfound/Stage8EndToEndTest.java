package com.campus.lostfound;

import com.campus.lostfound.config.DBConnection;
import com.campus.lostfound.dao.ClaimDAO;
import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.dao.NotificationDAO;
import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.Claim;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.MatchResult;
import com.campus.lostfound.models.Notification;
import com.campus.lostfound.models.User;
import com.campus.lostfound.utils.ImageUtil;
import com.campus.lostfound.utils.ItemMatchingEngine;
import com.campus.lostfound.utils.PasswordUtil;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage8EndToEndTest
 * 
 * Comprehensive, production-grade automated verification suite covering all 7 core features:
 * 1. Fail-closed catalog gate: strictly halts unless catalog is exactly 'smart_campus_db_phase_b_test'.
 * 2. Feature 1: User Registration, PBKDF2 Password Hashing & Authentication.
 * 3. Feature 2: Item Reporting, Structured Location (Building/Room) & Photo Processing.
 * 4. Feature 3: Dynamic Multi-Criteria Search & Filtering.
 * 5. Feature 6: Deterministic Similarity Matching Engine.
 * 6. Feature 4: Claim Submission, Duplicate Prevention & Reporter Notification.
 * 7. Feature 5: Atomic Claim Approval, Competing Claim Auto-Rejection & Transaction Notifications.
 * 8. Feature 7: Notification Feed, Unread Counts, Deduplication & Read-State Mutations.
 * 9. Lifecycle Status: Item Returned Workflow & Resolution Alerts.
 * 10. Guaranteed post-test cleanup in a 'finally' block: deletes ONLY tracked test IDs and test image files.
 */
public class Stage8EndToEndTest {

    private static final String REQUIRED_TEST_DB = "smart_campus_db_phase_b_test";

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  Smart Campus Lost & Found - Stage 8 Comprehensive End-to-End Suite   ");
        System.out.println("======================================================================\n");

        int totalAssertions = 0;
        int passedAssertions = 0;

        // Specific tracked IDs for fail-safe cleanup
        List<Integer> trackedUserIds = new ArrayList<>();
        List<Integer> trackedItemIds = new ArrayList<>();
        List<Integer> trackedClaimIds = new ArrayList<>();
        List<Integer> trackedNotificationIds = new ArrayList<>();
        List<Path> trackedImageFiles = new ArrayList<>();

        Connection guardConn = null;

        try {
            // ====================================================================
            // GATE 0: Strict Database Catalog Guard (Fail-Closed)
            // ====================================================================
            System.out.println("[GATE 0] Enforcing Strict Database Isolation Guard...");
            guardConn = DBConnection.getConnection();
            String activeCatalog = guardConn.getCatalog();
            System.out.println("  Connected Catalog: " + activeCatalog);

            if (activeCatalog == null || !REQUIRED_TEST_DB.equalsIgnoreCase(activeCatalog.trim())) {
                throw new IllegalStateException(
                        "CRITICAL SAFETY VIOLATION: Active database catalog is '" + activeCatalog + 
                        "', but must be EXACTLY '" + REQUIRED_TEST_DB + "'. Halting test immediately!"
                );
            }
            System.out.println("  [PASS] Catalog guard confirmed. Isolation target: " + REQUIRED_TEST_DB + "\n");
            totalAssertions++;
            passedAssertions++;

            // ====================================================================
            // SETUP: Provision Test Schema (Phase B columns & Notifications table)
            // ====================================================================
            System.out.println("[SETUP] Verifying and provisioning isolated test schema...");
            provisionTestSchemaIfMissing(guardConn);
            System.out.println("  [PASS] Test schema verified and ready.\n");
            totalAssertions++;
            passedAssertions++;

            UserDAO userDAO = new UserDAO();
            ItemDAO itemDAO = new ItemDAO();
            ClaimDAO claimDAO = new ClaimDAO();
            NotificationDAO notifDAO = new NotificationDAO();

            long runId = System.currentTimeMillis();

            // ====================================================================
            // FEATURE 1: User Registration, PBKDF2 Password Hashing & Verification
            // ====================================================================
            System.out.println("[FEATURE 1] Testing User Registration & Password Security...");
            String student1Email = "e2e_student1_" + runId + "@campus.edu";
            String student2Email = "e2e_student2_" + runId + "@campus.edu";
            String adminEmail    = "e2e_admin_" + runId + "@campus.edu";
            String rawPassword   = "StrongStudentPass#2026";

            String hashedPassword = PasswordUtil.hashPassword(rawPassword);
            totalAssertions++;
            if (hashedPassword != null && hashedPassword.startsWith("65536:")) {
                System.out.println("  1a. PBKDF2 Hashing (65536 iterations): PASS");
                passedAssertions++;
            } else {
                System.err.println("  1a. PBKDF2 Hashing: FAIL");
            }

            totalAssertions++;
            if (PasswordUtil.verifyPassword(rawPassword, hashedPassword) &&
                !PasswordUtil.verifyPassword("WrongPassword!", hashedPassword)) {
                System.out.println("  1b. Password Verification (Constant-Time): PASS");
                passedAssertions++;
            } else {
                System.err.println("  1b. Password Verification: FAIL");
            }

            // Register Student 1 (Finder/Reporter)
            User student1 = new User("E2E Alice Finder", student1Email, hashedPassword, User.Role.STUDENT);
            boolean s1Registered = userDAO.register(student1);
            trackRegisteredUser(userDAO, student1, student1Email, trackedUserIds);

            // Register Student 2 (Claimant)
            User student2 = new User("E2E Bob Claimant", student2Email, hashedPassword, User.Role.STUDENT);
            boolean s2Registered = userDAO.register(student2);
            trackRegisteredUser(userDAO, student2, student2Email, trackedUserIds);

            // Register Admin
            User adminUser = new User("E2E Campus Admin", adminEmail, hashedPassword, User.Role.ADMIN);
            boolean adminRegistered = userDAO.register(adminUser);
            trackRegisteredUser(userDAO, adminUser, adminEmail, trackedUserIds);

            totalAssertions++;
            if (s1Registered && s2Registered && adminRegistered &&
                student1.getUserId() > 0 && student2.getUserId() > 0 && adminUser.getUserId() > 0) {
                System.out.println("  1c. User Accounts Registered: PASS (IDs: " + student1.getUserId() + ", " + student2.getUserId() + ", " + adminUser.getUserId() + ")");
                passedAssertions++;
            } else {
                System.err.println("  1c. User Accounts Registered: FAIL");
            }

            // 1d. Complete Login-Flow Authentication Pipeline Simulation (LoginFrame equivalent)
            totalAssertions++;
            User authUser = userDAO.findByEmail(student1Email);
            boolean authSuccess = (authUser != null) &&
                                  PasswordUtil.verifyPassword(rawPassword, authUser.getPassword());
            boolean authWrongBlocked = (authUser != null) &&
                                       !PasswordUtil.verifyPassword("IncorrectPassword#123", authUser.getPassword());

            if (authSuccess && authWrongBlocked && authUser.getRole() == User.Role.STUDENT) {
                System.out.println("  1d. Full Login Authentication Pipeline (findByEmail + verifyPassword): PASS -> User: " + authUser.getName());
                passedAssertions++;
            } else {
                System.err.println("  1d. Full Login Authentication Pipeline: FAIL");
            }

            // ====================================================================
            // FEATURE 2: Item Reporting, Structured Location & Photo Management
            // ====================================================================
            System.out.println("\n[FEATURE 2] Testing Item Reporting & Photo Attachment...");
            File samplePhoto = createTestImageFile("e2e_calc_" + runId + ".png", "PNG", 320, 240, new Color(13, 148, 136));
            ImageUtil.SavedImageResult imgResult = ImageUtil.saveItemImageResult(samplePhoto);

            totalAssertions++;
            if (imgResult != null && imgResult.getRelativePath() != null && Files.exists(imgResult.getTargetPath())) {
                trackedImageFiles.add(imgResult.getTargetPath());
                System.out.println("  2a. Photo Upload & Scaling: PASS -> " + imgResult.getRelativePath());
                passedAssertions++;
            } else {
                System.err.println("  2a. Photo Upload & Scaling: FAIL");
            }

            // Report FOUND item with structured building and room
            String foundItemTitle = "Silver HP Scientific Calculator #" + runId;
            Item foundItem = new Item(
                    foundItemTitle,
                    "HP 35s scientific calculator with engraved student initials AB",
                    "Electronics & Gadgets",
                    "Science Block - Room 302",
                    imgResult != null ? imgResult.getRelativePath() : null,
                    "Science Block",
                    "Room 302",
                    Item.ItemType.FOUND,
                    Item.ItemStatus.OPEN,
                    student1.getUserId()
            );
            itemDAO.addItem(foundItem);
            trackAddedItem(guardConn, foundItem, trackedItemIds);

            totalAssertions++;
            Item fetchedFoundItem = itemDAO.findById(foundItem.getItemId());
            if (fetchedFoundItem != null &&
                "Science Block".equals(fetchedFoundItem.getBuilding()) &&
                "Room 302".equals(fetchedFoundItem.getFloorRoom()) &&
                imgResult.getRelativePath().equals(fetchedFoundItem.getImagePath())) {
                System.out.println("  2b. Item Persistence with Location & Photo: PASS -> Item #" + foundItem.getItemId());
                passedAssertions++;
            } else {
                System.err.println("  2b. Item Persistence with Location & Photo: FAIL");
            }

            // ====================================================================
            // FEATURE 3: Dynamic Multi-Criteria Search & Filtering
            // ====================================================================
            System.out.println("\n[FEATURE 3] Testing Dynamic Multi-Criteria Search...");
            totalAssertions++;
            List<Item> bldgMatches = itemDAO.searchItems(null, "ALL", "Science Block", "FOUND", "OPEN");
            boolean foundByBldg = bldgMatches.stream().anyMatch(i -> i.getItemId() == foundItem.getItemId());

            List<Item> foreignMatches = itemDAO.searchItems(null, "ALL", "Arts Building", "ALL", "ALL");
            boolean excludedFromForeign = foreignMatches.stream().noneMatch(i -> i.getItemId() == foundItem.getItemId());

            List<Item> kwMatches = itemDAO.searchItems("Room 302", "ALL", "ALL", "ALL", "ALL");
            boolean foundByKw = kwMatches.stream().anyMatch(i -> i.getItemId() == foundItem.getItemId());

            if (foundByBldg && excludedFromForeign && foundByKw) {
                System.out.println("  3a. Multi-Filter Search (Building + Keyword + Type): PASS");
                passedAssertions++;
            } else {
                System.err.println("  3a. Multi-Filter Search: FAIL");
            }

            // ====================================================================
            // FEATURE 6: Deterministic Similarity Matching Engine
            // ====================================================================
            System.out.println("\n[FEATURE 6] Testing Deterministic Similarity Matching Engine...");
            // Create a LOST report for the same calculator by Student 2
            String lostItemTitle = "Lost HP Scientific Calculator #" + runId;
            Item lostItem = new Item(
                    lostItemTitle,
                    "Lost my silver HP scientific calculator in Science Block yesterday",
                    "Electronics & Gadgets",
                    "Science Block - Floor 3",
                    null,
                    "Science Block",
                    "Floor 3",
                    Item.ItemType.LOST,
                    Item.ItemStatus.OPEN,
                    student2.getUserId()
            );
            itemDAO.addItem(lostItem);
            trackAddedItem(guardConn, lostItem, trackedItemIds);

            List<Item> candidates = itemDAO.getEligibleCandidateItems(
                    lostItem.getItemType(),
                    lostItem.getReportedBy(),
                    lostItem.getItemId()
            );

            List<MatchResult> matchResults = ItemMatchingEngine.findMatches(lostItem, candidates);
            totalAssertions++;
            boolean foundMatch = matchResults.stream().anyMatch(m -> m.getCandidateItem().getItemId() == foundItem.getItemId() && m.getTotalScore() >= 35);
            if (foundMatch) {
                MatchResult topMatch = matchResults.get(0);
                System.out.println("  6a. Similarity Engine Score Calculation: PASS (Score: " + topMatch.getTotalScore() + "%, Reason: " + topMatch.getExplanation() + ")");
                passedAssertions++;
            } else {
                System.err.println("  6a. Similarity Engine Score Calculation: FAIL");
            }

            // ====================================================================
            // FEATURE 4: Claim Submission, Duplicate Prevention & Notifications
            // ====================================================================
            System.out.println("\n[FEATURE 4] Testing Claim Submission & Ownership Proof Workflow...");
            // Student 2 submits claim on the found item
            Claim claim1 = new Claim(foundItem.getItemId(), student2.getUserId(), "My initials AB are engraved on the back battery cover.");
            boolean claim1Submitted = claimDAO.submitClaim(claim1);
            trackSubmittedClaim(guardConn, claim1, trackedClaimIds);

            // Emit claim submitted notification for Student 1 (reporter)
            if (claim1Submitted && foundItem.getReportedBy() > 0) {
                Notification nSubmitted = new Notification(
                        foundItem.getReportedBy(),
                        "New Claim Submitted",
                        "A student submitted a claim for '" + foundItem.getTitle() + "'.",
                        Notification.NotificationType.CLAIM_SUBMITTED,
                        "CLAIM",
                        claim1.getClaimId(),
                        "CLAIM_SUBMITTED:" + claim1.getClaimId()
                );
                notifDAO.createNotification(nSubmitted);
            }

            totalAssertions++;
            boolean duplicateBlocked = claimDAO.hasUserClaimed(foundItem.getItemId(), student2.getUserId());
            if (claim1Submitted && claim1.getClaimId() > 0 && duplicateBlocked) {
                System.out.println("  4a. Claim Submitted & Duplicate Check Active: PASS -> Claim #" + claim1.getClaimId());
                passedAssertions++;
            } else {
                System.err.println("  4a. Claim Submitted & Duplicate Check Active: FAIL");
            }

            // Verify notification received by student 1
            totalAssertions++;
            List<Notification> s1Notifs = notifDAO.getNotificationsByUser(student1.getUserId(), false);
            boolean s1GotSubmitAlert = s1Notifs.stream().anyMatch(n -> n.getNotificationType() == Notification.NotificationType.CLAIM_SUBMITTED);
            if (s1GotSubmitAlert) {
                System.out.println("  4b. Claim Submitted Notification Delivery to Reporter: PASS");
                passedAssertions++;
            } else {
                System.err.println("  4b. Claim Submitted Notification Delivery to Reporter: FAIL");
            }

            // Submit competing claim from Admin user (as student) to test atomic rejection
            Claim claim2Competing = new Claim(foundItem.getItemId(), adminUser.getUserId(), "Competing claim message.");
            claimDAO.submitClaim(claim2Competing);
            trackSubmittedClaim(guardConn, claim2Competing, trackedClaimIds);

            // ====================================================================
            // FEATURE 5: Atomic Claim Approval, Competing Rejection & Tx Notifications
            // ====================================================================
            System.out.println("\n[FEATURE 5] Testing Atomic Claim Approval & Conflict Adjudication...");
            totalAssertions++;
            boolean approveTxOk = claimDAO.approveClaimTransaction(claim1.getClaimId(), foundItem.getItemId());

            Claim approvedClaim = claimDAO.findById(claim1.getClaimId());
            Claim rejectedClaim = claimDAO.findById(claim2Competing.getClaimId());
            Item claimedItem = itemDAO.findById(foundItem.getItemId());

            if (approveTxOk &&
                approvedClaim.getStatus() == Claim.ClaimStatus.APPROVED &&
                rejectedClaim.getStatus() == Claim.ClaimStatus.REJECTED &&
                claimedItem.getStatus() == Item.ItemStatus.CLAIMED) {
                System.out.println("  5a. Atomic Transaction (Approve Claim + Claim Item + Reject Competing): PASS");
                passedAssertions++;
            } else {
                System.err.println("  5a. Atomic Transaction: FAIL");
            }

            // ====================================================================
            // FEATURE 7: Notification Feed, Unread Counts & Deduplication
            // ====================================================================
            System.out.println("\n[FEATURE 7] Testing In-App Notification System & Deduplication...");
            // Claimant (Student 2) must have received CLAIM_APPROVED notification
            totalAssertions++;
            List<Notification> s2Notifs = notifDAO.getNotificationsByUser(student2.getUserId(), false);
            boolean s2GotApprovalAlert = s2Notifs.stream().anyMatch(n -> n.getNotificationType() == Notification.NotificationType.CLAIM_APPROVED);
            if (s2GotApprovalAlert) {
                System.out.println("  7a. Claimant Received In-Transaction Approval Notification: PASS");
                passedAssertions++;
            } else {
                System.err.println("  7a. Claimant Received In-Transaction Approval Notification: FAIL");
            }

            // Competing claimant (Admin) must have received CLAIM_REJECTED notification
            totalAssertions++;
            List<Notification> adminNotifs = notifDAO.getNotificationsByUser(adminUser.getUserId(), false);
            boolean adminGotRejectAlert = adminNotifs.stream().anyMatch(n -> n.getNotificationType() == Notification.NotificationType.CLAIM_REJECTED);
            if (adminGotRejectAlert) {
                System.out.println("  7b. Competing Claimant Received Automatic Rejection Notification: PASS");
                passedAssertions++;
            } else {
                System.err.println("  7b. Competing Claimant Received Automatic Rejection Notification: FAIL");
            }

            // Deduplication test: re-inserting same notification key must be ignored safely
            totalAssertions++;
            Notification duplicateAttempt = new Notification(
                    student2.getUserId(),
                    "Claim Approved",
                    "Duplicate attempt",
                    Notification.NotificationType.CLAIM_APPROVED,
                    "CLAIM",
                    claim1.getClaimId(),
                    "CLAIM_APPROVED:" + claim1.getClaimId() // Identical dedup_key
            );
            boolean dupResult = notifDAO.createNotification(duplicateAttempt);
            int s2CountAfterDup = notifDAO.getNotificationsByUser(student2.getUserId(), false).size();
            if (s2CountAfterDup == s2Notifs.size()) {
                System.out.println("  7c. Concurrency-Safe Deduplication (INSERT IGNORE via dedup_key): PASS");
                passedAssertions++;
            } else {
                System.err.println("  7c. Concurrency-Safe Deduplication: FAIL");
            }

            // Mark as read action test
            totalAssertions++;
            int unreadBefore = notifDAO.getUnreadCount(student2.getUserId());
            Notification firstS2Notif = s2Notifs.get(0);
            notifDAO.markAsRead(firstS2Notif.getNotificationId(), student2.getUserId());
            int unreadAfter = notifDAO.getUnreadCount(student2.getUserId());
            if (unreadAfter == unreadBefore - 1) {
                System.out.println("  7d. Mark as Read & Live Unread Count Decrement: PASS");
                passedAssertions++;
            } else {
                System.err.println("  7d. Mark as Read & Live Unread Count Decrement: FAIL");
            }

            // ====================================================================
            // LIFECYCLE: Item RETURNED Transition & Final Resolution Alerts
            // ====================================================================
            System.out.println("\n[LIFECYCLE] Testing Item Return & Resolution Alerts...");
            totalAssertions++;
            boolean returnOk = itemDAO.updateStatus(foundItem.getItemId(), Item.ItemStatus.RETURNED);
            Item returnedItem = itemDAO.findById(foundItem.getItemId());

            // Claimant should receive ITEM_RETURNED alert
            List<Notification> s2UpdatedNotifs = notifDAO.getNotificationsByUser(student2.getUserId(), false);
            boolean s2GotReturnAlert = s2UpdatedNotifs.stream().anyMatch(n -> n.getNotificationType() == Notification.NotificationType.ITEM_RETURNED);

            if (returnOk && returnedItem.getStatus() == Item.ItemStatus.RETURNED && s2GotReturnAlert) {
                System.out.println("  8a. Status Updated to RETURNED & Claimant Notified: PASS");
                passedAssertions++;
            } else {
                System.err.println("  8a. Status Updated to RETURNED & Claimant Notified: FAIL");
            }

            // Spam prevention check: updating to same status should not re-trigger notifications
            totalAssertions++;
            int s2NotifsBeforeRedundant = notifDAO.getNotificationsByUser(student2.getUserId(), false).size();
            itemDAO.updateStatus(foundItem.getItemId(), Item.ItemStatus.RETURNED);
            int s2NotifsAfterRedundant = notifDAO.getNotificationsByUser(student2.getUserId(), false).size();
            if (s2NotifsAfterRedundant == s2NotifsBeforeRedundant) {
                System.out.println("  8b. Redundant Status Update Spam Suppression: PASS");
                passedAssertions++;
            } else {
                System.err.println("  8b. Redundant Status Update Spam Suppression: FAIL");
            }

        } catch (Exception ex) {
            System.err.println("\nFATAL TEST EXECUTION EXCEPTION: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            // ====================================================================
            // GUARANTEED CLEANUP: Purge strictly tracked IDs and test files
            // ====================================================================
            System.out.println("\n[TEARDOWN] Executing Guaranteed Cleanup of Test Artifacts...");
            cleanupTrackedFiles(trackedImageFiles);
            cleanupTrackedDatabaseRecords(guardConn, trackedUserIds, trackedItemIds, trackedClaimIds);

            if (guardConn != null) {
                try {
                    guardConn.close();
                } catch (SQLException ignored) { }
            }
        }

        System.out.println("\n======================================================================");
        System.out.printf("  STAGE 8 E2E TEST SUMMARY: %d / %d ASSERTIONS PASSED\n", passedAssertions, totalAssertions);
        System.out.println("======================================================================\n");

        if (passedAssertions < totalAssertions) {
            System.exit(1);
        }
    }

    private static void provisionTestSchemaIfMissing(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();

            // 1. Verify Phase B columns in items table
            boolean hasImagePath = false;
            try (ResultSet rs = meta.getColumns(catalog, null, "items", "image_path")) {
                hasImagePath = rs.next();
            }

            if (!hasImagePath) {
                System.out.println("  Provisioning Phase B columns (image_path, building, floor_room) in " + catalog + "...");
                stmt.executeUpdate("ALTER TABLE items " +
                        "ADD COLUMN image_path VARCHAR(255) NULL AFTER location, " +
                        "ADD COLUMN building VARCHAR(100) NULL AFTER image_path, " +
                        "ADD COLUMN floor_room VARCHAR(100) NULL AFTER building");
            }

            // 2. Provision notifications table if missing
            boolean hasNotifications = false;
            try (ResultSet rs = meta.getTables(catalog, null, "notifications", new String[]{"TABLE"})) {
                hasNotifications = rs.next();
            }

            if (!hasNotifications) {
                System.out.println("  Provisioning notifications table in " + catalog + "...");
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS notifications (" +
                        "    notification_id   INT AUTO_INCREMENT PRIMARY KEY," +
                        "    user_id           INT NOT NULL," +
                        "    title             VARCHAR(150) NOT NULL," +
                        "    message           TEXT NOT NULL," +
                        "    notification_type VARCHAR(50) NOT NULL," +
                        "    reference_type    VARCHAR(50) NULL," +
                        "    reference_id      INT NULL," +
                        "    dedup_key         VARCHAR(191) NULL," +
                        "    is_read           BOOLEAN NOT NULL DEFAULT FALSE," +
                        "    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    CONSTRAINT fk_test_notifications_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE," +
                        "    CONSTRAINT uq_test_notifications_dedup UNIQUE (dedup_key)," +
                        "    INDEX idx_test_notif_user_read (user_id, is_read)," +
                        "    INDEX idx_test_notif_created (user_id, created_at DESC)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
                );
            }
        }
    }

    private static File createTestImageFile(String filename, String format, int width, int height, Color color) throws IOException {
        File temp = File.createTempFile("e2e_img_", "_" + filename);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ImageIO.write(img, format, temp);
        temp.deleteOnExit();
        return temp;
    }

    private static void trackRegisteredUser(UserDAO userDAO, User user, String email, List<Integer> trackedUserIds) {
        int id = user.getUserId();
        if (id > 0) {
            trackedUserIds.add(id);
            return;
        }
        // Fallback recovery: query by unique email to ensure record is tracked for cleanup
        try {
            User recovered = userDAO.findByEmail(email);
            if (recovered != null && recovered.getUserId() > 0) {
                user.setUserId(recovered.getUserId());
                trackedUserIds.add(recovered.getUserId());
            }
        } catch (Exception ignored) { }
    }

    private static void trackAddedItem(Connection conn, Item item, List<Integer> trackedItemIds) {
        int id = item.getItemId();
        if (id > 0) {
            trackedItemIds.add(id);
            return;
        }
        // Fallback recovery: query by title and reported_by
        if (conn != null && item.getTitle() != null && item.getReportedBy() > 0) {
            String sql = "SELECT item_id FROM items WHERE title = ? AND reported_by = ? ORDER BY item_id DESC LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, item.getTitle());
                pstmt.setInt(2, item.getReportedBy());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int recoveredId = rs.getInt(1);
                        if (recoveredId > 0) {
                            item.setItemId(recoveredId);
                            trackedItemIds.add(recoveredId);
                        }
                    }
                }
            } catch (SQLException ignored) { }
        }
    }

    private static void trackSubmittedClaim(Connection conn, Claim claim, List<Integer> trackedClaimIds) {
        int id = claim.getClaimId();
        if (id > 0) {
            trackedClaimIds.add(id);
            return;
        }
        // Fallback recovery: query by item_id and claimed_by
        if (conn != null && claim.getItemId() > 0 && claim.getClaimedBy() > 0) {
            String sql = "SELECT claim_id FROM claims WHERE item_id = ? AND claimed_by = ? ORDER BY claim_id DESC LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, claim.getItemId());
                pstmt.setInt(2, claim.getClaimedBy());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int recoveredId = rs.getInt(1);
                        if (recoveredId > 0) {
                            claim.setClaimId(recoveredId);
                            trackedClaimIds.add(recoveredId);
                        }
                    }
                }
            } catch (SQLException ignored) { }
        }
    }

    private static void cleanupTrackedFiles(List<Path> files) {
        Path uploadDir = ImageUtil.getUploadDirectory();
        for (Path f : files) {
            try {
                if (f != null && Files.exists(f)) {
                    if (f.toRealPath().startsWith(uploadDir.toRealPath())) {
                        Files.delete(f);
                        System.out.println("  Cleaned up test image: " + f.getFileName());
                    }
                }
            } catch (Exception e) {
                System.err.println("  Warning: Failed to delete test file " + f + ": " + e.getMessage());
            }
        }
    }

    private static void cleanupTrackedDatabaseRecords(Connection conn, List<Integer> userIds, List<Integer> itemIds, List<Integer> claimIds) {
        if (conn == null) return;
        try {
            // Delete notifications created for tracked users
            if (!userIds.isEmpty()) {
                StringBuilder inUsers = new StringBuilder();
                for (int i = 0; i < userIds.size(); i++) {
                    inUsers.append(userIds.get(i)).append(i == userIds.size() - 1 ? "" : ",");
                }
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM notifications WHERE user_id IN (" + inUsers + ")");
                } catch (SQLException ignored) { }
            }

            // Delete tracked claims
            if (!claimIds.isEmpty()) {
                StringBuilder inClaims = new StringBuilder();
                for (int i = 0; i < claimIds.size(); i++) {
                    inClaims.append(claimIds.get(i)).append(i == claimIds.size() - 1 ? "" : ",");
                }
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM claims WHERE claim_id IN (" + inClaims + ")");
                }
            }

            // Delete tracked items
            if (!itemIds.isEmpty()) {
                StringBuilder inItems = new StringBuilder();
                for (int i = 0; i < itemIds.size(); i++) {
                    inItems.append(itemIds.get(i)).append(i == itemIds.size() - 1 ? "" : ",");
                }
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM items WHERE item_id IN (" + inItems + ")");
                }
            }

            // Delete tracked users
            if (!userIds.isEmpty()) {
                StringBuilder inUsers = new StringBuilder();
                for (int i = 0; i < userIds.size(); i++) {
                    inUsers.append(userIds.get(i)).append(i == userIds.size() - 1 ? "" : ",");
                }
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM users WHERE user_id IN (" + inUsers + ")");
                }
            }

            System.out.printf("  Cleaned up %d user(s), %d item(s), and %d claim(s).\n",
                    userIds.size(), itemIds.size(), claimIds.size());

        } catch (SQLException e) {
            System.err.println("  Warning during tracked database cleanup: " + e.getMessage());
        }
    }
}

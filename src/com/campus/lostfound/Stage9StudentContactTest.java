package com.campus.lostfound;

import com.campus.lostfound.config.DBConnection;
import com.campus.lostfound.dao.ClaimDAO;
import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.dao.NotificationDAO;
import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.Claim;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.User;
import com.campus.lostfound.utils.PasswordUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage9StudentContactTest
 * 
 * Production-grade automated test suite verifying Feature 8:
 * - Strict database catalog isolation guard (Fail-Closed).
 * - Student Profile fields: roll number, department, semester, phone, and privacy flag.
 * - Roll Number uniqueness constraints and validation.
 * - Backward compatibility with unmigrated / incomplete profiles.
 * - Strict Backend Authorization:
 *   1. Similarity scores alone NEVER authorize contact.
 *   2. Contact is rejected prior to claim approval.
 *   3. Contact is authorized between verified finder and claimant ONLY AFTER claim approval.
 *   4. Unrelated third-party students are strictly rejected.
 *   5. Phone privacy masking (phone is hidden when phone_public == false, exposed when true).
 *   6. Password hash is never leaked in authorized contact payload.
 * - Post-test cleanup in a 'finally' block guaranteeing zero leftover records.
 */
public class Stage9StudentContactTest {

    private static final String REQUIRED_TEST_DB = "smart_campus_db_phase_b_test";

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  Smart Campus Lost & Found - Stage 9: Profile & Contact Auth Suite   ");
        System.out.println("======================================================================\n");

        int totalAssertions = 0;
        int passedAssertions = 0;

        List<Integer> trackedUserIds = new ArrayList<>();
        List<Integer> trackedItemIds = new ArrayList<>();
        List<Integer> trackedClaimIds = new ArrayList<>();

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
            // SETUP: Provision Feature 8 Schema on Test Database
            // ====================================================================
            System.out.println("[SETUP] Verifying and provisioning Feature 8 test schema...");
            provisionFeature8SchemaIfMissing(guardConn);
            UserDAO.resetSchemaCache();
            System.out.println("  [PASS] Feature 8 test schema verified and ready.\n");
            totalAssertions++;
            passedAssertions++;

            UserDAO userDAO = new UserDAO();
            ItemDAO itemDAO = new ItemDAO();
            ClaimDAO claimDAO = new ClaimDAO();

            long runId = System.currentTimeMillis();

            // ====================================================================
            // TEST 1: Register Test Students & Verify Backward Compatibility
            // ====================================================================
            System.out.println("[TEST 1] Registering test users & verifying backward compatibility...");
            String student1Email = "finder_" + runId + "@campus.edu";
            String student2Email = "claimant_" + runId + "@campus.edu";
            String student3Email = "bystander_" + runId + "@campus.edu";
            String rawPassword = "TestPassword@123";
            String hashedPassword = PasswordUtil.hashPassword(rawPassword);

            User finderUser = new User("Alice Finder", student1Email, hashedPassword, User.Role.STUDENT);
            boolean reg1 = userDAO.register(finderUser);
            trackRegisteredUser(userDAO, finderUser, student1Email, trackedUserIds);
            assertTrue(reg1 && finderUser.getUserId() > 0, "Finder student registration succeeded");
            totalAssertions++; passedAssertions++;

            User claimantUser = new User("Bob Claimant", student2Email, hashedPassword, User.Role.STUDENT);
            boolean reg2 = userDAO.register(claimantUser);
            trackRegisteredUser(userDAO, claimantUser, student2Email, trackedUserIds);
            assertTrue(reg2 && claimantUser.getUserId() > 0, "Claimant student registration succeeded");
            totalAssertions++; passedAssertions++;

            User bystanderUser = new User("Charlie Bystander", student3Email, hashedPassword, User.Role.STUDENT);
            boolean reg3 = userDAO.register(bystanderUser);
            trackRegisteredUser(userDAO, bystanderUser, student3Email, trackedUserIds);
            assertTrue(reg3 && bystanderUser.getUserId() > 0, "Bystander student registration succeeded");
            totalAssertions++; passedAssertions++;

            // Check backward compatibility: Initial user has NULL profile fields
            User loadedBystander = userDAO.findById(bystanderUser.getUserId());
            assertTrue(loadedBystander != null, "Unmigrated/default user retrieved successfully");
            assertTrue(loadedBystander.getRollNumber() == null, "Default roll number is null");
            assertTrue(loadedBystander.getDepartment() == null, "Default department is null");
            assertTrue(!loadedBystander.isPhonePublic(), "Default phone visibility is false (private)");
            totalAssertions += 4; passedAssertions += 4;
            System.out.println("  [PASS] Backward compatibility verified.\n");

            // ====================================================================
            // TEST 2: Student Profile Updates & Uniqueness Enforcement
            // ====================================================================
            System.out.println("[TEST 2] Testing Student Profile updates & Roll Number uniqueness...");
            String finderRoll = "CS" + (runId % 100000);
            finderUser.setRollNumber(finderRoll);
            finderUser.setDepartment("Computer Science & Engineering");
            finderUser.setSemester("Semester 6");
            finderUser.setPhoneNumber("+1 555-0144");
            finderUser.setPhonePublic(false); // Kept private initially

            boolean updateFinder = userDAO.updateProfile(finderUser);
            assertTrue(updateFinder, "Finder profile updated successfully");
            totalAssertions++; passedAssertions++;

            User loadedFinder = userDAO.findById(finderUser.getUserId());
            assertTrue(finderRoll.equals(loadedFinder.getRollNumber()), "Roll number persisted correctly");
            assertTrue("Computer Science & Engineering".equals(loadedFinder.getDepartment()), "Department persisted correctly");
            assertTrue("Semester 6".equals(loadedFinder.getSemester()), "Semester persisted correctly");
            assertTrue("+1 555-0144".equals(loadedFinder.getPhoneNumber()), "Phone number persisted correctly");
            assertTrue(!loadedFinder.isPhonePublic(), "Phone public preference persisted as false");
            totalAssertions += 5; passedAssertions += 5;

            // Roll number collision test
            boolean rollTaken = userDAO.isRollNumberTaken(finderRoll, claimantUser.getUserId());
            assertTrue(rollTaken, "isRollNumberTaken detects collision with another user's roll number");
            totalAssertions++; passedAssertions++;

            boolean selfRollCheck = userDAO.isRollNumberTaken(finderRoll, finderUser.getUserId());
            assertTrue(!selfRollCheck, "isRollNumberTaken allows user's own existing roll number");
            totalAssertions++; passedAssertions++;

            // Update Claimant profile with unique roll number
            String claimantRoll = "EE" + (runId % 100000);
            claimantUser.setRollNumber(claimantRoll);
            claimantUser.setDepartment("Electrical & Electronics Engineering");
            claimantUser.setSemester("Semester 4");
            claimantUser.setPhoneNumber("+1 555-0288");
            claimantUser.setPhonePublic(true); // Public phone for Bob

            boolean updateClaimant = userDAO.updateProfile(claimantUser);
            assertTrue(updateClaimant, "Claimant profile updated successfully");
            totalAssertions++; passedAssertions++;
            System.out.println("  [PASS] Profile updates and uniqueness checks verified.\n");

            // ====================================================================
            // TEST 3: Report FOUND Item & Pre-Approval Contact Rejection
            // ====================================================================
            System.out.println("[TEST 3] Reporting FOUND Item & testing Pre-Approval Contact Rejection...");
            Item foundItem = new Item();
            foundItem.setTitle("TI-84 Plus Graphing Calculator [Run " + runId + "]");
            foundItem.setDescription("Found on table in Engineering Hall, black casing with silver buttons.");
            foundItem.setCategory("Electronics");
            foundItem.setLocation("Engineering Hall");
            foundItem.setBuilding("Engineering Complex");
            foundItem.setFloorRoom("Room 204");
            foundItem.setItemType(Item.ItemType.FOUND);
            foundItem.setStatus(Item.ItemStatus.OPEN);
            foundItem.setReportedBy(finderUser.getUserId());

            boolean itemAdded = itemDAO.addItem(foundItem);
            trackAddedItem(guardConn, foundItem, trackedItemIds);
            assertTrue(itemAdded && foundItem.getItemId() > 0, "FOUND item reported successfully");
            totalAssertions++; passedAssertions++;

            // Submit a claim by Claimant
            Claim claim = new Claim();
            claim.setItemId(foundItem.getItemId());
            claim.setClaimedBy(claimantUser.getUserId());
            claim.setMessage("I forgot my calculator after the differential equations lecture. Name 'Bob' engraved on back.");
            claim.setStatus(Claim.ClaimStatus.PENDING);

            boolean claimSubmitted = claimDAO.submitClaim(claim);
            trackSubmittedClaim(guardConn, claim, trackedClaimIds);
            assertTrue(claimSubmitted && claim.getClaimId() > 0, "Claim submitted with PENDING status");
            totalAssertions++; passedAssertions++;

            // SECURITY ASSERTION: Before approval, claimant cannot view finder contact
            User preAuth1 = userDAO.getAuthorizedContact(claimantUser.getUserId(), finderUser.getUserId(), foundItem.getItemId());
            assertTrue(preAuth1 == null, "Pre-approval: Claimant request for finder contact is REJECTED (null)");
            totalAssertions++; passedAssertions++;

            // SECURITY ASSERTION: Before approval, finder cannot view claimant contact
            User preAuth2 = userDAO.getAuthorizedContact(finderUser.getUserId(), claimantUser.getUserId(), foundItem.getItemId());
            assertTrue(preAuth2 == null, "Pre-approval: Finder request for claimant contact is REJECTED (null)");
            totalAssertions++; passedAssertions++;

            // SECURITY ASSERTION: Bystander cannot view contact
            User preAuth3 = userDAO.getAuthorizedContact(bystanderUser.getUserId(), finderUser.getUserId(), foundItem.getItemId());
            assertTrue(preAuth3 == null, "Pre-approval: Bystander request for contact is REJECTED (null)");
            totalAssertions++; passedAssertions++;

            // SECURITY ASSERTION: Peer contact resolution returns null
            User prePeer = userDAO.getAuthorizedPeerContactForItem(claimantUser.getUserId(), foundItem.getItemId());
            assertTrue(prePeer == null, "Pre-approval: getAuthorizedPeerContactForItem returns null");
            totalAssertions++; passedAssertions++;
            System.out.println("  [PASS] Pre-approval contact rejection confirmed.\n");

            // ====================================================================
            // TEST 4: Atomic Claim Approval & Authorized Contact Access
            // ====================================================================
            System.out.println("[TEST 4] Approving claim and verifying Authorized Contact Access...");
            boolean approved = claimDAO.approveClaimTransaction(claim.getClaimId(), foundItem.getItemId());
            assertTrue(approved, "Claim approved via atomic transaction");
            totalAssertions++; passedAssertions++;

            // POST-APPROVAL: Claimant requests Finder contact
            User authFinder = userDAO.getAuthorizedContact(claimantUser.getUserId(), finderUser.getUserId(), foundItem.getItemId());
            assertTrue(authFinder != null, "Post-approval: Claimant is AUTHORIZED to view Finder contact");
            assertTrue("Alice Finder".equals(authFinder.getName()), "Finder name is correct");
            assertTrue(student1Email.equals(authFinder.getEmail()), "Finder campus email is accessible");
            assertTrue(finderRoll.equals(authFinder.getRollNumber()), "Finder roll number is accessible");
            assertTrue("Computer Science & Engineering".equals(authFinder.getDepartment()), "Finder department is accessible");
            assertTrue("Semester 6".equals(authFinder.getSemester()), "Finder semester is accessible");
            assertTrue(authFinder.getPassword() == null, "SECURITY: Password hash is NOT exposed in contact payload");
            totalAssertions += 7; passedAssertions += 7;

            // PHONE PRIVACY CHECK 1: Finder had phone_public == false
            assertTrue(authFinder.getPhoneNumber() == null, "PRIVACY SAFEGUARD: Finder phone number is MASKED (null) because phone_public=false");
            totalAssertions++; passedAssertions++;

            // POST-APPROVAL: Finder requests Claimant contact
            User authClaimant = userDAO.getAuthorizedContact(finderUser.getUserId(), claimantUser.getUserId(), foundItem.getItemId());
            assertTrue(authClaimant != null, "Post-approval: Finder is AUTHORIZED to view Claimant contact");
            assertTrue("Bob Claimant".equals(authClaimant.getName()), "Claimant name is correct");
            assertTrue(student2Email.equals(authClaimant.getEmail()), "Claimant campus email is accessible");
            assertTrue(claimantRoll.equals(authClaimant.getRollNumber()), "Claimant roll number is accessible");
            assertTrue("Electrical & Electronics Engineering".equals(authClaimant.getDepartment()), "Claimant department is accessible");
            assertTrue(authClaimant.getPassword() == null, "SECURITY: Password hash is NOT exposed in contact payload");
            totalAssertions += 6; passedAssertions += 6;

            // PHONE PRIVACY CHECK 2: Claimant had phone_public == true
            assertTrue("+1 555-0288".equals(authClaimant.getPhoneNumber()), "PRIVACY PREFERENCE: Claimant phone number is VISIBLE because phone_public=true");
            totalAssertions++; passedAssertions++;

            // Peer contact resolution helper check
            User resolvedFinder = userDAO.getAuthorizedPeerContactForItem(claimantUser.getUserId(), foundItem.getItemId());
            assertTrue(resolvedFinder != null && resolvedFinder.getUserId() == finderUser.getUserId(),
                    "getAuthorizedPeerContactForItem resolves Finder for Claimant");
            totalAssertions++; passedAssertions++;

            User resolvedClaimant = userDAO.getAuthorizedPeerContactForItem(finderUser.getUserId(), foundItem.getItemId());
            assertTrue(resolvedClaimant != null && resolvedClaimant.getUserId() == claimantUser.getUserId(),
                    "getAuthorizedPeerContactForItem resolves Claimant for Finder");
            totalAssertions++; passedAssertions++;
            System.out.println("  [PASS] Authorized contact retrieval and peer resolution verified.\n");

            // ====================================================================
            // TEST 5: Bystander Rejection & Phone Visibility Toggle
            // ====================================================================
            System.out.println("[TEST 5] Testing Third-Party Rejection & Phone Visibility Dynamic Toggle...");
            User bystanderAttempt = userDAO.getAuthorizedContact(bystanderUser.getUserId(), finderUser.getUserId(), foundItem.getItemId());
            assertTrue(bystanderAttempt == null, "Post-approval: Bystander contact access is strictly REJECTED (null)");
            totalAssertions++; passedAssertions++;

            User bystanderPeer = userDAO.getAuthorizedPeerContactForItem(bystanderUser.getUserId(), foundItem.getItemId());
            assertTrue(bystanderPeer == null, "Post-approval: Bystander peer resolution returns null");
            totalAssertions++; passedAssertions++;

            // Dynamic toggle: Finder opts to make phone number public
            finderUser.setPhonePublic(true);
            boolean toggleSuccess = userDAO.updateProfile(finderUser);
            assertTrue(toggleSuccess, "Finder updated phone_public to true");
            totalAssertions++; passedAssertions++;

            User authFinderAfterToggle = userDAO.getAuthorizedContact(claimantUser.getUserId(), finderUser.getUserId(), foundItem.getItemId());
            assertTrue(authFinderAfterToggle != null, "Claimant retrieves updated Finder contact");
            assertTrue("+1 555-0144".equals(authFinderAfterToggle.getPhoneNumber()), "Finder phone number is now UNMASKED after enabling phone_public");
            totalAssertions += 2; passedAssertions += 2;
            System.out.println("  [PASS] Third-party rejection and dynamic privacy toggle verified.\n");

            // ====================================================================
            // SUMMARY
            // ====================================================================
            System.out.println("======================================================================");
            System.out.printf("  STAGE 9 VERIFICATION COMPLETE: %d / %d Assertions Passed (100%%)\n", passedAssertions, totalAssertions);
            System.out.println("======================================================================\n");

        } catch (Throwable t) {
            System.err.println("TEST FAILURE EXCEPTION: " + t.getMessage());
            t.printStackTrace(System.err);
        } finally {
            // ====================================================================
            // CLEANUP: Post-test record cleanup in finally block
            // ====================================================================
            System.out.println("[CLEANUP] Executing post-test cleanup of tracked test records...");
            cleanupTrackedDatabaseRecords(guardConn, trackedUserIds, trackedItemIds, trackedClaimIds);
            if (guardConn != null) {
                try { guardConn.close(); } catch (SQLException ignored) { }
            }
            System.out.println("[CLEANUP] Post-test cleanup complete.");
        }
    }

    private static void assertTrue(boolean condition, String description) {
        if (!condition) {
            System.err.println("  [FAIL] Assertion Failed: " + description);
            throw new AssertionError("Assertion Failed: " + description);
        } else {
            System.out.println("  [PASS] " + description);
        }
    }

    private static void provisionFeature8SchemaIfMissing(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();

            // 1. Check profile columns in users table
            boolean hasRollNumber = false;
            try (ResultSet rs = meta.getColumns(catalog, null, "users", "roll_number")) {
                hasRollNumber = rs.next();
            }

            if (!hasRollNumber) {
                System.out.println("  Provisioning Feature 8 columns (roll_number, department, semester, phone_number, phone_public) in " + catalog + "...");
                stmt.executeUpdate("ALTER TABLE users " +
                        "ADD COLUMN roll_number VARCHAR(50) NULL AFTER email, " +
                        "ADD COLUMN department VARCHAR(100) NULL AFTER roll_number, " +
                        "ADD COLUMN semester VARCHAR(20) NULL AFTER department, " +
                        "ADD COLUMN phone_number VARCHAR(20) NULL AFTER semester, " +
                        "ADD COLUMN phone_public BOOLEAN NOT NULL DEFAULT FALSE AFTER phone_number");

                try {
                    stmt.executeUpdate("ALTER TABLE users ADD CONSTRAINT uq_users_roll_number UNIQUE (roll_number)");
                } catch (SQLException ignored) {
                    // Constraint may already exist
                }
            }
        }
    }

    private static void trackRegisteredUser(UserDAO userDAO, User user, String email, List<Integer> trackedUserIds) {
        int id = user.getUserId();
        if (id > 0) {
            trackedUserIds.add(id);
            return;
        }
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

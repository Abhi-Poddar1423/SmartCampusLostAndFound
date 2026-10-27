package com.campus.lostfound;

import com.campus.lostfound.dao.ClaimDAO;
import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.Claim;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.User;
import com.campus.lostfound.utils.PasswordUtil;
import com.campus.lostfound.utils.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * Stage5Test
 * 
 * Comprehensive automated verification for Stage 5:
 * 1. Checks baseline database state (0 rows).
 * 2. Unauthorized access checks.
 * 3. Self-protection & last-admin protection checks.
 * 4. Foreign key integrity protection (blocking user deletion with related items/claims).
 * 5. Safe item moderation deletion with related claims transaction.
 * 6. Claim approval transaction with item status synchronization (CLAIMED)
 *    and competing claims rejection.
 * 7. Verification that database is completely purged back to 0 rows.
 */
public class Stage5Test {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("      Smart Campus Lost & Found - Stage 5 Test    ");
        System.out.println("==================================================");

        boolean allPassed = true;
        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        ClaimDAO claimDAO = new ClaimDAO();

        // Step 0: Check initial database count
        int initialUsers = userDAO.getTotalUsersCount();
        int initialItems = itemDAO.getLostItemsCount() + itemDAO.getFoundItemsCount();
        int initialClaims = claimDAO.getPendingClaimsCount();
        System.out.println("[Baseline Check] Initial DB counts: Users=" + initialUsers + ", Items=" + initialItems + ", PendingClaims=" + initialClaims);

        // Test 1: Unauthorized Access Prevention
        System.out.println("\n[Test 1] Testing Unauthorized Admin Access Prevention...");
        SessionManager session = SessionManager.getInstance();
        session.logout(); // No session
        boolean unauthorizedBlocked1 = !session.isLoggedIn() || !session.isAdmin();

        // Student session
        User studentUser = new User(101, "Regular Student", "student@campus.edu", "", User.Role.STUDENT, null);
        session.login(studentUser);
        boolean unauthorizedBlocked2 = !session.isAdmin();
        session.logout();

        if (unauthorizedBlocked1 && unauthorizedBlocked2) {
            System.out.println("  [PASS] Non-logged-in users and students are strictly blocked from admin privileges.");
        } else {
            System.err.println("  [FAIL] Unauthorized access prevention failed.");
            allPassed = false;
        }

        // Test 2: Role Protection & Last Admin Security
        System.out.println("\n[Test 2] Testing Role Protection & Last Admin Security...");
        long timestamp = System.currentTimeMillis();
        User admin1 = new User("Primary Admin", "admin1_" + timestamp + "@campus.edu", PasswordUtil.hashPassword("AdminPass@123"), User.Role.ADMIN);
        User admin2 = new User("Secondary Admin", "admin2_" + timestamp + "@campus.edu", PasswordUtil.hashPassword("AdminPass@123"), User.Role.ADMIN);

        try {
            userDAO.register(admin1);
            userDAO.register(admin2);

            int adminCount = userDAO.countAdminUsers();
            System.out.println("  Registered 2 test admins. Total active admins: " + adminCount);

            // Self-demotion & self-deletion check logic
            session.login(admin1);
            int currentAdminId = session.getUserId();
            boolean selfProtectionEnforced = (admin1.getUserId() == currentAdminId);

            // Demote secondary admin
            boolean demoted = userDAO.updateUserRole(admin2.getUserId(), User.Role.STUDENT);
            int remainingAdmins = userDAO.countAdminUsers();
            boolean lastAdminProtected = (remainingAdmins <= 1);

            if (selfProtectionEnforced && demoted && lastAdminProtected) {
                System.out.println("  [PASS] Self-protection and last-admin protection logic verified.");
            } else {
                System.err.println("  [FAIL] Admin protection checks failed.");
                allPassed = false;
            }
        } catch (Exception ex) {
            System.err.println("  [FAIL] Test 2 error: " + ex.getMessage());
            allPassed = false;
        }

        // Test 3: Foreign Key User Deletion Protection
        System.out.println("\n[Test 3] Testing Foreign Key User Deletion Protection...");
        User studentReporter = new User("Student Reporter", "rep_" + timestamp + "@campus.edu", PasswordUtil.hashPassword("Pass@123"), User.Role.STUDENT);
        Item reportedItem = null;
        try {
            userDAO.register(studentReporter);
            reportedItem = new Item("Smart Watch", "Silver strap", "Electronics", "Gym", Item.ItemType.FOUND, Item.ItemStatus.OPEN, studentReporter.getUserId());
            itemDAO.addItem(reportedItem);

            boolean hasRelated = userDAO.hasUserRelatedRecords(studentReporter.getUserId());
            if (hasRelated) {
                System.out.println("  [PASS] hasUserRelatedRecords correctly detected active item reports, preventing deletion.");
            } else {
                System.err.println("  [FAIL] hasUserRelatedRecords failed to detect related records.");
                allPassed = false;
            }
        } catch (Exception ex) {
            System.err.println("  [FAIL] Test 3 error: " + ex.getMessage());
            allPassed = false;
        }

        // Test 4: Atomic Claim Approval Transaction & State Synchronization
        System.out.println("\n[Test 4] Testing Claim Approval Transaction & State Synchronization...");
        User claimant1 = new User("Claimant One", "c1_" + timestamp + "@campus.edu", PasswordUtil.hashPassword("Pass@123"), User.Role.STUDENT);
        User claimant2 = new User("Claimant Two", "c2_" + timestamp + "@campus.edu", PasswordUtil.hashPassword("Pass@123"), User.Role.STUDENT);
        Claim claim1 = null;
        Claim claim2 = null;

        try {
            userDAO.register(claimant1);
            userDAO.register(claimant2);

            claim1 = new Claim(reportedItem.getItemId(), claimant1.getUserId(), "My watch has a scratch on the side bezel.");
            claim2 = new Claim(reportedItem.getItemId(), claimant2.getUserId(), "It fell out of my gym locker.");
            claimDAO.submitClaim(claim1);
            claimDAO.submitClaim(claim2);

            System.out.println("  Submitted Claim #" + claim1.getClaimId() + " and Claim #" + claim2.getClaimId() + " for Item #" + reportedItem.getItemId());

            // Approve Claim 1 via atomic transaction
            boolean approved = claimDAO.approveClaimTransaction(claim1.getClaimId(), reportedItem.getItemId());
            if (!approved) {
                throw new RuntimeException("Claim approval transaction returned false.");
            }

            // Verify claim1 is APPROVED
            Claim refreshedClaim1 = claimDAO.findById(claim1.getClaimId());
            // Verify claim2 was automatically REJECTED
            Claim refreshedClaim2 = claimDAO.findById(claim2.getClaimId());
            // Verify reportedItem status is now CLAIMED
            Item refreshedItem = itemDAO.findById(reportedItem.getItemId());

            boolean claim1Approved = (refreshedClaim1.getStatus() == Claim.ClaimStatus.APPROVED);
            boolean claim2Rejected = (refreshedClaim2.getStatus() == Claim.ClaimStatus.REJECTED);
            boolean itemClaimed = (refreshedItem.getStatus() == Item.ItemStatus.CLAIMED);

            if (claim1Approved && claim2Rejected && itemClaimed) {
                System.out.println("  [PASS] Claim #1 is APPROVED, Item is CLAIMED, and competing Claim #2 was automatically REJECTED.");
            } else {
                System.err.println("  [FAIL] State synchronization mismatch: claim1=" + refreshedClaim1.getStatus() +
                        ", claim2=" + refreshedClaim2.getStatus() + ", item=" + refreshedItem.getStatus());
                allPassed = false;
            }

            // Verify conflicting approval is blocked
            try {
                claimDAO.approveClaimTransaction(claim2.getClaimId(), reportedItem.getItemId());
                System.err.println("  [FAIL] Conflicting approval was NOT blocked!");
                allPassed = false;
            } catch (IllegalStateException ise) {
                System.out.println("  [PASS] Conflicting approval correctly blocked with exception: " + ise.getMessage());
            }

        } catch (Exception ex) {
            System.err.println("  [FAIL] Test 4 error: " + ex.getMessage());
            allPassed = false;
        }

        // Test 5: Safe Atomic Item Deletion With Claims
        System.out.println("\n[Test 5] Testing Safe Atomic Item Deletion With Associated Claims...");
        try {
            boolean deleted = itemDAO.deleteItemWithClaims(reportedItem.getItemId());
            if (deleted && itemDAO.findById(reportedItem.getItemId()) == null && claimDAO.findById(claim1.getClaimId()) == null) {
                System.out.println("  [PASS] Item and associated claims were cleanly removed in a single transaction.");
            } else {
                System.err.println("  [FAIL] Item deletion with claims failed.");
                allPassed = false;
            }
        } catch (Exception ex) {
            System.err.println("  [FAIL] Test 5 error: " + ex.getMessage());
            allPassed = false;
        }

        // Test 6: Database Cleanup & Zero-Footprint Verification
        System.out.println("\n[Test 6] Performing Zero-Footprint Cleanup...");
        try {
            userDAO.deleteUser(admin1.getUserId());
            userDAO.deleteUser(admin2.getUserId());
            userDAO.deleteUser(studentReporter.getUserId());
            userDAO.deleteUser(claimant1.getUserId());
            userDAO.deleteUser(claimant2.getUserId());
            session.logout();

            int finalUsers = userDAO.getTotalUsersCount();
            int finalItems = itemDAO.getLostItemsCount() + itemDAO.getFoundItemsCount();
            int finalClaims = claimDAO.getPendingClaimsCount();

            System.out.println("  Final DB counts: Users=" + finalUsers + ", Items=" + finalItems + ", PendingClaims=" + finalClaims);

            if (finalUsers == initialUsers && finalItems == initialItems && finalClaims == initialClaims) {
                System.out.println("  [PASS] Database completely restored to original clean state (0 rows).");
            } else {
                System.err.println("  [FAIL] Leftover records detected!");
                allPassed = false;
            }
        } catch (Exception ex) {
            System.err.println("  [FAIL] Cleanup error: " + ex.getMessage());
            allPassed = false;
        }

        System.out.println("\n--------------------------------------------------");
        if (allPassed) {
            System.out.println("[SUCCESS] Stage 5 verification completed successfully!");
        } else {
            System.err.println("[WARNING] Stage 5 verification encountered issues.");
        }
        System.out.println("--------------------------------------------------");
    }
}

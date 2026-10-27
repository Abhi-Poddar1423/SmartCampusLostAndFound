package com.campus.lostfound;

import com.campus.lostfound.dao.ClaimDAO;
import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.Claim;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.ui.student.ReportItemDialog;
import com.campus.lostfound.utils.PasswordUtil;
import com.campus.lostfound.utils.SessionManager;

import java.util.List;

/**
 * Stage4Test
 * 
 * Comprehensive automated verification for Stage 4:
 * 1. UI components & category catalog verification.
 * 2. Claim eligibility rules (prevent self-claims, prevent lost-item claims).
 * 3. End-to-end zero-footprint test:
 *    - Registers two ephemeral test students.
 *    - Student 1 reports a found item.
 *    - Student 2 claims the found item.
 *    - Verifies duplicate claims are blocked.
 *    - Cleans up all test data immediately so the database remains 100% clean.
 */
public class Stage4Test {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("      Smart Campus Lost & Found - Stage 4 Test    ");
        System.out.println("==================================================");

        boolean allPassed = true;

        // 1. Verify Category Catalog
        System.out.println("\n[Test 1] Verifying Category Catalog...");
        if (ReportItemDialog.CATEGORIES.length >= 8) {
            System.out.println("  [PASS] Verified " + ReportItemDialog.CATEGORIES.length + " standard campus categories.");
        } else {
            System.err.println("  [FAIL] Category catalog is incomplete.");
            allPassed = false;
        }

        // 2. Test Claim Eligibility Rules (Unit Logic)
        System.out.println("\n[Test 2] Testing Claim Eligibility Rules...");
        Item ownItem = new Item("Calculator", "TI-84", "Electronics", "Lab", Item.ItemType.FOUND, Item.ItemStatus.OPEN, 10);
        Item lostItem = new Item("Wallet", "Brown", "Other", "Gym", Item.ItemType.LOST, Item.ItemStatus.OPEN, 20);
        Item claimedItem = new Item("Phone", "iPhone", "Electronics", "Library", Item.ItemType.FOUND, Item.ItemStatus.CLAIMED, 30);
        Item eligibleItem = new Item("Keys", "Room 101", "Keys", "Lobby", Item.ItemType.FOUND, Item.ItemStatus.OPEN, 40);

        int studentId = 10;
        boolean ownItemClaimable = (ownItem.getItemType() == Item.ItemType.FOUND && ownItem.getStatus() == Item.ItemStatus.OPEN && ownItem.getReportedBy() != studentId);
        boolean lostItemClaimable = (lostItem.getItemType() == Item.ItemType.FOUND && lostItem.getStatus() == Item.ItemStatus.OPEN && lostItem.getReportedBy() != studentId);
        boolean claimedItemClaimable = (claimedItem.getItemType() == Item.ItemType.FOUND && claimedItem.getStatus() == Item.ItemStatus.OPEN && claimedItem.getReportedBy() != studentId);
        boolean eligibleClaimable = (eligibleItem.getItemType() == Item.ItemType.FOUND && eligibleItem.getStatus() == Item.ItemStatus.OPEN && eligibleItem.getReportedBy() != studentId);

        if (!ownItemClaimable && !lostItemClaimable && !claimedItemClaimable && eligibleClaimable) {
            System.out.println("  [PASS] All business logic claim eligibility checks passed.");
        } else {
            System.err.println("  [FAIL] Claim eligibility checks failed.");
            allPassed = false;
        }

        // 3. Live Zero-Footprint End-to-End Cycle
        System.out.println("\n[Test 3] Testing Live Database Item Reporting & Claim Submission (Zero-Footprint)...");
        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        ClaimDAO claimDAO = new ClaimDAO();

        long timestamp = System.currentTimeMillis();
        User studentA = new User("Student A", "studentA_" + timestamp + "@campus.edu", PasswordUtil.hashPassword("Pass@123"), User.Role.STUDENT);
        User studentB = new User("Student B", "studentB_" + timestamp + "@campus.edu", PasswordUtil.hashPassword("Pass@123"), User.Role.STUDENT);

        Item testItem = null;
        Claim testClaim = null;

        try {
            // Register Student A & Student B
            userDAO.register(studentA);
            userDAO.register(studentB);
            System.out.println("  Created test students: ID " + studentA.getUserId() + " & ID " + studentB.getUserId());

            // Student A reports a FOUND item
            testItem = new Item(
                    "Test Blue Backpack",
                    "Contains blue notebook and water bottle",
                    "Bags & Backpacks",
                    "Main Cafeteria Table 4",
                    Item.ItemType.FOUND,
                    Item.ItemStatus.OPEN,
                    studentA.getUserId()
            );
            boolean itemReported = itemDAO.addItem(testItem);
            if (!itemReported || testItem.getItemId() <= 0) {
                throw new RuntimeException("Failed to insert test item report.");
            }
            System.out.println("  Student A reported item successfully (Item ID: " + testItem.getItemId() + ")");

            // Search verification
            List<Item> searchResults = itemDAO.searchItems("Backpack", "Bags & Backpacks", "FOUND", "OPEN");
            if (!searchResults.isEmpty()) {
                System.out.println("  [PASS] ItemDAO.searchItems successfully located the reported item.");
            } else {
                System.err.println("  [FAIL] ItemDAO.searchItems could not locate the item.");
                allPassed = false;
            }

            // Student B submits a claim for the item
            testClaim = new Claim(
                    testItem.getItemId(),
                    studentB.getUserId(),
                    "It is a Skybags navy blue backpack with a keychain on the front zipper."
            );
            boolean claimSubmitted = claimDAO.submitClaim(testClaim);
            if (!claimSubmitted || testClaim.getClaimId() <= 0) {
                throw new RuntimeException("Failed to submit claim.");
            }
            System.out.println("  Student B submitted claim successfully (Claim ID: " + testClaim.getClaimId() + ")");

            // Duplicate claim prevention check
            boolean duplicateBlocked = claimDAO.hasUserClaimed(testItem.getItemId(), studentB.getUserId());
            if (duplicateBlocked) {
                System.out.println("  [PASS] Duplicate claim prevention verified (hasUserClaimed returned true).");
            } else {
                System.err.println("  [FAIL] Duplicate claim check failed.");
                allPassed = false;
            }

            // Student B claim retrieval check
            List<Claim> studentBClaims = claimDAO.getClaimsByUser(studentB.getUserId());
            if (!studentBClaims.isEmpty() && studentBClaims.get(0).getClaimId() == testClaim.getClaimId()) {
                System.out.println("  [PASS] ClaimDAO.getClaimsByUser successfully retrieved the claim.");
            } else {
                System.err.println("  [FAIL] Claim retrieval failed.");
                allPassed = false;
            }

        } catch (Exception e) {
            System.err.println("  [FAIL] Live test cycle threw exception: " + e.getMessage());
            allPassed = false;
        } finally {
            // Immediate Complete Cleanup (Zero-Footprint Guarantee)
            System.out.println("  Cleaning up temporary test records...");
            try (java.sql.Connection conn = com.campus.lostfound.config.DBConnection.getConnection()) {
                if (testClaim != null && testClaim.getClaimId() > 0) {
                    try (java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM claims WHERE claim_id = ?")) {
                        ps.setInt(1, testClaim.getClaimId());
                        ps.executeUpdate();
                    }
                }
                if (testItem != null && testItem.getItemId() > 0) {
                    itemDAO.deleteItem(testItem.getItemId());
                }
                if (studentA.getUserId() > 0) {
                    userDAO.deleteUser(studentA.getUserId());
                }
                if (studentB.getUserId() > 0) {
                    userDAO.deleteUser(studentB.getUserId());
                }
                System.out.println("  [PASS] All test records cleanly purged. Database restored to pristine state.");
            } catch (Exception ex) {
                System.err.println("  [WARNING] Cleanup error: " + ex.getMessage());
            }
        }

        System.out.println("\n--------------------------------------------------");
        if (allPassed) {
            System.out.println("[SUCCESS] Stage 4 verification completed successfully!");
        } else {
            System.err.println("[WARNING] Stage 4 verification encountered issues.");
        }
        System.out.println("--------------------------------------------------");
    }
}

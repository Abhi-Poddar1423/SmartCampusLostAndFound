package com.campus.lostfound;

import com.campus.lostfound.dao.ClaimDAO;
import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.Claim;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.User;
import com.campus.lostfound.utils.PasswordUtil;

import java.util.List;

/**
 * Stage2Test
 * 
 * Safely verifies:
 * 1. PasswordUtil: PBKDF2 hash generation and constant-time verification.
 * 2. Model classes: User, Item, Claim instantiation and enums.
 * 3. DAO classes: UserDAO, ItemDAO, ClaimDAO queries against existing database tables (READ-ONLY).
 * 
 * Does NOT insert, modify, or delete any records in the database.
 */
public class Stage2Test {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("      Smart Campus Lost & Found - Stage 2 Test    ");
        System.out.println("==================================================");

        boolean allPassed = true;

        // 1. Test PasswordUtil PBKDF2 Hashing and Verification
        System.out.println("\n[Test 1] Testing PBKDF2 Password Hashing...");
        String testPassword = "CampusPassword@2026";
        String hashed = PasswordUtil.hashPassword(testPassword);
        System.out.println("  Plain text sample : " + testPassword);
        System.out.println("  PBKDF2 hash       : " + hashed);
        System.out.println("  Hash character length : " + hashed.length() + " (fits inside VARCHAR(255))");

        boolean matchesCorrect = PasswordUtil.verifyPassword(testPassword, hashed);
        boolean matchesIncorrect = PasswordUtil.verifyPassword("WrongPassword123", hashed);

        if (matchesCorrect && !matchesIncorrect) {
            System.out.println("  [PASS] PBKDF2 hashing and verification works correctly!");
        } else {
            System.err.println("  [FAIL] Password verification failed!");
            allPassed = false;
        }

        // 2. Test Model Enums and Mapping
        System.out.println("\n[Test 2] Testing Model Enums...");
        if (User.Role.fromString("STUDENT") == User.Role.STUDENT &&
            Item.ItemType.fromString("LOST") == Item.ItemType.LOST &&
            Item.ItemStatus.fromString("OPEN") == Item.ItemStatus.OPEN &&
            Claim.ClaimStatus.fromString("PENDING") == Claim.ClaimStatus.PENDING) {
            System.out.println("  [PASS] All model enums mapped correctly to database schema.");
        } else {
            System.err.println("  [FAIL] Model enum mapping failed.");
            allPassed = false;
        }

        // 3. Test DAOs (Read-Only queries against MySQL)
        System.out.println("\n[Test 3] Testing DAOs against MySQL (Read-Only)...");
        try {
            UserDAO userDAO = new UserDAO();
            List<User> users = userDAO.getAllUsers();
            System.out.println("  UserDAO.getAllUsers() executed successfully. Found: " + users.size() + " records.");

            ItemDAO itemDAO = new ItemDAO();
            List<Item> items = itemDAO.getAllItems();
            System.out.println("  ItemDAO.getAllItems() executed successfully. Found: " + items.size() + " records.");

            ClaimDAO claimDAO = new ClaimDAO();
            List<Claim> claims = claimDAO.getAllClaims();
            System.out.println("  ClaimDAO.getAllClaims() executed successfully. Found: " + claims.size() + " records.");

            System.out.println("  [PASS] All DAO read-only queries executed without SQL errors.");
        } catch (Exception e) {
            System.err.println("  [FAIL] DAO testing threw exception: " + e.getMessage());
            allPassed = false;
        }

        System.out.println("\n--------------------------------------------------");
        if (allPassed) {
            System.out.println("[SUCCESS] Stage 2 verification completed successfully!");
        } else {
            System.err.println("[WARNING] Stage 2 verification encountered issues.");
        }
        System.out.println("--------------------------------------------------");
    }
}

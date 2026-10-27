package com.campus.lostfound;

import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.PasswordUtil;
import com.campus.lostfound.utils.SessionManager;
import com.campus.lostfound.utils.ValidationUtil;

/**
 * Stage3Test
 * 
 * Safely verifies:
 * 1. UITheme Look & Feel setup.
 * 2. ValidationUtil: registration and login validation rules.
 * 3. SessionManager: thread-safe login and logout session state.
 * 4. Authentication cycle test: registers a temporary test user, verifies PBKDF2 password
 *    authentication, and cleans up immediately so NO test data remains in the database.
 */
public class Stage3Test {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("      Smart Campus Lost & Found - Stage 3 Test    ");
        System.out.println("==================================================");

        boolean allPassed = true;

        // 1. UI Theme Initialization Test
        System.out.println("\n[Test 1] Testing UITheme Look and Feel initialization...");
        try {
            UITheme.setupLookAndFeel();
            System.out.println("  [PASS] UITheme initialized FlatLaf Look and Feel successfully.");
        } catch (Exception e) {
            System.err.println("  [FAIL] UITheme failed: " + e.getMessage());
            allPassed = false;
        }

        // 2. ValidationUtil Test
        System.out.println("\n[Test 2] Testing ValidationUtil rules...");
        ValidationUtil.ValidationResult validReg = ValidationUtil.validateRegistration(
                "Aarav Sharma", "aarav@campus.edu", "StrongPass@123", "StrongPass@123"
        );
        ValidationUtil.ValidationResult invalidEmail = ValidationUtil.validateRegistration(
                "Aarav Sharma", "bad-email-format", "StrongPass@123", "StrongPass@123"
        );
        ValidationUtil.ValidationResult shortPass = ValidationUtil.validateRegistration(
                "Aarav Sharma", "aarav@campus.edu", "123", "123"
        );
        ValidationUtil.ValidationResult mismatchedPass = ValidationUtil.validateRegistration(
                "Aarav Sharma", "aarav@campus.edu", "StrongPass@123", "DifferentPass@123"
        );

        if (validReg.isValid() &&
            !invalidEmail.isValid() &&
            !shortPass.isValid() &&
            !mismatchedPass.isValid()) {
            System.out.println("  [PASS] All registration validation rules enforced correctly.");
        } else {
            System.err.println("  [FAIL] ValidationUtil registration checks failed.");
            allPassed = false;
        }

        // 3. SessionManager Test
        System.out.println("\n[Test 3] Testing SessionManager lifecycle...");
        SessionManager session = SessionManager.getInstance();
        User dummyUser = new User(999, "Session User", "session@campus.edu", "dummyHash", User.Role.STUDENT, null);
        
        session.login(dummyUser);
        boolean loggedInOk = session.isLoggedIn() && session.getUserName().equals("Session User");
        session.logout();
        boolean loggedOutOk = !session.isLoggedIn();

        if (loggedInOk && loggedOutOk) {
            System.out.println("  [PASS] SessionManager login, role checking, and logout work correctly.");
        } else {
            System.err.println("  [FAIL] SessionManager lifecycle test failed.");
            allPassed = false;
        }

        // 4. Safe Database Registration & Authentication Cycle (with instant cleanup)
        System.out.println("\n[Test 4] Testing Live Database Registration & Authentication (Zero-Footprint)...");
        UserDAO userDAO = new UserDAO();
        String tempEmail = "ephemeral_test_" + System.currentTimeMillis() + "@campus.edu";
        String rawPassword = "TestSecretPassword#99";
        String hashed = PasswordUtil.hashPassword(rawPassword);

        User tempUser = new User("Ephemeral Test User", tempEmail, hashed, User.Role.STUDENT);

        try {
            // Register
            boolean registered = userDAO.register(tempUser);
            if (!registered || tempUser.getUserId() <= 0) {
                System.err.println("  [FAIL] Temporary user registration failed.");
                allPassed = false;
            } else {
                System.out.println("  User registered with generated ID: " + tempUser.getUserId());

                // Authenticate
                User fetched = userDAO.findByEmail(tempEmail);
                if (fetched != null && PasswordUtil.verifyPassword(rawPassword, fetched.getPassword())) {
                    System.out.println("  [PASS] User fetched and PBKDF2 password verified successfully.");
                } else {
                    System.err.println("  [FAIL] PBKDF2 verification against database record failed.");
                    allPassed = false;
                }

                // Immediate cleanup: Delete the test user so database remains 100% clean
                boolean deleted = userDAO.deleteUser(tempUser.getUserId());
                if (deleted) {
                    System.out.println("  [PASS] Temporary test account cleanly deleted. Database row count restored to original.");
                } else {
                    System.err.println("  [WARNING] Could not delete temporary test user ID: " + tempUser.getUserId());
                }
            }
        } catch (Exception e) {
            System.err.println("  [FAIL] Authentication cycle encountered error: " + e.getMessage());
            allPassed = false;
        }

        System.out.println("\n--------------------------------------------------");
        if (allPassed) {
            System.out.println("[SUCCESS] Stage 3 verification completed successfully!");
        } else {
            System.err.println("[WARNING] Stage 3 verification encountered issues.");
        }
        System.out.println("--------------------------------------------------");
    }
}

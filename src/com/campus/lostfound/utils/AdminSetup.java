package com.campus.lostfound.utils;

import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.User;

import java.io.Console;
import java.util.Scanner;

/**
 * AdminSetup
 * 
 * Secure One-Time Administrator Account Initializer.
 * 
 * Security Guarantees:
 * 1. Passwords are never hardcoded in source files.
 * 2. Passwords and hashes are NEVER printed or logged.
 * 3. Passwords are hashed using PBKDF2WithHmacSHA256 with a cryptographically secure random salt.
 * 4. Can be executed interactively via command line or batch script.
 */
public class AdminSetup {

    /**
     * Programmatic admin creation / promotion method.
     * 
     * @param name Admin's full name
     * @param email Admin's email address
     * @param plainPassword Admin's plain text password
     * @return true if successfully created or promoted to ADMIN
     */
    public static boolean setupAdmin(String name, String email, String plainPassword) {
        if (name == null || name.trim().length() < 2) {
            System.err.println("[AdminSetup] Error: Invalid admin name.");
            return false;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            System.err.println("[AdminSetup] Error: Invalid email format.");
            return false;
        }
        if (plainPassword == null || plainPassword.length() < 6) {
            System.err.println("[AdminSetup] Error: Password must be at least 6 characters.");
            return false;
        }

        // Security Guard: Prohibit bootstrap execution if a Student session is active
        if (SessionManager.getInstance().isLoggedIn() && !SessionManager.getInstance().isAdmin()) {
            System.err.println("[AdminSetup] Security Guard: Cannot execute Administrator setup while a Student session is active.");
            return false;
        }

        UserDAO userDAO = new UserDAO();

        // Policy Guard: Smart Campus Lost & Found enforces initial setup only once
        int activeAdmins = userDAO.countAdminUsers();
        if (activeAdmins > 0) {
            System.err.println("[AdminSetup] Policy Guard: Smart Campus Lost & Found enforces exactly ONE active Administrator.");
            System.err.println("  Initial bootstrap has already been completed. An active Administrator already exists in the system.");
            return false;
        }

        try {
            boolean success = userDAO.executeInitialAdminBootstrap(name.trim(), email.trim(), plainPassword);
            if (success) {
                System.out.println("[AdminSetup] Successfully initialized administrator account for: " + email);
            }
            return success;
        } catch (IllegalStateException e) {
            System.err.println("[AdminSetup] Bootstrap rejected: " + e.getMessage());
            return false;
        } catch (SecurityException e) {
            System.err.println("[AdminSetup] Security violation: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("[AdminSetup] Failed to initialize administrator: " + e.getMessage());
            return false;
        }
    }

    /**
     * Interactive command-line setup entry point.
     */
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  Smart Campus Lost & Found - Administrator Setup ");
        System.out.println("==================================================");

        Console console = System.console();
        Scanner scanner = new Scanner(System.in);

        String name;
        String email;
        String password;

        if (args.length == 3) {
            // Non-interactive command line args
            name = args[0];
            email = args[1];
            password = args[2];
        } else {
            // Interactive prompts
            System.out.print("Enter Administrator Full Name: ");
            name = scanner.nextLine().trim();

            System.out.print("Enter Administrator Email: ");
            email = scanner.nextLine().trim();

            if (console != null) {
                char[] passChars = console.readPassword("Enter Administrator Password: ");
                password = new String(passChars);
            } else {
                System.out.print("Enter Administrator Password: ");
                password = scanner.nextLine();
            }
        }

        boolean success = setupAdmin(name, email, password);
        if (success) {
            System.out.println("\n[SUCCESS] Setup complete! You can now log into the Admin Dashboard.");
        } else {
            System.err.println("\n[ERROR] Administrator setup could not be completed.");
        }
    }
}

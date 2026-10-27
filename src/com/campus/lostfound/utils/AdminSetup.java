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

        UserDAO userDAO = new UserDAO();
        User existingUser = userDAO.findByEmail(email.trim());

        if (existingUser != null) {
            if (existingUser.getRole() == User.Role.ADMIN) {
                System.out.println("[AdminSetup] Account already has ADMIN role: " + email);
                return true;
            }
            // Elevate existing user to ADMIN
            boolean updated = userDAO.updateUserRole(existingUser.getUserId(), User.Role.ADMIN);
            if (updated) {
                System.out.println("[AdminSetup] Successfully elevated existing account to ADMIN: " + email);
                return true;
            } else {
                System.err.println("[AdminSetup] Failed to update role for: " + email);
                return false;
            }
        }

        // Create new Administrator user
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);
        User newAdmin = new User(name.trim(), email.trim(), hashedPassword, User.Role.ADMIN);

        boolean registered = userDAO.register(newAdmin);
        if (registered) {
            System.out.println("[AdminSetup] Successfully created new administrator account for: " + email);
            return true;
        } else {
            System.err.println("[AdminSetup] Failed to register administrator in database.");
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

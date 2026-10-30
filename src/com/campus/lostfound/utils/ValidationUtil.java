package com.campus.lostfound.utils;

import java.util.regex.Pattern;

/**
 * ValidationUtil
 * 
 * Provides centralized input validation for authentication forms (Login & Registration).
 * Ensures clean user feedback before making database calls.
 */
public class ValidationUtil {

    // Standard RFC-compliant email regex pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // Name regex: Allows letters, spaces, dots, and hyphens (min 2 chars)
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[A-Za-z\\s.-]{2,100}$"
    );

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
    }

    /**
     * Validates input fields for user registration.
     * 
     * @param name Full name
     * @param email Email address
     * @param password Password
     * @param confirmPassword Password confirmation
     * @return ValidationResult indicating success or specific failure reason
     */
    public static ValidationResult validateRegistration(String name, String email, String password, String confirmPassword) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.fail("Full name is required.");
        }
        if (!NAME_PATTERN.matcher(name.trim()).matches()) {
            return ValidationResult.fail("Please enter a valid full name (letters only, at least 2 characters).");
        }

        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.fail("Email address is required.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return ValidationResult.fail("Please enter a valid email address (e.g. student@college.edu).");
        }

        if (password == null || password.isEmpty()) {
            return ValidationResult.fail("Password is required.");
        }
        if (password.length() < 6) {
            return ValidationResult.fail("Password must be at least 6 characters long.");
        }

        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return ValidationResult.fail("Please confirm your password.");
        }
        if (!password.equals(confirmPassword)) {
            return ValidationResult.fail("Passwords do not match. Please re-enter.");
        }

        return ValidationResult.success();
    }

    /**
     * Validates input fields for user login.
     * 
     * @param email Email address
     * @param password Password
     * @return ValidationResult indicating success or specific failure reason
     */
    public static ValidationResult validateLogin(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.fail("Email address is required.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return ValidationResult.fail("Please enter a valid email format.");
        }

        if (password == null || password.isEmpty()) {
            return ValidationResult.fail("Password is required.");
        }

        return ValidationResult.success();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates optional student profile roll number.
     */
    public static boolean isValidRollNumber(String roll) {
        if (roll == null || roll.trim().isEmpty()) return true;
        return roll.trim().matches("^[A-Za-z0-9/_-]{2,30}$");
    }

    /**
     * Validates optional phone number format.
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) return true;
        return phone.trim().matches("^\\+?[0-9\\s()\\-]{7,20}$");
    }
}

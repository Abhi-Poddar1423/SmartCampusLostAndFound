package com.campus.lostfound.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * PasswordUtil
 * 
 * Provides secure password hashing and verification using PBKDF2
 * (Password-Based Key Derivation Function 2) with HMAC-SHA256 and a random salt.
 * 
 * Stored Hash Format:
 *   iterations:base64(salt):base64(hash)
 * 
 * Why this is secure:
 * 1. Random Salt: Ensures identical passwords produce completely different hashes.
 * 2. 65,536 Iterations: Makes brute-force and dictionary attacks computationally expensive.
 * 3. Constant-Time Comparison: Uses MessageDigest.isEqual() to prevent timing attacks.
 */
public class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // in bits (32 bytes)
    private static final int SALT_LENGTH = 16;  // in bytes

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Hashes a plain-text password using PBKDF2 with a newly generated random salt.
     * 
     * @param plainPassword Plain-text password to hash
     * @return Formatted string containing iterations:salt:hash
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }

        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        return ITERATIONS + ":" + saltBase64 + ":" + hashBase64;
    }

    /**
     * Verifies a candidate plain-text password against a stored PBKDF2 hash.
     * 
     * @param candidatePassword Plain-text password entered by the user
     * @param storedHash The hashed string previously saved in the database
     * @return true if candidate matches the stored hash, false otherwise
     */
    public static boolean verifyPassword(String candidatePassword, String storedHash) {
        if (candidatePassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            // Unrecognized hash format
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            byte[] actualHash = pbkdf2(candidatePassword.toCharArray(), salt, iterations, expectedHash.length * 8);

            // MessageDigest.isEqual provides constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (Exception e) {
            System.err.println("[PasswordUtil] Password verification error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generates the PBKDF2 hash byte array.
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2 hashing failed: " + e.getMessage(), e);
        }
    }
}

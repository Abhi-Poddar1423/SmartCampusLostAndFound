package com.campus.lostfound.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LoginRateLimiter
 * 
 * Thread-safe rate limiter that protects against brute-force login attacks.
 * 
 * Security features:
 * 1. Tracks consecutive failed login attempts per normalized email.
 * 2. Applies a temporary lockout after 5 consecutive failures.
 * 3. Lockout duration is 15 minutes by default (configurable for testing).
 * 4. Automatically clears failure counters upon successful authentication.
 * 5. Automatically lifts lockouts when the duration expires (no permanent lockout).
 * 6. Thread-safe using ConcurrentHashMap and synchronized methods.
 * 7. Prevents user enumeration by operating consistently on normalized input.
 */
public class LoginRateLimiter {

    public static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;
    public static final long DEFAULT_LOCKOUT_DURATION_MS = 15 * 60 * 1000L; // 15 minutes

    private static volatile LoginRateLimiter instance;

    private int maxFailedAttempts = DEFAULT_MAX_FAILED_ATTEMPTS;
    private long lockoutDurationMs = DEFAULT_LOCKOUT_DURATION_MS;

    private final Map<String, AttemptRecord> attemptMap = new ConcurrentHashMap<>();

    private static class AttemptRecord {
        int failureCount = 0;
        long lockoutExpiryMs = 0L;
        long lastAttemptMs = 0L;
    }

    private LoginRateLimiter() {
        // Singleton private constructor
    }

    public static LoginRateLimiter getInstance() {
        if (instance == null) {
            synchronized (LoginRateLimiter.class) {
                if (instance == null) {
                    instance = new LoginRateLimiter();
                }
            }
        }
        return instance;
    }

    private String normalizeKey(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    /**
     * Checks if the given email account is currently temporarily locked out.
     * Automatically clears expired lockouts.
     * 
     * @param email Account email
     * @return true if currently locked out, false otherwise
     */
    public synchronized boolean isLocked(String email) {
        String key = normalizeKey(email);
        AttemptRecord record = attemptMap.get(key);
        if (record == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (record.lockoutExpiryMs > 0) {
            if (now < record.lockoutExpiryMs) {
                return true; // Still locked
            } else {
                // Lockout duration has expired: reset counters
                record.failureCount = 0;
                record.lockoutExpiryMs = 0L;
                return false;
            }
        }

        return false;
    }

    /**
     * Returns the remaining lockout time in seconds for the given email.
     * 
     * @param email Account email
     * @return Remaining seconds, or 0 if not locked
     */
    public synchronized long getRemainingLockoutSeconds(String email) {
        String key = normalizeKey(email);
        AttemptRecord record = attemptMap.get(key);
        if (record == null || record.lockoutExpiryMs <= 0) {
            return 0L;
        }

        long remainingMs = record.lockoutExpiryMs - System.currentTimeMillis();
        if (remainingMs <= 0) {
            record.failureCount = 0;
            record.lockoutExpiryMs = 0L;
            return 0L;
        }
        return (remainingMs + 999) / 1000;
    }

    /**
     * Returns a user-friendly error message when an account is temporarily locked.
     * 
     * @param email Account email
     * @return Formatted lockout notification string
     */
    public synchronized String getLockoutMessage(String email) {
        long seconds = getRemainingLockoutSeconds(email);
        long minutes = (seconds + 59) / 60;
        if (minutes <= 1) {
            return "Too many failed login attempts. Account temporarily locked. Please try again in 1 minute.";
        } else {
            return String.format("Too many failed login attempts. Account temporarily locked. Please try again in %d minutes.", minutes);
        }
    }

    /**
     * Records a failed login attempt for the given email.
     * If failure count reaches maxFailedAttempts, triggers a temporary lockout.
     * 
     * @param email Account email
     * @return true if this failure caused the account to become locked out, false otherwise
     */
    public synchronized boolean recordFailedAttempt(String email) {
        String key = normalizeKey(email);
        long now = System.currentTimeMillis();

        AttemptRecord record = attemptMap.computeIfAbsent(key, k -> new AttemptRecord());

        // If previously locked but lock expired, reset count first
        if (record.lockoutExpiryMs > 0 && now >= record.lockoutExpiryMs) {
            record.failureCount = 0;
            record.lockoutExpiryMs = 0L;
        }

        record.failureCount++;
        record.lastAttemptMs = now;

        if (record.failureCount >= maxFailedAttempts) {
            record.lockoutExpiryMs = now + lockoutDurationMs;
            return true;
        }
        return false;
    }

    /**
     * Resets failure counters and lockouts upon a successful authentication.
     * 
     * @param email Account email
     */
    public synchronized void recordSuccessfulLogin(String email) {
        String key = normalizeKey(email);
        attemptMap.remove(key);
    }

    /**
     * Returns the current number of consecutive failed attempts for the given email.
     */
    public synchronized int getFailedAttempts(String email) {
        String key = normalizeKey(email);
        AttemptRecord record = attemptMap.get(key);
        if (record == null) return 0;
        // Check if lock expired
        if (record.lockoutExpiryMs > 0 && System.currentTimeMillis() >= record.lockoutExpiryMs) {
            record.failureCount = 0;
            record.lockoutExpiryMs = 0L;
            return 0;
        }
        return record.failureCount;
    }

    /**
     * Returns attempts remaining before lockout.
     */
    public synchronized int getAttemptsRemaining(String email) {
        int failed = getFailedAttempts(email);
        return Math.max(0, maxFailedAttempts - failed);
    }

    public synchronized void setMaxFailedAttempts(int max) {
        this.maxFailedAttempts = Math.max(1, max);
    }

    public synchronized int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public synchronized void setLockoutDurationMs(long ms) {
        this.lockoutDurationMs = Math.max(100L, ms);
    }

    public synchronized long getLockoutDurationMs() {
        return lockoutDurationMs;
    }

    /**
     * Clears all tracking state (used in automated unit testing).
     */
    public synchronized void resetForTesting() {
        attemptMap.clear();
        this.maxFailedAttempts = DEFAULT_MAX_FAILED_ATTEMPTS;
        this.lockoutDurationMs = DEFAULT_LOCKOUT_DURATION_MS;
    }
}

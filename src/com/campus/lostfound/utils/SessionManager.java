package com.campus.lostfound.utils;

import com.campus.lostfound.models.User;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SessionManager
 * 
 * Thread-safe singleton that manages active user authentication and session lifecycle.
 * 
 * Security and reliability features:
 * 1. 15-Minute Inactivity Timeout: Automatically terminates inactive sessions.
 * 2. Cross-Screen Activity Tracking: Tracks mouse and keyboard activity globally via AWTEventListener.
 * 3. EDT Thread Safety: Uses Swing Timer for background inactivity checks without blocking EDT.
 * 4. Duplicate Execution Protection: Uses AtomicBoolean to guarantee timeout handling runs exactly once.
 * 5. Protected DAO Invalidation: Expired sessions immediately return -1 for getUserId() and false for isAdmin().
 * 6. Clean UI Restoration: Safely closes dashboard windows and redirects to LoginFrame.
 */
public class SessionManager {

    public static final long DEFAULT_SESSION_TIMEOUT_MS = 15 * 60 * 1000L; // 15 minutes
    public static final int TIMER_CHECK_INTERVAL_MS = 5000; // 5 seconds

    private static volatile SessionManager instance;

    private User currentUser;
    private volatile long sessionTimeoutMs = DEFAULT_SESSION_TIMEOUT_MS;
    private volatile long lastActivityTimeMs = 0L;

    private final AtomicBoolean sessionExpiredHandled = new AtomicBoolean(false);
    private volatile boolean headlessOrTesting = false;

    private javax.swing.Timer inactivityTimer;
    private static AWTEventListener globalActivityListener;
    private static boolean activityListenerRegistered = false;

    private SessionManager() {
        // Private constructor to prevent direct instantiation
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes a user session upon successful login.
     * Starts activity tracking and session timeout monitoring.
     * 
     * @param user The authenticated User object
     */
    public synchronized void login(User user) {
        this.currentUser = user;
        this.lastActivityTimeMs = System.currentTimeMillis();
        this.sessionExpiredHandled.set(false);
        startInactivityMonitoring();
    }

    /**
     * Terminates the active user session and stops monitoring timers.
     */
    public synchronized void logout() {
        this.currentUser = null;
        this.lastActivityTimeMs = 0L;
        stopInactivityMonitoring();
        this.sessionExpiredHandled.set(false);
    }

    /**
     * Records user activity (mouse movement, clicks, keystrokes) to refresh session lifespan.
     */
    public void recordActivity() {
        if (currentUser != null && !isSessionExpired()) {
            this.lastActivityTimeMs = System.currentTimeMillis();
        }
    }

    /**
     * Checks if the active session has exceeded the inactivity timeout.
     */
    public boolean isSessionExpired() {
        if (currentUser == null) {
            return false;
        }
        if (lastActivityTimeMs <= 0L) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - lastActivityTimeMs;
        return elapsed >= sessionTimeoutMs;
    }

    /**
     * Returns the currently authenticated user, or null if logged out or expired.
     */
    public synchronized User getCurrentUser() {
        if (currentUser == null) return null;
        if (isSessionExpired()) {
            currentUser = null;
            return null;
        }
        return currentUser;
    }

    /**
     * Checks if a user is currently logged in with a valid, non-expired session.
     */
    public synchronized boolean isLoggedIn() {
        if (currentUser == null) return false;
        if (isSessionExpired()) {
            currentUser = null;
            return false;
        }
        return true;
    }

    /**
     * Checks if the currently logged-in user is an administrator and session is valid.
     */
    public synchronized boolean isAdmin() {
        if (currentUser == null) return false;
        if (isSessionExpired()) {
            currentUser = null;
            return false;
        }
        return currentUser.isAdmin();
    }

    /**
     * Gets the logged-in user's ID, or -1 if logged out or session expired.
     */
    public synchronized int getUserId() {
        if (currentUser == null) return -1;
        if (isSessionExpired()) {
            currentUser = null;
            return -1;
        }
        return currentUser.getUserId();
    }

    /**
     * Gets the logged-in user's name, or empty string if not logged in.
     */
    public synchronized String getUserName() {
        if (currentUser == null || isSessionExpired()) return "";
        return currentUser.getName();
    }

    /**
     * Gets the logged-in user's email, or empty string if not logged in.
     */
    public synchronized String getUserEmail() {
        if (currentUser == null || isSessionExpired()) return "";
        return currentUser.getEmail();
    }

    /**
     * Re-validates the administrator session against the database.
     */
    public synchronized boolean revalidateAdminSession(com.campus.lostfound.dao.UserDAO userDAO) {
        if (currentUser == null || isSessionExpired()) return false;
        if (!currentUser.isAdmin()) return false;

        if (userDAO != null) {
            boolean stillAdmin = userDAO.isActiveAdmin(currentUser.getUserId());
            if (!stillAdmin) {
                currentUser.setRole(User.Role.STUDENT);
                return false;
            }
            return true;
        }
        return currentUser.isAdmin();
    }

    /**
     * Starts the global AWT listener and Swing EDT Timer for inactivity monitoring.
     */
    private void startInactivityMonitoring() {
        if (GraphicsEnvironment.isHeadless() || headlessOrTesting) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            registerGlobalActivityListener();

            if (inactivityTimer == null) {
                inactivityTimer = new javax.swing.Timer(TIMER_CHECK_INTERVAL_MS, e -> checkSessionInactivityOnEDT());
                inactivityTimer.setRepeats(true);
            }
            if (!inactivityTimer.isRunning()) {
                inactivityTimer.start();
            }
        });
    }

    /**
     * Stops the inactivity timer.
     */
    private void stopInactivityMonitoring() {
        if (inactivityTimer != null && inactivityTimer.isRunning()) {
            inactivityTimer.stop();
        }
    }

    /**
     * Registers a global AWTEventListener to intercept user interactions across all Swing frames.
     */
    private static synchronized void registerGlobalActivityListener() {
        if (!activityListenerRegistered && !GraphicsEnvironment.isHeadless()) {
            try {
                globalActivityListener = event -> {
                    SessionManager sm = SessionManager.getInstance();
                    if (sm.currentUser != null) {
                        sm.recordActivity();
                    }
                };
                Toolkit.getDefaultToolkit().addAWTEventListener(
                        globalActivityListener,
                        AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK |
                        AWTEvent.KEY_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK
                );
                activityListenerRegistered = true;
            } catch (SecurityException ignored) {
                // In restricted security manager environments, fallback gracefully
            }
        }
    }

    /**
     * Called on the Swing EDT by inactivityTimer.
     */
    private void checkSessionInactivityOnEDT() {
        if (currentUser != null && isSessionExpired()) {
            handleSessionExpiryOnEDT();
        }
    }

    /**
     * Handles session expiration cleanly on the Event Dispatch Thread:
     * 1. Invalidates user session.
     * 2. Closes open dashboard windows.
     * 3. Displays session expired notification dialog.
     * 4. Redirects to LoginFrame.
     */
    public void handleSessionExpiryOnEDT() {
        if (sessionExpiredHandled.compareAndSet(false, true)) {
            // Nullify user session data
            synchronized (this) {
                this.currentUser = null;
                this.lastActivityTimeMs = 0L;
                stopInactivityMonitoring();
            }

            if (!GraphicsEnvironment.isHeadless() && !headlessOrTesting) {
                // Close open dashboard windows
                for (java.awt.Frame frame : java.awt.Frame.getFrames()) {
                    if (frame instanceof com.campus.lostfound.ui.admin.AdminDashboard ||
                        frame instanceof com.campus.lostfound.ui.student.StudentDashboard) {
                        frame.dispose();
                    }
                }

                JOptionPane.showMessageDialog(
                        null,
                        "Your session has expired due to 15 minutes of inactivity.\nPlease sign in again.",
                        "Session Expired",
                        JOptionPane.WARNING_MESSAGE
                );

                new com.campus.lostfound.ui.auth.LoginFrame().setVisible(true);
            }
        }
    }

    // ==========================================
    // TESTING & CONFIGURATION HOOKS
    // ==========================================

    public synchronized void setSessionTimeoutMs(long timeoutMs) {
        this.sessionTimeoutMs = Math.max(100L, timeoutMs);
    }

    public synchronized long getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public synchronized void setLastActivityTimeMs(long timeMs) {
        this.lastActivityTimeMs = timeMs;
    }

    public synchronized long getLastActivityTimeMs() {
        return lastActivityTimeMs;
    }

    public void setHeadlessOrTesting(boolean testing) {
        this.headlessOrTesting = testing;
        if (testing) {
            stopInactivityMonitoring();
        }
    }

    public boolean isHeadlessOrTesting() {
        return headlessOrTesting;
    }
}

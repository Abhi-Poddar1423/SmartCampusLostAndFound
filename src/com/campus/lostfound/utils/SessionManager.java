package com.campus.lostfound.utils;

import com.campus.lostfound.models.User;

/**
 * SessionManager
 * 
 * Thread-safe singleton that manages the active user session across the application.
 * Retains information about the currently authenticated student or administrator.
 */
public class SessionManager {

    private static volatile SessionManager instance;
    private User currentUser;

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
     * 
     * @param user The authenticated User object
     */
    public synchronized void login(User user) {
        this.currentUser = user;
    }

    /**
     * Terminates the active user session.
     */
    public synchronized void logout() {
        this.currentUser = null;
    }

    /**
     * Returns the currently authenticated user.
     * 
     * @return User object, or null if no user is logged in
     */
    public synchronized User getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks if a user is currently logged in.
     */
    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Checks if the currently logged-in user is an administrator.
     */
    public synchronized boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    /**
     * Gets the logged-in user's ID, or -1 if not logged in.
     */
    public synchronized int getUserId() {
        return currentUser != null ? currentUser.getUserId() : -1;
    }

    /**
     * Gets the logged-in user's name, or empty string if not logged in.
     */
    public synchronized String getUserName() {
        return currentUser != null ? currentUser.getName() : "";
    }

    /**
     * Gets the logged-in user's email, or empty string if not logged in.
     */
    public synchronized String getUserEmail() {
        return currentUser != null ? currentUser.getEmail() : "";
    }
}

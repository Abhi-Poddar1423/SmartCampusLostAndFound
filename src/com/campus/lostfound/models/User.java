package com.campus.lostfound.models;

import java.sql.Timestamp;

/**
 * User Model
 * 
 * Represents a user record from the MySQL 'users' table.
 * Matches exact database schema:
 * - user_id    INT (Primary Key, Auto Increment)
 * - name       VARCHAR(100) NOT NULL
 * - email      VARCHAR(100) NOT NULL
 * - password   VARCHAR(255) NOT NULL (PBKDF2 hashed)
 * - role       ENUM('STUDENT', 'ADMIN')
 * - created_at TIMESTAMP
 */
public class User {

    // Enum matching MySQL: enum('STUDENT', 'ADMIN')
    public enum Role {
        STUDENT,
        ADMIN;

        public static Role fromString(String roleStr) {
            if (roleStr == null) return STUDENT;
            try {
                return Role.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return STUDENT;
            }
        }
    }

    private int userId;
    private String name;
    private String email;
    private String password; // PBKDF2 hash with salt
    private Role role;
    private Timestamp createdAt;

    // Default constructor
    public User() {
        this.role = Role.STUDENT;
    }

    // Constructor for creating a new user prior to insertion (no ID or timestamp yet)
    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : Role.STUDENT;
    }

    // Full constructor (retrieval from database)
    public User(int userId, String name, String email, String password, Role role, Timestamp createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : Role.STUDENT;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }
}

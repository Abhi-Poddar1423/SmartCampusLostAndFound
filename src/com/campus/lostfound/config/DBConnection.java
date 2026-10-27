package com.campus.lostfound.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection
 * 
 * Responsible for creating and managing JDBC database connections.
 * Reads database credentials from 'config/db.properties' so that sensitive
 * information (like passwords) is never hardcoded inside Java code.
 */
public class DBConnection {

    private static String url;
    private static String user;
    private static String password;

    // Static block runs once when the class is first loaded into memory
    static {
        loadConfiguration();
    }

    /**
     * Loads database credentials from config/db.properties.
     */
    private static void loadConfiguration() {
        Properties props = new Properties();
        File configFile = new File("config/db.properties");

        if (!configFile.exists()) {
            System.err.println("[DBConnection] Error: 'config/db.properties' file not found at " + configFile.getAbsolutePath());
            return;
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
            url = props.getProperty("db.url");
            user = props.getProperty("db.user");
            password = props.getProperty("db.password");

            // Allow runtime override via system property with clear precedence
            String sysUrl = System.getProperty("db.url");
            if (sysUrl != null && !sysUrl.trim().isEmpty()) {
                url = sysUrl.trim();
                System.out.println("[DBConnection] Runtime target override active: " + parseSafeTarget(url));
            } else {
                System.out.println("[DBConnection] Target configured from file: " + parseSafeTarget(url));
            }

            // Explicitly load MySQL JDBC driver class
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (IOException e) {
            System.err.println("[DBConnection] Failed to read database configuration: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] MySQL JDBC Driver not found in classpath. Ensure mysql-connector-j JAR is in lib/: " + e.getMessage());
        }
    }

    /**
     * Safely extracts database/catalog name and host/port from a JDBC URL for logging
     * without exposing complete URLs, credentials, or query parameters.
     */
    private static String parseSafeTarget(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            return "unknown";
        }
        try {
            String clean = jdbcUrl.trim();
            if (clean.toLowerCase().startsWith("jdbc:")) {
                clean = clean.substring(5);
            }
            if (clean.contains("://")) {
                clean = clean.substring(clean.indexOf("://") + 3);
            }
            int qIdx = clean.indexOf('?');
            if (qIdx != -1) {
                clean = clean.substring(0, qIdx);
            }
            int slashIdx = clean.lastIndexOf('/');
            if (slashIdx != -1 && slashIdx + 1 < clean.length()) {
                String dbName = clean.substring(slashIdx + 1);
                String hostPart = clean.substring(0, slashIdx);
                return dbName + " on " + hostPart;
            }
            return "database";
        } catch (Exception e) {
            return "custom-url";
        }
    }

    /**
     * Obtains a fresh database connection.
     * 
     * @return an open java.sql.Connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (url == null || user == null) {
            // Re-attempt loading if it failed initially
            loadConfiguration();
            if (url == null || user == null) {
                throw new SQLException("Database configuration is incomplete. Check config/db.properties.");
            }
        }
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Helper method to safely close a connection.
     * 
     * @param conn Connection to close
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("[DBConnection] Failed to close connection: " + e.getMessage());
            }
        }
    }
}

package com.campus.lostfound;

import com.campus.lostfound.config.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseTest
 * 
 * Stage 1 Verification Utility:
 * 1. Verifies that the JDBC connection to MySQL succeeds.
 * 2. Safely inspects (READ-ONLY) existing tables ('users', 'items', 'claims')
 *    and prints their exact columns, data types, and row counts.
 * 3. Does NOT alter, delete, or modify any database data.
 */
public class DatabaseTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   Smart Campus Lost & Found - Database Test      ");
        System.out.println("==================================================");

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("[SUCCESS] Connected to MySQL successfully!");
            System.out.println("Database Product : " + conn.getMetaData().getDatabaseProductName());
            System.out.println("Product Version  : " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("Catalog (DB Name): " + conn.getCatalog());
            System.out.println("--------------------------------------------------\n");

            String[] targetTables = {"users", "items", "claims"};
            DatabaseMetaData metaData = conn.getMetaData();

            for (String tableName : targetTables) {
                inspectTable(conn, metaData, tableName);
            }

            System.out.println("\n[SUCCESS] Stage 1 database inspection complete!");

        } catch (SQLException e) {
            System.err.println("\n[ERROR] Database connection or inspection failed:");
            System.err.println("Message: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("ErrorCode: " + e.getErrorCode());
            System.err.println("\nTip: Make sure MySQL is running and your password in 'config/db.properties' is correct.");
        }
    }

    /**
     * Inspects a single table's columns and row count using DatabaseMetaData (Read-only).
     */
    private static void inspectTable(Connection conn, DatabaseMetaData meta, String tableName) {
        System.out.println("--------------------------------------------------");
        System.out.println(" Table: `" + tableName + "`");
        System.out.println("--------------------------------------------------");

        try {
            // Check row count safely (Read-only)
            try (Statement stmt = conn.createStatement();
                 ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) FROM `" + tableName + "`")) {
                if (countRs.next()) {
                    System.out.println("  Existing Rows Count: " + countRs.getInt(1));
                }
            } catch (SQLException e) {
                System.out.println("  [Warning] Could not read row count (table might not exist): " + e.getMessage());
                return;
            }

            // Inspect columns with exact SQL data types (including ENUM definitions)
            String sql = "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, EXTRA FROM information_schema.COLUMNS " +
                         "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, conn.getCatalog());
                pstmt.setString(2, tableName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    System.out.printf("  %-20s %-35s %-10s %-15s%n", "Column Name", "Column Type", "Nullable", "Extra");
                    System.out.printf("  %-20s %-35s %-10s %-15s%n", "-----------", "-----------", "--------", "-----");

                    boolean foundColumns = false;
                    while (rs.next()) {
                        foundColumns = true;
                        String colName = rs.getString("COLUMN_NAME");
                        String colType = rs.getString("COLUMN_TYPE");
                        String isNullable = rs.getString("IS_NULLABLE");
                        String extra = rs.getString("EXTRA");
                        System.out.printf("  %-20s %-35s %-10s %-15s%n", colName, colType, isNullable, extra);
                    }

                    if (!foundColumns) {
                        System.out.println("  (No columns found for table `" + tableName + "`)");
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("  [Error] Failed inspecting table `" + tableName + "`: " + e.getMessage());
        }
        System.out.println();
    }
}

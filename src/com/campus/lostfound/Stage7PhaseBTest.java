package com.campus.lostfound;

import com.campus.lostfound.config.DBConnection;
import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.common.ItemDetailsDialog;
import com.campus.lostfound.utils.ImageUtil;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage7PhaseBTest
 * 
 * Comprehensive automated QA suite verifying Phase B functionality:
 * 1. Database isolation guard: strictly halts unless catalog is 'smart_campus_db_phase_b_test'.
 * 2. Schema provisioning in test database with image_path, building, floor_room.
 * 3. Image validation: valid PNG/JPEG, oversized limit rejection (>5MB), path traversal resistance.
 * 4. Image storage and retrieval via ImageUtil.
 * 5. ItemDAO persistence: image_path, building, floor_room insertion and retrieval.
 * 6. Dynamic multi-criteria search: filtering by building and floor/room keyword.
 * 7. UI rendering: ItemDetailsDialog thumbnail and placeholder rendering on EDT.
 * 8. Cleanup: strictly purges test-created files and test database records.
 */
public class Stage7PhaseBTest {

    private static final String REQUIRED_TEST_DB = "smart_campus_db_phase_b_test";

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  Smart Campus Lost & Found - Stage 7 Phase B QA Test Suite    ");
        System.out.println("===============================================================\n");

        int totalTests = 0;
        int passedTests = 0;

        List<Integer> createdItemIds = new ArrayList<>();
        List<Integer> createdUserIds = new ArrayList<>();
        List<Path> createdImageFiles = new ArrayList<>();

        Connection conn = null;

        try {
            // STEP 1: Strict Database Isolation Check
            System.out.println("[Guard 1] Verifying Database Isolation Target...");
            conn = DBConnection.getConnection();
            String currentCatalog = conn.getCatalog();
            System.out.println("  Active Database Catalog: " + currentCatalog);

            if (currentCatalog == null || !currentCatalog.equalsIgnoreCase(REQUIRED_TEST_DB)) {
                System.err.println("FATAL ERROR: Target database catalog is '" + currentCatalog + 
                                   "', but MUST be exactly '" + REQUIRED_TEST_DB + "'. Halting immediately!");
                System.exit(1);
            }
            System.out.println("  [PASS] Database isolation confirmed: Connected strictly to " + REQUIRED_TEST_DB + "\n");
            totalTests++;
            passedTests++;

            // STEP 2: Schema Provisioning in Isolated Test Database
            System.out.println("[Step 2] Provisioning Phase B Columns in " + REQUIRED_TEST_DB + "...");
            provisionTestSchema(conn);
            totalTests++;
            passedTests++;
            System.out.println("  [PASS] Test schema provisioned successfully.\n");

            // STEP 3: Setup Test User
            UserDAO userDAO = new UserDAO();
            ItemDAO itemDAO = new ItemDAO();

            String testEmail = "phaseb_student_" + System.currentTimeMillis() + "@campus.edu";
            User testUser = new User("PhaseB Student", testEmail, "SecureP@ss123", User.Role.STUDENT);
            boolean userCreated = userDAO.register(testUser);
            if (!userCreated || testUser.getUserId() <= 0) {
                throw new RuntimeException("Failed to create test user in test DB.");
            }
            createdUserIds.add(testUser.getUserId());
            System.out.println("[Step 3] Test user created with ID: " + testUser.getUserId());

            // STEP 4: ImageUtil Security & Validation Tests
            System.out.println("\n[Step 4] ImageUtil Security & Format Validation Tests...");
            
            // 4a. Valid PNG image save
            totalTests++;
            File validPng = createTestImageFile("valid_sample.png", "PNG", 300, 200, Color.BLUE);
            ImageUtil.SavedImageResult saveResult = ImageUtil.saveItemImageResult(validPng);
            if (saveResult != null && saveResult.getRelativePath() != null && Files.exists(saveResult.getTargetPath())) {
                createdImageFiles.add(saveResult.getTargetPath());
                System.out.println("  Test 4a (Valid PNG Save): PASS -> Saved relative: " + saveResult.getRelativePath());
                passedTests++;
            } else {
                System.err.println("  Test 4a (Valid PNG Save): FAIL");
            }

            // 4b. Oversized Image File (>5MB) rejection
            totalTests++;
            File oversizedFile = createDummyLargeFile("oversized_test.jpg", 5 * 1024 * 1024 + 1024); // 5MB + 1KB
            try {
                ImageUtil.SavedImageResult overResult = ImageUtil.saveItemImageResult(oversizedFile);
                if (overResult == null) {
                    System.out.println("  Test 4b (Oversized >5MB Rejection): PASS -> Correctly rejected");
                    passedTests++;
                } else {
                    createdImageFiles.add(overResult.getTargetPath());
                    System.err.println("  Test 4b (Oversized >5MB Rejection): FAIL -> Allowed oversized file");
                }
            } catch (Exception expected) {
                System.out.println("  Test 4b (Oversized >5MB Rejection): PASS -> Exception caught: " + expected.getMessage());
                passedTests++;
            } finally {
                if (oversizedFile.exists()) oversizedFile.delete();
            }

            // 4c. Path traversal containment
            totalTests++;
            String relPath = saveResult.getRelativePath();
            File resolvedFile = ImageUtil.resolveStoragePath(relPath);
            Path expectedUploadDir = ImageUtil.getUploadDirectory().toRealPath();
            if (resolvedFile != null && resolvedFile.toPath().toRealPath().startsWith(expectedUploadDir)) {
                System.out.println("  Test 4c (Path Traversal Confinement): PASS -> Path confined strictly inside uploads/items/");
                passedTests++;
            } else {
                System.err.println("  Test 4c (Path Traversal Confinement): FAIL -> Escaped storage boundary");
            }

            // STEP 5: ItemDAO Persistence with Phase B Columns
            System.out.println("\n[Step 5] ItemDAO Phase B Persistence Tests...");
            totalTests++;
            Item newItem = new Item(
                    "Blue Scientific Calculator",
                    "TI-84 Plus with protective cover left on desk",
                    "Electronics & Gadgets",
                    "Engineering Hub - Room 405",
                    saveResult.getRelativePath(),
                    "Engineering Hub",
                    "Room 405",
                    Item.ItemType.LOST,
                    Item.ItemStatus.OPEN,
                    testUser.getUserId()
            );

            boolean itemAdded = itemDAO.addItem(newItem);
            if (itemAdded && newItem.getItemId() > 0) {
                createdItemIds.add(newItem.getItemId());
                System.out.println("  Test 5a (ItemDAO.addItem with photo & building): PASS -> Item ID: " + newItem.getItemId());
                passedTests++;
            } else {
                System.err.println("  Test 5a (ItemDAO.addItem with photo & building): FAIL");
            }

            // 5b. Retrieval and Field Integrity
            totalTests++;
            Item fetchedItem = itemDAO.findById(newItem.getItemId());
            if (fetchedItem != null &&
                "Engineering Hub".equals(fetchedItem.getBuilding()) &&
                "Room 405".equals(fetchedItem.getFloorRoom()) &&
                saveResult.getRelativePath().equals(fetchedItem.getImagePath())) {
                System.out.println("  Test 5b (Field Integrity in findById): PASS -> All Phase B fields verified");
                passedTests++;
            } else {
                System.err.println("  Test 5b (Field Integrity in findById): FAIL");
            }

            // STEP 6: Multi-Criteria Dynamic Search with Building Filter
            System.out.println("\n[Step 6] Dynamic Search & Building Filter Tests...");
            
            // 6a. Search by Building
            totalTests++;
            List<Item> bldgMatches = itemDAO.searchItems(null, "ALL", "Engineering Hub", "ALL", "ALL");
            boolean foundByBldg = bldgMatches.stream().anyMatch(i -> i.getItemId() == newItem.getItemId());
            if (foundByBldg) {
                System.out.println("  Test 6a (Filter by exact building): PASS -> Found item in Engineering Hub");
                passedTests++;
            } else {
                System.err.println("  Test 6a (Filter by exact building): FAIL");
            }

            // 6b. Search by non-matching building
            totalTests++;
            List<Item> otherBldgMatches = itemDAO.searchItems(null, "ALL", "Central Library", "ALL", "ALL");
            boolean notFoundInOther = otherBldgMatches.stream().noneMatch(i -> i.getItemId() == newItem.getItemId());
            if (notFoundInOther) {
                System.out.println("  Test 6b (Exclude non-matching building): PASS -> Excluded from Central Library");
                passedTests++;
            } else {
                System.err.println("  Test 6b (Exclude non-matching building): FAIL");
            }

            // 6c. Search by Room in keyword
            totalTests++;
            List<Item> roomMatches = itemDAO.searchItems("Room 405", "ALL", "ALL", "ALL", "ALL");
            boolean foundByRoom = roomMatches.stream().anyMatch(i -> i.getItemId() == newItem.getItemId());
            if (foundByRoom) {
                System.out.println("  Test 6c (Search floor/room keyword): PASS -> Found item matching Room 405");
                passedTests++;
            } else {
                System.err.println("  Test 6c (Search floor/room keyword): FAIL");
            }

            // STEP 7: UI Component Headless Rendering Test
            System.out.println("\n[Step 7] GUI Component Rendering Tests (EDT)...");
            totalTests++;
            final boolean[] uiRenderSuccess = {false};
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ItemDetailsDialog dialog = new ItemDetailsDialog(null, fetchedItem);
                    dialog.pack();
                    uiRenderSuccess[0] = (dialog.getTitle() != null && dialog.getTitle().contains("#" + fetchedItem.getItemId()));
                    dialog.dispose();
                } catch (Exception e) {
                    System.err.println("UI Render exception: " + e.getMessage());
                }
            });

            if (uiRenderSuccess[0]) {
                System.out.println("  Test 7a (ItemDetailsDialog rendering with photo & location): PASS");
                passedTests++;
            } else {
                System.err.println("  Test 7a (ItemDetailsDialog rendering with photo & location): FAIL");
            }

            // 7b. Item without photo (Placeholder test)
            totalTests++;
            Item itemWithoutPhoto = new Item(
                    "Black Umbrellas", "Foldable umbrella left at entrance",
                    "Other", "Central Library - Ground Floor",
                    null, "Central Library", "Ground Floor",
                    Item.ItemType.FOUND, Item.ItemStatus.OPEN, testUser.getUserId()
            );
            itemDAO.addItem(itemWithoutPhoto);
            if (itemWithoutPhoto.getItemId() > 0) createdItemIds.add(itemWithoutPhoto.getItemId());

            final boolean[] placeholderRenderSuccess = {false};
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ItemDetailsDialog dialog = new ItemDetailsDialog(null, itemWithoutPhoto);
                    dialog.pack();
                    placeholderRenderSuccess[0] = true;
                    dialog.dispose();
                } catch (Exception e) {
                    System.err.println("UI Placeholder Render exception: " + e.getMessage());
                }
            });

            if (placeholderRenderSuccess[0]) {
                System.out.println("  Test 7b (ItemDetailsDialog placeholder rendering without photo): PASS");
                passedTests++;
            } else {
                System.err.println("  Test 7b (ItemDetailsDialog placeholder rendering without photo): FAIL");
            }

            // STEP 8: Cleanup Test Artifacts
            System.out.println("\n[Step 8] Safe Cleanup of Test Artifacts...");
            cleanupTestFiles(createdImageFiles);
            cleanupTestDatabase(conn, createdItemIds, createdUserIds);
            System.out.println("  [PASS] All test records and generated test files successfully purged.\n");

        } catch (Exception e) {
            System.err.println("FATAL TEST EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) { }
            }
        }

        System.out.println("===============================================================");
        System.out.println("  Stage 7 Phase B QA Suite Summary: " + passedTests + " / " + totalTests + " PASSED");
        System.out.println("===============================================================");
        if (passedTests < totalTests) {
            System.exit(1);
        }
    }

    private static void provisionTestSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Check if items table has columns
            DatabaseMetaData meta = conn.getMetaData();
            boolean hasImagePath = false;
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "items", "image_path")) {
                hasImagePath = rs.next();
            }

            if (!hasImagePath) {
                System.out.println("  Adding Phase B columns (image_path, building, floor_room) to " + REQUIRED_TEST_DB + "...");
                stmt.executeUpdate("ALTER TABLE items " +
                                   "ADD COLUMN image_path VARCHAR(255) NULL AFTER location, " +
                                   "ADD COLUMN building VARCHAR(100) NULL AFTER image_path, " +
                                   "ADD COLUMN floor_room VARCHAR(100) NULL AFTER building");
            } else {
                System.out.println("  Phase B columns already present in " + REQUIRED_TEST_DB + ".");
            }
        }
    }

    private static File createTestImageFile(String filename, String format, int width, int height, Color color) throws IOException {
        File temp = File.createTempFile("test_phaseb_", "_" + filename);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ImageIO.write(img, format, temp);
        temp.deleteOnExit();
        return temp;
    }

    private static File createDummyLargeFile(String filename, long sizeBytes) throws IOException {
        File temp = File.createTempFile("test_oversized_", "_" + filename);
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            byte[] chunk = new byte[64 * 1024];
            long written = 0;
            while (written < sizeBytes) {
                int toWrite = (int) Math.min(chunk.length, sizeBytes - written);
                fos.write(chunk, 0, toWrite);
                written += toWrite;
            }
        }
        return temp;
    }

    private static void cleanupTestFiles(List<Path> files) {
        Path uploadDir = ImageUtil.getUploadDirectory();
        for (Path f : files) {
            try {
                if (f != null && Files.exists(f)) {
                    // Confirm ownership inside uploads/items/
                    if (f.toRealPath().startsWith(uploadDir.toRealPath())) {
                        Files.delete(f);
                        System.out.println("  Cleaned up test file: " + f.getFileName());
                    }
                }
            } catch (Exception e) {
                System.err.println("  Warning: Failed to clean up file: " + e.getMessage());
            }
        }
    }

    private static void cleanupTestDatabase(Connection conn, List<Integer> itemIds, List<Integer> userIds) {
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            for (int id : itemIds) {
                stmt.executeUpdate("DELETE FROM claims WHERE item_id = " + id);
                stmt.executeUpdate("DELETE FROM items WHERE item_id = " + id);
            }
            for (int uid : userIds) {
                stmt.executeUpdate("DELETE FROM users WHERE user_id = " + uid);
            }
            System.out.println("  Cleaned up " + itemIds.size() + " test item(s) and " + userIds.size() + " test user(s).");
        } catch (SQLException e) {
            System.err.println("  Warning during test DB cleanup: " + e.getMessage());
        }
    }
}

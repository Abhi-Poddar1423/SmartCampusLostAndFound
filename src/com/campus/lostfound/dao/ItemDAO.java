package com.campus.lostfound.dao;

import com.campus.lostfound.config.DBConnection;
import com.campus.lostfound.models.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemDAO (Data Access Object)
 * 
 * Handles database operations for the 'items' table using PreparedStatements.
 * Supports reporting items, searching with filters, updating statuses, and admin moderation.
 */
public class ItemDAO {

    // Cached schema check to ensure backward compatibility before and after migration
    private static volatile Boolean v2ColumnsAvailable = null;

    private static boolean areV2ColumnsPresent(Connection conn) {
        if (v2ColumnsAvailable != null) {
            return v2ColumnsAvailable;
        }
        synchronized (ItemDAO.class) {
            if (v2ColumnsAvailable != null) {
                return v2ColumnsAvailable;
            }
            try {
                java.sql.DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "items", "image_path")) {
                    v2ColumnsAvailable = rs.next();
                }
            } catch (SQLException e) {
                v2ColumnsAvailable = false;
            }
        }
        return v2ColumnsAvailable != null && v2ColumnsAvailable;
    }

    private String getSelectColumns(boolean v2) {
        if (v2) {
            return "i.item_id, i.title, i.description, i.category, i.location, i.image_path, i.building, i.floor_room, " +
                   "i.item_type, i.status, i.reported_by, i.report_date, u.name AS reporter_name ";
        } else {
            return "i.item_id, i.title, i.description, i.category, i.location, " +
                   "i.item_type, i.status, i.reported_by, i.report_date, u.name AS reporter_name ";
        }
    }

    /**
     * Reports a new lost or found item.
     * Schema-adaptive: persists image_path, building, floor_room if columns exist,
     * or seamlessly falls back to legacy columns if unmigrated.
     * 
     * @param item Item object containing report details
     * @return true if item was saved successfully, false otherwise
     */
    public boolean addItem(Item item) {
        try (Connection conn = DBConnection.getConnection()) {
            boolean v2 = areV2ColumnsPresent(conn);
            String sql;
            if (v2) {
                sql = "INSERT INTO items (title, description, category, location, image_path, building, floor_room, item_type, status, reported_by) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            } else {
                sql = "INSERT INTO items (title, description, category, location, item_type, status, reported_by) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?)";
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, item.getTitle());
                pstmt.setString(2, item.getDescription());
                pstmt.setString(3, item.getCategory());
                pstmt.setString(4, item.getLocation());

                if (v2) {
                    pstmt.setString(5, item.getImagePath());
                    pstmt.setString(6, item.getBuilding());
                    pstmt.setString(7, item.getFloorRoom());
                    pstmt.setString(8, item.getItemType().name());
                    pstmt.setString(9, item.getStatus().name());
                    pstmt.setInt(10, item.getReportedBy());
                } else {
                    pstmt.setString(5, item.getItemType().name()); // Matches ENUM('LOST','FOUND')
                    pstmt.setString(6, item.getStatus().name());   // Matches ENUM('OPEN','CLAIMED','RETURNED')
                    pstmt.setInt(7, item.getReportedBy());
                }

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            item.setItemId(keys.getInt(1));
                        }
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error adding item: " + e.getMessage());
        }
        return false;
    }

    /**
     * Finds an item by its ID, joining with the 'users' table to fetch reporter's name.
     * 
     * @param itemId Item ID
     * @return Item object if found, or null
     */
    public Item findById(int itemId) {
        try (Connection conn = DBConnection.getConnection()) {
            boolean v2 = areV2ColumnsPresent(conn);
            String sql = "SELECT " + getSelectColumns(v2) +
                         "FROM items i " +
                         "LEFT JOIN users u ON i.reported_by = u.user_id " +
                         "WHERE i.item_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, itemId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToItem(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error finding item by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all items ordered newest first.
     * 
     * @return List of all items
     */
    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            boolean v2 = areV2ColumnsPresent(conn);
            String sql = "SELECT " + getSelectColumns(v2) +
                         "FROM items i " +
                         "LEFT JOIN users u ON i.reported_by = u.user_id " +
                         "ORDER BY i.report_date DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error fetching all items: " + e.getMessage());
        }
        return items;
    }

    /**
     * Dynamic search and filter across reported items.
     * Legacy compatible: defaults building to "ALL".
     */
    public List<Item> searchItems(String keyword, String category, String itemType, String status) {
        return searchItems(keyword, category, "ALL", itemType, status);
    }

    /**
     * Extended dynamic search and filter across reported items including building.
     * 
     * @param keyword Partial match on title, description, location, building, or floor/room
     * @param category Exact match category (pass null or "ALL" to ignore)
     * @param building Exact match building (pass null or "ALL" to ignore)
     * @param itemType Exact match on 'LOST' or 'FOUND' (pass null or "ALL" to ignore)
     * @param status Exact match on 'OPEN', 'CLAIMED', or 'RETURNED' (pass null or "ALL" to ignore)
     * @return Filtered list of items
     */
    public List<Item> searchItems(String keyword, String category, String building, String itemType, String status) {
        List<Item> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            boolean v2 = areV2ColumnsPresent(conn);
            StringBuilder sql = new StringBuilder(
                    "SELECT " + getSelectColumns(v2) +
                    "FROM items i " +
                    "LEFT JOIN users u ON i.reported_by = u.user_id " +
                    "WHERE 1=1 "
            );

            List<Object> params = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                if (v2) {
                    sql.append("AND (LOWER(i.title) LIKE ? OR LOWER(i.description) LIKE ? OR LOWER(i.location) LIKE ? OR LOWER(i.building) LIKE ? OR LOWER(i.floor_room) LIKE ?) ");
                    String kwParam = "%" + keyword.trim().toLowerCase() + "%";
                    params.add(kwParam);
                    params.add(kwParam);
                    params.add(kwParam);
                    params.add(kwParam);
                    params.add(kwParam);
                } else {
                    sql.append("AND (LOWER(i.title) LIKE ? OR LOWER(i.description) LIKE ? OR LOWER(i.location) LIKE ?) ");
                    String kwParam = "%" + keyword.trim().toLowerCase() + "%";
                    params.add(kwParam);
                    params.add(kwParam);
                    params.add(kwParam);
                }
            }

            if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL")) {
                sql.append("AND i.category = ? ");
                params.add(category.trim());
            }

            if (v2 && building != null && !building.trim().isEmpty() && !building.equalsIgnoreCase("ALL")) {
                sql.append("AND i.building = ? ");
                params.add(building.trim());
            }

            if (itemType != null && !itemType.trim().isEmpty() && !itemType.equalsIgnoreCase("ALL")) {
                sql.append("AND i.item_type = ? ");
                params.add(itemType.trim().toUpperCase());
            }

            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
                sql.append("AND i.status = ? ");
                params.add(status.trim().toUpperCase());
            }

            sql.append("ORDER BY i.report_date DESC");

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapResultSetToItem(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error searching items: " + e.getMessage());
        }
        return items;
    }

    /**
     * Retrieves all items reported by a specific user.
     * 
     * @param userId User's ID
     * @return List of items reported by the given user
     */
    public List<Item> getItemsByReporter(int userId) {
        List<Item> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            boolean v2 = areV2ColumnsPresent(conn);
            String sql = "SELECT " + getSelectColumns(v2) +
                         "FROM items i " +
                         "LEFT JOIN users u ON i.reported_by = u.user_id " +
                         "WHERE i.reported_by = ? " +
                         "ORDER BY i.report_date DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapResultSetToItem(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error fetching user's reported items: " + e.getMessage());
        }
        return items;
    }

    /**
     * Updates an item's status (OPEN, CLAIMED, RETURNED).
     * 
     * @param itemId Item ID
     * @param newStatus New status to set
     * @return true if updated successfully, false otherwise
     */
    public boolean updateStatus(int itemId, Item.ItemStatus newStatus) {
        String sql = "UPDATE items SET status = ? WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, itemId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error updating item status: " + e.getMessage());
        }
        return false;
    }

    /**
     * Deletes an item by ID.
     * 
     * @param itemId Item ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteItem(int itemId) {
        String sql = "DELETE FROM items WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error deleting item: " + e.getMessage());
        }
        return false;
    }

    /**
     * Safely deletes an item and any associated claims within a single transaction.
     * Prevents foreign key constraint violations while preserving data integrity.
     * 
     * @param itemId Item ID to delete
     * @return true if both item and any related claims were deleted, false otherwise
     */
    public boolean deleteItemWithClaims(int itemId) {
        String deleteClaimsSql = "DELETE FROM claims WHERE item_id = ?";
        String deleteItemSql = "DELETE FROM items WHERE item_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Delete associated claims first
            try (PreparedStatement pClaims = conn.prepareStatement(deleteClaimsSql)) {
                pClaims.setInt(1, itemId);
                pClaims.executeUpdate();
            }

            // 2. Delete the item record
            int rowsDeleted;
            try (PreparedStatement pItem = conn.prepareStatement(deleteItemSql)) {
                pItem.setInt(1, itemId);
                rowsDeleted = pItem.executeUpdate();
            }

            conn.commit(); // Commit transaction
            return rowsDeleted > 0;

        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error deleting item with claims: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[ItemDAO] Rollback failed: " + rollbackEx.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) { }
            }
        }
    }

    /**
     * Counts how many claims have been submitted for a specific item.
     */
    public int getItemClaimsCount(int itemId) {
        String sql = "SELECT COUNT(*) FROM claims WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error counting item claims: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Counts total reported LOST items.
     */
    public int getLostItemsCount() {
        return countByCondition("item_type = 'LOST'");
    }

    /**
     * Counts total reported FOUND items.
     */
    public int getFoundItemsCount() {
        return countByCondition("item_type = 'FOUND'");
    }

    /**
     * Counts items with OPEN status.
     */
    public int getOpenItemsCount() {
        return countByCondition("status = 'OPEN'");
    }

    private int countByCondition(String condition) {
        String sql = "SELECT COUNT(*) FROM items WHERE " + condition;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error executing count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Retrieves eligible candidate items for smart matching.
     * Selects OPEN items of the opposite item type (LOST <-> FOUND),
     * excluding self-reported items and the target item itself.
     * Uses parameterized PreparedStatement without loading unnecessary rows.
     *
     * @param targetType Target item's type (LOST or FOUND)
     * @param excludeUserId User ID of the target item's reporter
     * @param excludeItemId Item ID of the target item itself
     * @return List of eligible candidate items
     */
    public List<Item> getEligibleCandidateItems(Item.ItemType targetType, int excludeUserId, int excludeItemId) {
        List<Item> items = new ArrayList<>();
        Item.ItemType candidateType = (targetType == Item.ItemType.LOST) ? Item.ItemType.FOUND : Item.ItemType.LOST;

        try (Connection conn = DBConnection.getConnection()) {
            boolean v2 = areV2ColumnsPresent(conn);
            String sql = "SELECT " + getSelectColumns(v2) +
                         "FROM items i " +
                         "LEFT JOIN users u ON i.reported_by = u.user_id " +
                         "WHERE i.item_type = ? " +
                         "  AND i.status = 'OPEN' " +
                         "  AND i.reported_by != ? " +
                         "  AND i.item_id != ? " +
                         "ORDER BY i.report_date DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, candidateType.name());
                pstmt.setInt(2, excludeUserId);
                pstmt.setInt(3, excludeItemId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapResultSetToItem(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error fetching eligible candidate match items: " + e.getMessage());
        }
        return items;
    }

    /**
     * Helper method to map a ResultSet row to an Item model.
     */
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setItemId(rs.getInt("item_id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setCategory(rs.getString("category"));
        item.setLocation(rs.getString("location"));
        item.setItemType(Item.ItemType.fromString(rs.getString("item_type")));
        item.setStatus(Item.ItemStatus.fromString(rs.getString("status")));
        item.setReportedBy(rs.getInt("reported_by"));
        item.setReportDate(rs.getTimestamp("report_date"));

        // Extended details (schema-adaptive fallback)
        try {
            item.setImagePath(rs.getString("image_path"));
        } catch (SQLException ignored) { }
        try {
            item.setBuilding(rs.getString("building"));
        } catch (SQLException ignored) { }
        try {
            item.setFloorRoom(rs.getString("floor_room"));
        } catch (SQLException ignored) { }

        // Reporter name from JOIN
        try {
            item.setReporterName(rs.getString("reporter_name"));
        } catch (SQLException ignored) { }

        return item;
    }
}

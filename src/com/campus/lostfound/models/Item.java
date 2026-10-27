package com.campus.lostfound.models;

import java.sql.Timestamp;

/**
 * Item Model
 * 
 * Represents a lost or found item record from the MySQL 'items' table.
 * Matches exact database schema:
 * - item_id     INT (Primary Key, Auto Increment)
 * - title       VARCHAR(150) NOT NULL
 * - description TEXT
 * - category    VARCHAR(80)
 * - location    VARCHAR(150)
 * - item_type   ENUM('LOST', 'FOUND') NOT NULL
 * - status      ENUM('OPEN', 'CLAIMED', 'RETURNED')
 * - reported_by INT NOT NULL (Foreign Key referencing users.user_id)
 * - report_date TIMESTAMP
 */
public class Item {

    // Enum matching MySQL: enum('LOST', 'FOUND')
    public enum ItemType {
        LOST,
        FOUND;

        public static ItemType fromString(String typeStr) {
            if (typeStr == null) return LOST;
            try {
                return ItemType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return LOST;
            }
        }
    }

    // Enum matching MySQL: enum('OPEN', 'CLAIMED', 'RETURNED')
    public enum ItemStatus {
        OPEN,
        CLAIMED,
        RETURNED;

        public static ItemStatus fromString(String statusStr) {
            if (statusStr == null) return OPEN;
            try {
                return ItemStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return OPEN;
            }
        }
    }

    private int itemId;
    private String title;
    private String description;
    private String category;
    private String location;
    private String imagePath;
    private String building;
    private String floorRoom;
    private ItemType itemType;
    private ItemStatus status;
    private int reportedBy;
    private Timestamp reportDate;

    // Optional joined field for convenient GUI display (e.g., student name who reported)
    private String reporterName;

    public Item() {
        this.itemType = ItemType.LOST;
        this.status = ItemStatus.OPEN;
    }

    // Constructor for reporting a new item (before insertion into DB) - Legacy compatible
    public Item(String title, String description, String category, String location,
                ItemType itemType, ItemStatus status, int reportedBy) {
        this(title, description, category, location, null, null, null, itemType, status, reportedBy);
    }

    // Constructor for reporting a new item with photo and structured location
    public Item(String title, String description, String category, String location,
                String imagePath, String building, String floorRoom,
                ItemType itemType, ItemStatus status, int reportedBy) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.imagePath = imagePath;
        this.building = building;
        this.floorRoom = floorRoom;
        this.itemType = itemType != null ? itemType : ItemType.LOST;
        this.status = status != null ? status : ItemStatus.OPEN;
        this.reportedBy = reportedBy;
    }

    // Full constructor (retrieval from database) - Legacy compatible
    public Item(int itemId, String title, String description, String category, String location,
                ItemType itemType, ItemStatus status, int reportedBy, Timestamp reportDate) {
        this(itemId, title, description, category, location, null, null, null, itemType, status, reportedBy, reportDate);
    }

    // Full constructor with photo and structured location
    public Item(int itemId, String title, String description, String category, String location,
                String imagePath, String building, String floorRoom,
                ItemType itemType, ItemStatus status, int reportedBy, Timestamp reportDate) {
        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.imagePath = imagePath;
        this.building = building;
        this.floorRoom = floorRoom;
        this.itemType = itemType != null ? itemType : ItemType.LOST;
        this.status = status != null ? status : ItemStatus.OPEN;
        this.reportedBy = reportedBy;
        this.reportDate = reportDate;
    }

    // Getters and Setters
    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public int getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(int reportedBy) {
        this.reportedBy = reportedBy;
    }

    public Timestamp getReportDate() {
        return reportDate;
    }

    public void setReportDate(Timestamp reportDate) {
        this.reportDate = reportDate;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloorRoom() {
        return floorRoom;
    }

    public void setFloorRoom(String floorRoom) {
        this.floorRoom = floorRoom;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    @Override
    public String toString() {
        return "Item{" +
                "itemId=" + itemId +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", location='" + location + '\'' +
                ", itemType=" + itemType +
                ", status=" + status +
                ", reportedBy=" + reportedBy +
                ", reportDate=" + reportDate +
                '}';
    }
}

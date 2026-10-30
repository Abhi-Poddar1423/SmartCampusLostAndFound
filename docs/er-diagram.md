# Entity-Relationship (ER) Diagram: Smart Campus Lost and Found

This document specifies the database structure, table definitions, column specifications, constraints, and relational mappings for the MySQL database `smart_campus_db`.

> **Implementation Note on Schema Migrations:**  
> The core schema comprises `users`, `items`, and `claims`. Phase B item enhancements (`image_path`, `building`, `floor_room`) are supported at the DAO layer with dynamic fallback if columns are unmigrated. Feature 7 introduces the `notifications` table; its migration script is provided in `sql/migration_feature7_notifications.sql` and is currently pending manual database execution.

---

## 1. Mermaid Entity-Relationship Diagram

```mermaid
erDiagram
    USERS ||--o{ ITEMS : "reports (1:N)"
    USERS ||--o{ CLAIMS : "submits (1:N)"
    USERS ||--o{ NOTIFICATIONS : "receives (1:N)"
    ITEMS ||--o{ CLAIMS : "associated with (1:N)"

    USERS {
        int user_id PK "Auto Increment"
        varchar(100) name "Full name"
        varchar(100) email "Unique college email"
        varchar(255) password "PBKDF2 salted hash"
        enum role "STUDENT, ADMIN"
        timestamp created_at "Registration timestamp"
    }

    ITEMS {
        int item_id PK "Auto Increment"
        varchar(150) title "Item headline/name"
        text description "Detailed physical marks"
        varchar(80) category "Electronics, ID, Bags, etc."
        varchar(150) location "Campus incident spot"
        varchar(255) image_path "Relative photo path (uploads/items/...)"
        varchar(100) building "Campus building identifier"
        varchar(100) floor_room "Specific floor or room"
        enum item_type "LOST, FOUND"
        enum status "OPEN, CLAIMED, RETURNED"
        int reported_by FK "References USERS.user_id"
        timestamp report_date "Report submission timestamp"
    }

    CLAIMS {
        int claim_id PK "Auto Increment"
        int item_id FK "References ITEMS.item_id"
        int claimed_by FK "References USERS.user_id"
        varchar(500) message "Ownership proof details"
        enum status "PENDING, APPROVED, REJECTED"
        timestamp claim_date "Claim submission timestamp"
    }

    NOTIFICATIONS {
        int notification_id PK "Auto Increment"
        int user_id FK "References USERS.user_id"
        varchar(150) title "Notification title"
        text message "Body text"
        varchar(50) notification_type "CLAIM_SUBMITTED, CLAIM_APPROVED, etc."
        varchar(50) reference_type "CLAIM, ITEM"
        int reference_id "Referenced record ID"
        varchar(191) dedup_key UK "Unique deduplication key"
        boolean is_read "Read/unread indicator"
        timestamp created_at "Notification timestamp"
    }
```

---

## 2. Table Specifications & Data Dictionary

### Table 1: `users`
Stores student and administrator credentials and permissions.

| Column Name | Data Type | Nullable | Key | Default / Extra | Description |
| :--- | :--- | :---: | :---: | :--- | :--- |
| `user_id` | `INT` | NO | **PK** | `AUTO_INCREMENT` | Unique identifier for each campus user. |
| `name` | `VARCHAR(100)` | NO | | | Full name of the user. |
| `email` | `VARCHAR(100)` | NO | **UK** | | Unique college email address used for authentication. |
| `password` | `VARCHAR(255)` | NO | | | PBKDF2 hash formatted as `iterations:salt:hash`. |
| `role` | `ENUM('STUDENT','ADMIN')` | YES | | `'STUDENT'` | Security access tier determining accessible dashboards. |
| `created_at` | `TIMESTAMP` | YES | | `CURRENT_TIMESTAMP` | Account creation timestamp. |

---

### Table 2: `items`
Stores reports of items that have been either lost or found across campus, including structured building locations and photo paths.

| Column Name | Data Type | Nullable | Key | Default / Extra | Description |
| :--- | :--- | :---: | :---: | :--- | :--- |
| `item_id` | `INT` | NO | **PK** | `AUTO_INCREMENT` | Unique identifier for each reported item. |
| `title` | `VARCHAR(150)` | NO | | | Concise descriptive name of the item. |
| `description` | `TEXT` | YES | | | Detailed description, colors, condition, contents. |
| `category` | `VARCHAR(80)` | YES | | | Categorical grouping (e.g., Electronics, ID Card). |
| `location` | `VARCHAR(150)` | YES | | | General campus location description. |
| `image_path` | `VARCHAR(255)` | YES | | `NULL` | Relative path to uploaded photo in `uploads/items/`. |
| `building` | `VARCHAR(100)` | YES | | `NULL` | Campus building (e.g., Library, Academic Block A). |
| `floor_room` | `VARCHAR(100)` | YES | | `NULL` | Specific floor or room details. |
| `item_type` | `ENUM('LOST','FOUND')` | NO | | | Classification of the item report. |
| `status` | `ENUM('OPEN','CLAIMED','RETURNED')` | YES | | `'OPEN'` | Current lifecycle status of the item. |
| `reported_by` | `INT` | NO | **FK** | | Foreign key referencing `users(user_id)`. |
| `report_date` | `TIMESTAMP` | YES | | `CURRENT_TIMESTAMP` | Timestamp when the item was submitted. |

---

### Table 3: `claims`
Stores ownership claims filed by students against found items.

| Column Name | Data Type | Nullable | Key | Default / Extra | Description |
| :--- | :--- | :---: | :---: | :--- | :--- |
| `claim_id` | `INT` | NO | **PK** | `AUTO_INCREMENT` | Unique claim identifier. |
| `item_id` | `INT` | NO | **FK** | | Foreign key referencing `items(item_id)`. |
| `claimed_by` | `INT` | NO | **FK** | | Foreign key referencing `users(user_id)`. |
| `message` | `VARCHAR(500)` | YES | | | Specific proof of ownership submitted by student. |
| `status` | `ENUM('PENDING','APPROVED','REJECTED')` | YES | | `'PENDING'` | Administrative review verdict. |
| `claim_date` | `TIMESTAMP` | YES | | `CURRENT_TIMESTAMP` | Timestamp when claim was submitted. |

---

### Table 4: `notifications`
*(Pending manual database execution via `sql/migration_feature7_notifications.sql`)*  
Stores user-scoped in-app notifications generated by claim submissions, approvals, rejections, and item lifecycle status transitions.

| Column Name | Data Type | Nullable | Key | Default / Extra | Description |
| :--- | :--- | :---: | :---: | :--- | :--- |
| `notification_id` | `INT` | NO | **PK** | `AUTO_INCREMENT` | Unique notification identifier. |
| `user_id` | `INT` | NO | **FK** | | Recipient foreign key referencing `users(user_id)`. |
| `title` | `VARCHAR(150)` | NO | | | Notification headline. |
| `message` | `TEXT` | NO | | | Detailed notification body text. |
| `notification_type` | `VARCHAR(50)` | NO | | | Type: `CLAIM_SUBMITTED`, `CLAIM_APPROVED`, `CLAIM_REJECTED`, `ITEM_RETURNED`, `ITEM_STATUS_CHANGED`. |
| `reference_type` | `VARCHAR(50)` | YES | | `NULL` | Business entity type (e.g., `CLAIM` or `ITEM`). |
| `reference_id` | `INT` | YES | | `NULL` | Target record ID in referenced table. |
| `dedup_key` | `VARCHAR(191)` | YES | **UK** | `NULL` | Unique key preventing duplicate notifications during concurrent requests. |
| `is_read` | `BOOLEAN` | NO | | `FALSE` | Read status indicator. |
| `created_at` | `TIMESTAMP` | NO | | `CURRENT_TIMESTAMP` | Timestamp when notification was generated. |

---

## 3. Referential Integrity & Relational Constraints

1. **`items.reported_by` → `users.user_id`**:
   - `ON DELETE RESTRICT`: Prevents deleting a user account if that user has active item reports. Protects the system against orphaned item reports.
2. **`claims.item_id` → `items.item_id`**:
   - `ON DELETE CASCADE`: If an item report is permanently purged by an administrator, any associated ownership claims are cleaned up in an atomic transaction.
3. **`claims.claimed_by` → `users.user_id`**:
   - `ON DELETE RESTRICT`: Prevents deleting a student user account if they have submitted claims pending or approved in the system.
4. **`notifications.user_id` → `users.user_id`**:
   - `ON DELETE CASCADE`: If a user account is removed, all associated in-app notifications are automatically removed.
5. **Deduplication Constraint**:
   - `UNIQUE KEY uq_notifications_dedup (dedup_key)`: Database-enforced uniqueness guarantee preventing duplicate notifications across concurrent threads.

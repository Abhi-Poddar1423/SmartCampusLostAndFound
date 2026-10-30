# College Project Report: Smart Campus Lost and Found System

**Course:** B.Tech Computer Science & Engineering (3rd Semester)  
**Project Title:** Smart Campus Lost and Found Management System  
**Technology Stack:** Java (JDK 21/26), Java Swing, MySQL 8.0, JDBC, FlatLaf  
**Architecture:** 3-Tier Layered Architecture (Model-View-DAO)  

> **Implementation & Verification Status:**  
> All 7 core modules and the custom vector UI theme are fully implemented in the Java source codebase and compile cleanly. The Feature 7 `notifications` table migration script is provided in `sql/migration_feature7_notifications.sql` and is pending manual database execution. Full end-to-end runtime test execution is scheduled for final verification.

---

## Abstract

On university and college campuses with thousands of active students, faculty, and visitors, personal items such as calculators, student identity cards, electronic devices, bags, and notebooks are frequently misplaced. The conventional approach of logging items into physical registers, posting on informal social messaging groups, or displaying paper notices on bulletin boards suffers from lack of searchability, absence of ownership verification, risks of fraudulent claims, and delayed return times.

This project, **Smart Campus Lost and Found**, presents a desktop enterprise application developed in Java Swing and backed by a relational MySQL database via JDBC. It enforces strict separation of concerns through the Model-View-DAO pattern, keeps user interfaces non-blocking and responsive using `SwingWorker` multithreading, and secures authentication credentials using PBKDF2 with cryptographic salting. The system provides role-based workspaces: students can report lost or found items, upload scaled photos, search with multi-criteria filters, evaluate smart similarity matches, submit claims with confidential proof, and manage in-app notifications; administrators can moderate items, oversee accounts, review claim proof, and atomically synchronize item lifecycles upon claim approval.

---

## Chapter 1: Introduction

### 1.1 Problem Statement
Educational institutions lack an integrated, secure, and searchable platform to track lost and found belongings. Current informal systems result in:
1. Low recovery rate of lost valuables.
2. Inability to search or filter lost items across departments and locations.
3. Vulnerability to fraudulent claims without verifiable ownership proof.
4. Administrative burden in tracking physical logs and conflicting claims.

### 1.2 Objectives
- Build a centralized, secure Java Swing desktop portal for students and campus authorities.
- Ensure responsive user experience during network and database I/O through asynchronous worker threads (`SwingWorker`).
- Prevent credential theft using cryptographic password hashing (`PBKDF2WithHmacSHA256`).
- Implement atomic database transactions for claim approvals and cascading item deletions to guarantee data consistency.
- Eliminate SQL injection risks through 100% parameterized `PreparedStatement` usage.
- Provide a deterministic similarity matching engine to connect lost and found reports.
- Implement an in-app notification center to track claim events and status changes.

---

## Chapter 2: Seven Implemented Core Features

### Feature 1: User Registration, Authentication & Role-Based Routing
- Secure student account creation with name, unique institutional email, and password validation.
- Cryptographic password protection using `PBKDF2WithHmacSHA256` with secure random salts (65,536 iterations).
- Dynamic session management via `SessionManager` routing users to either `StudentDashboard` or `AdminDashboard`.

### Feature 2: Item Reporting & Photo Attachment
- Comprehensive item intake form capturing title, category, description, and structured campus location (building, floor/room).
- Photo attachment via `ImageUtil` with client-side image scaling to standard dimensions, stored on disk in `uploads/items/` with relative paths persisted in MySQL.
- Cancellation cleanup safeguarding temporary photo files.

### Feature 3: Dynamic Item Search, Filtering & Browsing
- Real-time search by keyword matching against item title, description, building, or room.
- Filter criteria across category, building, item classification (`LOST` or `FOUND`), and lifecycle status (`OPEN`, `CLAIMED`, `RETURNED`).
- Visual card grid rendering item badges, photo indicators, and full inspection in `ItemDetailsDialog`.

### Feature 4: Ownership Claim Workflow
- Structured claiming modal enabling students to submit detailed ownership proof for found items.
- Business rule enforcement: students cannot file claims on their own reports; duplicate claims on the same item are blocked; claims can only be filed on `OPEN` items.
- Student claim tracking in `MyClaimsPanel` showing administrative review decisions.

### Feature 5: Administrative Moderation & Atomic Adjudication
- Centralized administrator dashboard featuring live KPI counters (total items, active users, open cases, pending claims).
- Claim proof adjudication modal with atomic database transaction (`ClaimDAO.approveClaimTransaction`):
  - Sets selected claim to `APPROVED`.
  - Automatically updates item lifecycle status to `CLAIMED`.
  - Automatically rejects all competing pending claims on that item.
  - Generates in-transaction notifications for all involved parties with automatic rollback on collision.

### Feature 6: Item Matching Engine
- Pure Java deterministic similarity calculation between target reports and opposite-type open candidates (`ItemMatchingEngine`).
- Transparent weighted scoring: Category match (40%), Location spatial match (25%), and Text similarity via Dice coefficient (35%).
- Interactive score breakdown dialog (`MatchResultsDialog`) allowing students to review matching reports.

### Feature 7: In-App Notification System
- User-scoped notification center (`NotificationsPanel`) providing real-time alerts for claim submissions, approvals, rejections, and status transitions.
- Concurrency-safe deduplication via unique constraint `uq_notifications_dedup (dedup_key)` preventing duplicate alert generation.
- Dynamic unread count badge in student sidebar and action bar bell icon.
- *(Database table migration script prepared at `sql/migration_feature7_notifications.sql`, pending manual database execution)*.

---

## Chapter 3: Software Requirements Specification (SRS)

### 3.1 Functional Requirements (FR)
- **FR-1 (Registration)**: Students register with full name, college email, and password. Public registration is locked to the `STUDENT` role.
- **FR-2 (Authentication)**: Students and administrators authenticate; dynamic role routing opens the respective workspace.
- **FR-3 (Item Reporting)**: Students report items with metadata, structured building locations, and optional photo uploads.
- **FR-4 (Search & Filter)**: Multi-parameter search across text, category, building, item type, and status.
- **FR-5 (Claim Submission)**: Students submit ownership claims with confidential proof for open found items.
- **FR-6 (Claim Adjudication)**: Administrators approve or reject claims with atomic transaction integrity.
- **FR-7 (Item Similarity)**: Deterministic matching engine scores lost vs. found reports without cloud or AI dependencies.
- **FR-8 (Notifications)**: Users receive and manage unread notifications for claim decisions and report lifecycle transitions.
- **FR-9 (Account Governance)**: Administrators can promote or demote user roles, with safeguards protecting the active admin and the last administrator.

### 3.2 Non-Functional Requirements (NFR)
- **NFR-1 (Performance & Responsiveness)**: All database queries run asynchronously via `SwingWorker` to prevent EDT thread blocking.
- **NFR-2 (Security)**: Passwords hashed via PBKDF2; constant-time digest verification; complete parameterization of SQL statements.
- **NFR-3 (Data Integrity)**: Foreign-key cascades and explicit transaction rollbacks guarantee relational consistency.
- **NFR-4 (Usability)**: Modern FlatLaf university theme in navy and teal with custom anti-aliased Java 2D vector icons (`CampusIcons`).

---

## Chapter 4: System Architecture & Database Design

### 4.1 3-Tier Layered Architecture
1. **Presentation Layer (`com.campus.lostfound.ui`)**:
   - `auth`: `LoginFrame`, `RegisterFrame`
   - `student`: `StudentDashboard`, `StudentHomeOverviewPanel`, `ItemBrowsePanel`, `MyReportedItemsPanel`, `MyClaimsPanel`, `NotificationsPanel`, dialogs
   - `admin`: `AdminDashboard`, `AdminHomeOverviewPanel`, `ManageUsersPanel`, `ManageItemsPanel`, `ManageClaimsPanel`
   - `common`: `UITheme`, `RoundedButton`, `CampusIcons`, `CampusLogo`, `ItemDetailsDialog`, `MatchResultsDialog`
2. **Business Logic & DAO Layer (`com.campus.lostfound.dao`, `utils`)**:
   - `UserDAO`, `ItemDAO`, `ClaimDAO`, `NotificationDAO`
   - `ItemMatchingEngine`, `PasswordUtil`, `ValidationUtil`, `SessionManager`, `ImageUtil`
3. **Persistence Layer (`config`, MySQL)**:
   - `DBConnection` loading `config/db.properties`
   - Relational tables: `users`, `items`, `claims`, `notifications`

---

## Chapter 5: Conclusion & Next Steps

The **Smart Campus Lost and Found System** demonstrates a complete, secure, and production-grade desktop solution for higher education institutions. All seven planned capabilities—from authentication to deterministic item matching and the in-app notification system—are fully implemented in the source code.

**Pending Actions:**
1. Execute the `sql/migration_feature7_notifications.sql` script on the target MySQL instance to enable notification persistence.
2. Conduct full end-to-end runtime GUI and integration testing.

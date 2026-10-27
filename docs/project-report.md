# College Project Report: Smart Campus Lost and Found System

**Course:** B.Tech Computer Science & Engineering (3rd Semester)  
**Project Title:** Smart Campus Lost and Found Management System  
**Technology Stack:** Java (JDK 21/26), Java Swing, MySQL 8.0, JDBC, FlatLaf  
**Architecture:** 3-Tier Layered Architecture (Model-View-DAO)  

---

## Abstract

On university and college campuses with thousands of active students, faculty, and visitors, personal items such as calculators, student identity cards, electronic devices, bags, and notebooks are frequently misplaced. The conventional approach of logging items into physical registers, posting on informal social messaging groups, or displaying paper notices on bulletin boards suffers from lack of searchability, absence of ownership verification, risks of fraudulent claims, and delayed return times.

This project, **Smart Campus Lost and Found**, presents a desktop application developed in Java Swing and backed by a relational MySQL database via JDBC. It enforces strict separation of concerns through the Model-View-DAO pattern, keeps user interfaces non-blocking and responsive using `SwingWorker` multithreading, and secures authentication credentials using PBKDF2 with cryptographic salting. The system provides role-based workspaces: students can report lost or found items, browse items with multi-criteria search, submit claims with ownership proof, and track claim status; administrators can moderate items, oversee accounts, review claim proof, and atomically synchronize item lifecycles upon claim approval.

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
- Ensure responsive user experience during network and database I/O through asynchronous worker threads.
- Prevent credential theft using cryptographic password hashing (PBKDF2WithHmacSHA256).
- Implement atomic database transactions for claim approvals and cascading item deletions to guarantee data consistency.
- Eliminate SQL injection risks through 100% parameterized `PreparedStatement` usage.

### 1.3 Scope of the System
The application is scoped for local campus deployment across campus computers, student kiosks, and administration/security offices, operating against an institutional MySQL database server.

---

## Chapter 2: Literature Survey & Comparative Analysis

| Parameter | Manual Logbooks | Campus Social Media Groups | Smart Campus Lost & Found |
| :--- | :--- | :--- | :--- |
| **Searchability** | Sequential paper search | Unstructured text search | Multi-filter keyword & category search |
| **Authentication** | None | Personal social accounts | College email verified accounts |
| **Proof Validation** | Subjective / Verbal | Open comments (visible to all) | Confidential proof message to admin |
| **Conflicting Claims** | Difficult to arbitrate | Frequent disputes | Automated atomic resolution & rejection |
| **Audit Trail** | Vulnerable to damage | Easily lost in group history | Permanent timestamped database records |

---

## Chapter 3: Software Requirements Specification (SRS)

### 3.1 Functional Requirements (FR)
- **FR-1 (Registration)**: Students can register with full name, college email, and password. Public registration is locked to the `STUDENT` role.
- **FR-2 (Authentication)**: Students and administrators log in with their email and password; dynamic role routing opens the appropriate dashboard.
- **FR-3 (Item Reporting)**: Students can report items with title, category, campus location, item type (`LOST` or `FOUND`), and description.
- **FR-4 (Search & Filter)**: Real-time filtering across titles, locations, categories, types, and statuses.
- **FR-5 (Claim Submission)**: Students can file ownership claims with descriptive proof for open found items.
- **FR-6 (Claim Adjudication)**: Administrators can inspect claims and either approve or reject them. Approval atomically updates the item to `CLAIMED` and rejects competing claims.
- **FR-7 (Account Governance)**: Administrators can promote or demote user roles, with safeguards protecting the active admin and the last administrator.

### 3.2 Non-Functional Requirements (NFR)
- **NFR-1 (Performance & Responsiveness)**: The GUI must not freeze during queries; all network operations run on worker threads.
- **NFR-2 (Security)**: Passwords must be hashed using PBKDF2 with random salts. Plain-text passwords must never be stored, logged, or displayed.
- **NFR-3 (Data Integrity)**: Foreign-key constraints and transaction rollbacks must prevent orphaned records and inconsistent states.
- **NFR-4 (Usability)**: Modern, cohesive design utilizing FlatLaf styling with intuitive color-coded badges.

### 3.3 Hardware & Software Specifications
- **Operating System**: Windows 10 / 11, Linux, macOS
- **JDK Version**: Java Standard Edition (JDK 21 or JDK 26)
- **RDBMS**: MySQL Community Server 8.0 or higher
- **RAM**: Minimum 2 GB (4 GB recommended)
- **Disk Space**: ~50 MB for application files and libraries

---

## Chapter 4: System Design & Architecture

### 4.1 Layered Architecture Overview
The system follows a 3-tier architecture:
1. **Presentation Layer (`com.campus.lostfound.ui`)**:
   - `auth`: `LoginFrame`, `RegisterFrame`
   - `student`: `StudentDashboard`, `ItemBrowsePanel`, `MyReportedItemsPanel`, `MyClaimsPanel`, dialogs
   - `admin`: `AdminDashboard`, `ManageUsersPanel`, `ManageItemsPanel`, `ManageClaimsPanel`
   - `common`: `UITheme`, `RoundedButton`
2. **Business Logic & DAO Layer (`com.campus.lostfound.dao`, `utils`)**:
   - `UserDAO`, `ItemDAO`, `ClaimDAO`
   - `PasswordUtil`, `ValidationUtil`, `SessionManager`, `AdminSetup`
3. **Persistence Layer (`config`, MySQL)**:
   - `DBConnection` loading `config/db.properties`
   - Relational tables: `users`, `items`, `claims`

### 4.2 Database Design
- **`users` table**: Primary key `user_id`, unique `email`, salted `password` hash, role enum.
- **`items` table**: Primary key `item_id`, foreign key `reported_by` referencing `users(user_id)`.
- **`claims` table**: Primary key `claim_id`, foreign key `item_id` referencing `items(item_id)` (`ON DELETE CASCADE`), foreign key `claimed_by` referencing `users(user_id)`.

---

## Chapter 5: Implementation Highlights

### 5.1 Password Hashing Implementation
Passwords use `PBKDF2WithHmacSHA256`:
$$\text{Hash} = \text{PBKDF2}(\text{password}, \text{salt}, \text{iterations} = 65536, \text{keyLength} = 256)$$
Stored as: `65536:<Base64 Salt>:<Base64 Hash>`  
Comparison utilizes `MessageDigest.isEqual()` to prevent timing side-channel attacks.

### 5.2 Atomic Claim Approval Transaction
To eliminate race conditions when approving claims:
```sql
START TRANSACTION;
-- 1. Check if item already has an approved claim
SELECT claim_id FROM claims WHERE item_id = ? AND status = 'APPROVED';
-- 2. Approve selected claim
UPDATE claims SET status = 'APPROVED' WHERE claim_id = ?;
-- 3. Synchronize item status
UPDATE items SET status = 'CLAIMED' WHERE item_id = ?;
-- 4. Reject other competing pending claims
UPDATE claims SET status = 'REJECTED' WHERE item_id = ? AND claim_id != ? AND status = 'PENDING';
COMMIT;
```
If any statement fails, `conn.rollback()` restores the initial state.

---

## Chapter 6: Conclusion

The **Smart Campus Lost and Found System** demonstrates a complete, robust, and secure desktop enterprise solution. By replacing informal, error-prone manual lost-and-found routines with structured claiming, cryptographic security, non-blocking UI threads, and transactional data integrity, the application meets all academic requirements and provides practical campus utility.

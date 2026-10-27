# Smart Campus Lost and Found System

A desktop application designed for college campuses to streamline reporting, searching, claiming, and moderating lost and found personal belongings. Built with **Java Swing**, **JDBC**, and **MySQL**, adhering to Object-Oriented Programming (OOP) principles, Layered Architecture (DAO Pattern), secure PBKDF2 password hashing, and responsive multithreading using `SwingWorker`.

---

## Table of Contents
1. [Project Overview & Objectives](#project-overview--objectives)
2. [Problem Statement & Solution](#problem-statement--solution)
3. [Key Features](#key-features)
4. [System Architecture & Technology Stack](#system-architecture--technology-stack)
5. [Prerequisites](#prerequisites)
6. [Database Setup & Schema](#database-setup--schema)
7. [Configuration (`config/db.properties`)](#configuration-configdbproperties)
8. [Setup & Running the Application](#setup--running-the-application)
9. [Initial Administrator Setup](#initial-administrator-setup)
10. [Automated Verification & Test Scripts](#automated-verification--test-scripts)
11. [Troubleshooting & Common Errors](#troubleshooting--common-errors)
12. [Project Documentation Directory](#project-documentation-directory)

---

## Project Overview & Objectives

In large college campuses, students and staff frequently misplace personal items (student IDs, calculators, keys, electronic devices, bags, and books). Traditional lost-and-found methods rely on informal social media groups, physical pin-boards, or security desks with paper logs, leading to delays, lack of ownership verification, duplicate claims, and low recovery rates.

**Objectives:**
- Provide a centralized, authenticated desktop portal for campus students and administrators.
- Separate student self-service capabilities from administrative moderation.
- Protect user security with modern cryptographic hashing (PBKDF2 with salt) and prepared statements.
- Keep the user interface responsive during database queries through background threads (`SwingWorker`).
- Ensure atomicity in claim approvals and item lifecycle changes via SQL transactions.

---

## Problem Statement & Solution

| Traditional Campus Process | Smart Campus Lost & Found Solution |
| :--- | :--- |
| Paper logs and scattered social media posts. | Centralized database accessible by all registered students and campus staff. |
| Anyone can falsely claim an item without verification. | Structured claim workflow requiring ownership proof evaluated by an administrator. |
| Physical desks must be visited to verify lost items. | Real-time multi-criteria search by title, location, category, item type, and status. |
| Inconsistent status updates when items are claimed. | Atomic database transactions that update claims and synchronize item status to `CLAIMED`. |

---

## Key Features

### For Students:
- **Secure Registration & Login**: Registration restricted to `STUDENT` role with RFC-compliant email and password length validation.
- **Report Lost & Found Items**: Standardized form capturing title, item type (`LOST` or `FOUND`), category, campus location, and detailed description.
- **Browse & Filter Items**: Real-time filtering by keyword, campus location, category, item type (`ALL`, `LOST`, `FOUND`), and lifecycle status (`ALL`, `OPEN`, `CLAIMED`, `RETURNED`).
- **Submit Ownership Claims**: Submit claims with detailed proof on open `FOUND` items.
- **Claim Safeguards**:
  - Cannot claim your own reported items.
  - Cannot claim items marked as `LOST`.
  - Duplicate claims on the same item are blocked.
- **My Reported Items**: View personal reports, mark recovered items as `RETURNED`, or delete accidental reports.
- **My Submitted Claims**: Track administrative adjudication (`PENDING`, `APPROVED`, `REJECTED`) in real-time.

### For Administrators:
- **Live Summary Metrics**: Dashboard displaying registered user counts, lost items, found items, open items, and pending claims.
- **User Management**:
  - View all registered student and admin accounts.
  - Promote students to `ADMIN` or demote admins to `STUDENT`.
  - **Self-Protection**: Administrators cannot demote or delete their own active account.
  - **Last-Admin Protection**: The system prevents deleting or demoting the last active administrator.
  - **Foreign-Key Safeguard**: Users with existing item reports or claims cannot be deleted without first resolving their records.
- **Item Moderation**: Moderate inappropriate reports, update item statuses (`OPEN`, `CLAIMED`, `RETURNED`), or delete reports using safe atomic cascade deletion.
- **Claim Adjudication**:
  - Inspect claimant contact details and submitted proof messages.
  - **Atomic Approval Transaction**: Sets claim to `APPROVED`, synchronizes item to `CLAIMED`, and automatically rejects competing pending claims on that item.
  - **Conflict Prevention**: Rejects any attempt to approve multiple conflicting claims for the same item.

---

## System Architecture & Technology Stack

- **Programming Language**: Java SE (JDK 21 or JDK 26)
- **GUI Framework**: Java Swing (FlatLaf 3.5.4 Modern Look & Feel)
- **Database Engine**: MySQL Server 8.0+
- **Database Connectivity**: MySQL Connector/J 8.4.0 (JDBC)
- **Design Pattern**: Layered Architecture (Model-View-DAO)
- **Concurrency**: `javax.swing.SwingWorker` for asynchronous background database operations
- **Security**: `PBKDF2WithHmacSHA256` password derivation with `java.security.SecureRandom` salt (65,536 iterations, 256-bit key)

```text
SmartCampusLostAndFound/
├── bin/                       # Compiled .class files
├── config/
│   ├── db.properties.example  # Version-controlled configuration template
│   └── db.properties          # Local database credentials (git-ignored)
├── docs/                      # UML diagrams, reports, demo guide
├── lib/
│   ├── flatlaf-3.5.4.jar      # Modern Swing theme library
│   └── mysql-connector-j-8.4.0.jar # Official MySQL JDBC driver
├── resources/                 # Icons and image assets
├── src/com/campus/lostfound/
│   ├── Main.java              # Application entry point
│   ├── config/DBConnection.java # Database connection manager
│   ├── dao/                   # UserDAO, ItemDAO, ClaimDAO
│   ├── models/                # User, Item, Claim entity classes
│   ├── ui/                    # Swing GUI frames, dialogs, and panels
│   └── utils/                 # PasswordUtil, ValidationUtil, SessionManager, AdminSetup
├── run-app.bat                # One-click application launcher
├── setup-admin.bat            # One-time interactive administrator setup
└── run-stage*-test.bat        # Automated test suite scripts
```

---

## Prerequisites

Before running the application, ensure the following are installed on your machine:
1. **Java Development Kit (JDK)**: Version 21 or higher (JDK 26 confirmed working).
2. **MySQL Server**: Version 8.0 or higher running on `localhost:3306`.
3. **Command Line / PowerShell**: Standard Windows terminal environment.

---

## Database Setup & Schema

If configuring a new MySQL instance, create the database and tables using the schema below:

```sql
CREATE DATABASE IF NOT EXISTS smart_campus_db;
USE smart_campus_db;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'ADMIN') DEFAULT 'STUDENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Items Table
CREATE TABLE IF NOT EXISTS items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    category VARCHAR(80),
    location VARCHAR(150),
    item_type ENUM('LOST', 'FOUND') NOT NULL,
    status ENUM('OPEN', 'CLAIMED', 'RETURNED') DEFAULT 'OPEN',
    reported_by INT NOT NULL,
    report_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reported_by) REFERENCES users(user_id) ON DELETE RESTRICT
);

-- 3. Claims Table
CREATE TABLE IF NOT EXISTS claims (
    claim_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    claimed_by INT NOT NULL,
    message VARCHAR(500),
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    claim_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(item_id) ON DELETE CASCADE,
    FOREIGN KEY (claimed_by) REFERENCES users(user_id) ON DELETE RESTRICT
);
```

---

## Configuration (`config/db.properties`)

Database connection credentials reside in `config/db.properties`. This keeps passwords decoupled from Java source code and excluded from version control via `.gitignore`.

To configure your local environment, copy the template and edit your MySQL credentials:
1. Copy `config/db.properties.example` to `config/db.properties`.
2. Open `config/db.properties` and enter your local database username and password:

```properties
# JDBC Connection URL
db.url=jdbc:mysql://localhost:3306/smart_campus_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC

# Database Credentials
db.user=root
db.password=YOUR_MYSQL_PASSWORD_HERE
```

> **Security Note:** Never commit `config/db.properties` with real passwords to public repositories.

---

## Setup & Running the Application

### Method 1: Double-Click Launcher
Double-click `run-app.bat` in the project root directory.

### Method 2: Command Line (PowerShell)
```powershell
# Compile the application
& "C:\Program Files\Java\jdk-26.0.1\bin\javac.exe" -cp "lib\mysql-connector-j-8.4.0.jar;lib\flatlaf-3.5.4.jar" -d "bin" -sourcepath "src" src\com\campus\lostfound\Main.java

# Run the application
& "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "bin;lib\mysql-connector-j-8.4.0.jar;lib\flatlaf-3.5.4.jar" com.campus.lostfound.Main
```

---

## Initial Administrator Setup

Public registration is strictly restricted to students. To bootstrap the first administrator account:

### Method 1: Using `setup-admin.bat`
Double-click `setup-admin.bat` and follow the interactive terminal prompts:
1. Enter Administrator Full Name (e.g., `Campus Security Admin`).
2. Enter Administrator College Email (e.g., `admin@campus.edu`).
3. Enter Administrator Password (secure, masked input).

### Method 2: Command Line Execution
```powershell
& "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "bin;lib\mysql-connector-j-8.4.0.jar;lib\flatlaf-3.5.4.jar" com.campus.lostfound.utils.AdminSetup
```

*If an account with that email already exists, `AdminSetup` elevates the existing account to `ADMIN`.*

---

## Automated Verification & Test Scripts

The project includes individual stage verification scripts and a comprehensive Stage 6 integration test suite. All tests execute non-destructively and clean up their temporary records automatically:

| Batch Script | Test Scope |
| :--- | :--- |
| [`run-db-test.bat`](file:///e:/JAVA%20Project/SmartCampusLostAndFound/run-db-test.bat) | Read-only inspection of database tables, column types, and row counts. |
| [`run-stage2-test.bat`](file:///e:/JAVA%20Project/SmartCampusLostAndFound/run-stage2-test.bat) | PBKDF2 hashing, model constructors, enum mappings, and read-only DAO checks. |
| [`run-stage3-test.bat`](file:///e:/JAVA%20Project/SmartCampusLostAndFound/run-stage3-test.bat) | Validation logic, session lifecycle, and zero-footprint authentication cycle. |
| [`run-stage4-test.bat`](file:///e:/JAVA%20Project/SmartCampusLostAndFound/run-stage4-test.bat) | Item reporting, search filters, claim eligibility, and duplicate claim blocking. |
| [`run-stage5-test.bat`](file:///e:/JAVA%20Project/SmartCampusLostAndFound/run-stage5-test.bat) | Admin adjudication transactions, atomic status sync, and foreign-key safeguards. |
| [`run-stage6-test.bat`](file:///e:/JAVA%20Project/SmartCampusLostAndFound/run-stage6-test.bat) | **Final Full QA Suite** (10 automated boundary and integration tests). |

---

## Troubleshooting & Common Errors

1. **`ERROR 1045 (28000): Access denied for user 'root'@'localhost'`**
   - *Cause*: Incorrect password in `config/db.properties`.
   - *Fix*: Open `config/db.properties` and verify your MySQL password matches your local MySQL server setup.

2. **`ERROR 2003 (HY000): Can't connect to MySQL server on 'localhost:3306'`**
   - *Cause*: MySQL Server service is not running.
   - *Fix*: Start MySQL via Windows Services (`services.msc` -> `MySQL80` -> Start) or via Command Prompt: `net start MySQL80`.

3. **`ClassNotFoundException: com.mysql.cj.jdbc.Driver`**
   - *Cause*: MySQL Connector/J JAR is missing from the classpath.
   - *Fix*: Confirm `lib/mysql-connector-j-8.4.0.jar` is present and that `-cp "bin;lib/*"` is used when launching manually.

4. **`'Found' is not recognized as an internal or external command`**
   - *Cause*: An unquoted ampersand (`&`) in a Windows `.bat` script banner.
   - *Fix*: Resolved in all scripts by replacing `&` with `and` in banner echo statements.

5. **`Cannot delete or update a parent row: a foreign key constraint fails`**
   - *Cause*: Attempting to delete a user who has active item reports or claims.
   - *Fix*: The system automatically blocks this action in `ManageUsersPanel.java` and informs the administrator to resolve the related records first.

---

## Project Documentation Directory

Detailed design and college submission documentation is located in the [`docs/`](file:///e:/JAVA%20Project/SmartCampusLostAndFound/docs/) directory:

- [Use Case Diagram & Actor Specifications](file:///e:/JAVA%20Project/SmartCampusLostAndFound/docs/use-case-diagram.md)
- [Class Diagram & OOP Architecture](file:///e:/JAVA%20Project/SmartCampusLostAndFound/docs/class-diagram.md)
- [Entity-Relationship (ER) Diagram](file:///e:/JAVA%20Project/SmartCampusLostAndFound/docs/er-diagram.md)
- [System Architecture & Concurrency Model](file:///e:/JAVA%20Project/SmartCampusLostAndFound/docs/system-architecture.md)
- [College Project Report (SRS & Design)](file:///e:/JAVA%20Project/SmartCampusLostAndFound/docs/project-report.md)
- [Testing & Quality Assurance Report](file:///e:/JAVA%20Project/SmartCampusLostAndFound/docs/testing-report.md)
- [Future Scope & System Extensions](file:///e:/JAVA%20Project/SmartCampusLostAndFound/docs/future-scope.md)
- [Live Demonstration & Presentation Guide](file:///e:/JAVA%20Project/SmartCampusLostAndFound/docs/demo-guide.md)

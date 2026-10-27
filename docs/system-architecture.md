# System Architecture: Smart Campus Lost and Found

This document details the multi-tiered system architecture, data flow, concurrency model, security enforcement, and state transition lifecycles of the **Smart Campus Lost and Found System**.

---

## 1. High-Level Multi-Tier Layered Architecture

The application is structured into three cleanly decoupled layers following the **Model-View-DAO** architecture pattern:

```mermaid
flowchart TD
    subgraph PresentationTier ["1. Presentation Tier (Java Swing + FlatLaf)"]
        UI_Auth["Authentication Views\n(LoginFrame, RegisterFrame)"]
        UI_Student["Student Portal\n(StudentDashboard, BrowsePanel, Dialogs)"]
        UI_Admin["Administrator Portal\n(AdminDashboard, Management Panels)"]
        UI_Common["UI Theme & Components\n(UITheme, RoundedButton)"]
    end

    subgraph LogicTier ["2. Business Logic & Access Tier (Java SE)"]
        Session["Session Governance\n(SessionManager)"]
        Validator["Input Validation\n(ValidationUtil)"]
        Crypto["Cryptographic Engine\n(PasswordUtil - PBKDF2)"]
        Async["Concurrency Workers\n(SwingWorker)"]
        DAOs["Data Access Objects\n(UserDAO, ItemDAO, ClaimDAO)"]
    end

    subgraph DataTier ["3. Persistence Tier (MySQL RDBMS)"]
        DBConn["JDBC Driver Connection\n(DBConnection via db.properties)"]
        MySQL[("MySQL Server 8.0+\n(smart_campus_db)")]
        Tables["Tables: users, items, claims"]
    end

    PresentationTier -->|User Actions & Inputs| LogicTier
    LogicTier -->|Dispatches Background Queries| DBConn
    DBConn -->|Executes PreparedStatements| MySQL
    MySQL -->|Returns ResultSets / Keys| LogicTier
    LogicTier -->|Publishes Formatted Models to EDT| PresentationTier
```

---

## 2. Concurrency Model: Non-Blocking UI Execution

To prevent the Java Swing GUI from freezing during network socket connections and MySQL queries, the application enforces the **SwingWorker Concurrency Pattern**:

```mermaid
sequenceDiagram
    autonumber
    actor User as User (GUI)
    participant EDT as Event Dispatch Thread (EDT)
    participant Worker as SwingWorker Background Thread
    participant DAO as Data Access Object (DAO)
    participant DB as MySQL Database

    User->>EDT: Clicks Action Button (e.g., "Filter", "Sign In", "Submit Claim")
    EDT->>EDT: Disables Button, Sets Busy Cursor, Clears Old Errors
    EDT->>Worker: execute() background task
    Note over EDT: EDT remains free to process UI repaints, window resizing, and mouse events

    Worker->>DAO: Invokes DAO method (e.g. searchItems / approveClaim)
    DAO->>DB: Sends PreparedStatement over TCP Socket
    DB-->>DAO: Returns ResultSet / Rows Affected
    DAO-->>Worker: Maps ResultSet to Domain Model List (List<Item>)

    Worker->>EDT: Calls done() callback on EDT
    EDT->>EDT: Re-enables Button, Restores Cursor, Updates JTable Model
    EDT-->>User: Displays Updated Table Data / Dialog Message
```

---

## 3. Security Architecture & Threat Mitigations

```mermaid
flowchart TD
    subgraph ThreatPrevention ["Security Controls"]
        direction TB
        PW["Password Security: PBKDF2WithHmacSHA256, 65536 iterations, 128-bit salt"]
        SQLI["SQL Injection Defense: 100% PreparedStatements with parameter binding"]
        AUTH["Role & Session Access: Registration locked to STUDENT, SessionManager checks"]
        INTEG["Referential Integrity: Admin self-deletion and last-admin deletion blocked"]
    end
```

---

## 4. Item and Claim Lifecycle State Transitions

The lifecycle of reported items and submitted claims is synchronized through database state transitions:

```mermaid
stateDiagram-v2
    [*] --> Item_Reported : Student reports item

    state Item_Lifecycle {
        Item_Reported --> Item_OPEN : Added as LOST or FOUND
        Item_OPEN --> Item_CLAIMED : Admin approves valid claim
        Item_OPEN --> Item_RETURNED : Student marks resolved
        Item_CLAIMED --> Item_RETURNED : Physical handover verified
        Item_RETURNED --> [*]
    }

    state Claim_Lifecycle {
        [*] --> Claim_PENDING : Student submits claim with proof
        Claim_PENDING --> Claim_APPROVED : Admin verifies ownership
        Claim_PENDING --> Claim_REJECTED : Proof fails / competing claim approved
        Claim_APPROVED --> [*]
        Claim_REJECTED --> [*]
    }
```

- When a claim is set to `APPROVED`, the associated item is atomically transitioned to `CLAIMED`.
- Competing claims remaining in `PENDING` for that item are automatically set to `REJECTED`.

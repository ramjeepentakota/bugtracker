# Entity Relationship Diagram for Defect Tracker Database

## Overview
The Defect Tracker database is designed for VAPT (Vulnerability Assessment and Penetration Testing) teams to manage clients, applications, test plans, and defects in a structured manner.

## Entities and Relationships

### 1. Users
- **Primary Key**: id (BIGINT)
- **Attributes**:
  - username (VARCHAR 50, UNIQUE)
  - email (VARCHAR 100, UNIQUE)
  - password_hash (VARCHAR 255)
  - role (ENUM: ADMIN, TESTER, CLIENT)
  - first_name (VARCHAR 50)
  - last_name (VARCHAR 50)
  - is_active (BOOLEAN)
  - created_at, updated_at (TIMESTAMP)

### 2. Clients
- **Primary Key**: id (BIGINT)
- **Foreign Keys**: added_by → users(id)
- **Attributes**:
  - client_name (VARCHAR 100)
  - company_name (VARCHAR 100)
  - email (VARCHAR 100, UNIQUE)
  - contact_number (VARCHAR 20)
  - description (TEXT)
  - created_at, updated_at (TIMESTAMP)

### 3. Applications
- **Primary Key**: id (BIGINT)
- **Foreign Keys**:
  - client_id → clients(id) [CASCADE DELETE]
  - added_by → users(id)
- **Attributes**:
  - application_name (VARCHAR 100)
  - environment (ENUM: PRODUCTION, STAGING, DEVELOPMENT, TESTING)
  - technology_stack (TEXT)
  - documentation_path (VARCHAR 255)
  - created_at, updated_at (TIMESTAMP)

### 4. Test Plans
- **Primary Key**: id (BIGINT)
- **Foreign Keys**: added_by → users(id)
- **Attributes**:
  - vulnerability_name (VARCHAR 200)
  - severity (ENUM: CRITICAL, HIGH, MEDIUM, LOW, INFO)
  - description (TEXT)
  - test_procedure (TEXT)
  - test_case_id (VARCHAR 20, UNIQUE) [TP-001, TP-002, etc.]
  - created_at, updated_at (TIMESTAMP)

### 5. Defects
- **Primary Key**: id (BIGINT)
- **Foreign Keys**:
  - client_id → clients(id) [CASCADE DELETE]
  - application_id → applications(id) [CASCADE DELETE]
  - test_plan_id → test_plans(id)
  - assigned_to → users(id)
  - created_by → users(id)
- **Attributes**:
  - defect_id (VARCHAR 20, UNIQUE) [DEF-001, DEF-002, etc.]
  - severity (ENUM: CRITICAL, HIGH, MEDIUM, LOW, INFO)
  - description (TEXT)
  - testing_procedure (TEXT)
  - poc_path (VARCHAR 255) [Proof of Concept file]
  - status (ENUM: NEW, OPEN, IN_PROGRESS, RETEST, CLOSED)
  - created_at, updated_at (TIMESTAMP)

### 6. Defect History
- **Primary Key**: id (BIGINT)
- **Foreign Keys**:
  - defect_id → defects(id) [CASCADE DELETE]
  - changed_by → users(id)
- **Attributes**:
  - old_status, new_status (ENUM)
  - change_reason (TEXT)
  - changed_at (TIMESTAMP)

### 7. Reports
- **Primary Key**: id (BIGINT)
- **Foreign Keys**: generated_by → users(id)
- **Attributes**:
  - report_name (VARCHAR 100)
  - report_type (ENUM: WEEKLY, MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY)
  - file_path (VARCHAR 255)
  - generated_at (TIMESTAMP)

## Relationships

```
Users (1) ────► Clients (many) [added_by]
Users (1) ────► Applications (many) [added_by]
Users (1) ────► Test Plans (many) [added_by]
Users (1) ────► Defects (many) [assigned_to, created_by]
Users (1) ────► Defect History (many) [changed_by]
Users (1) ────► Reports (many) [generated_by]

Clients (1) ────► Applications (many) [CASCADE DELETE]
Clients (1) ────► Defects (many) [CASCADE DELETE]

Applications (1) ────► Defects (many) [CASCADE DELETE]

Test Plans (1) ────► Defects (many)
```

## Key Design Decisions

1. **Cascade Deletes**: Applications and Defects are deleted when their parent Client is deleted to maintain data integrity.

2. **Auto-numbering**: Test Plans use TP-XXX format, Defects use DEF-XXX format for easy identification.

3. **Status Tracking**: Defect History table maintains a complete audit trail of status changes.

4. **Role-based Access**: Users table includes roles (ADMIN, TESTER, CLIENT) for access control.

5. **File Storage**: POC files and documentation are stored as file paths, allowing for external storage solutions.

6. **Indexing**: All foreign keys and commonly queried fields are indexed for performance.

## Sample Data Flow

1. **Admin** creates **Clients**
2. **Admin/Tester** creates **Applications** linked to **Clients**
3. **Tester** creates **Test Plans** (vulnerabilities)
4. **Tester** creates **Defects** linked to Client → Application → Test Plan
5. **Defect History** tracks all status changes
6. **Reports** are generated periodically for analytics

This schema supports the complete VAPT workflow from client onboarding to defect resolution and reporting.
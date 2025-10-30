-- Defect Tracker Database Schema for H2 (Docker)
-- H2 compatible version of the schema

-- Users table for authentication and roles
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'TESTER', 'CLIENT') NOT NULL DEFAULT 'CLIENT',
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Clients table
CREATE TABLE clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_name VARCHAR(100) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    contact_number VARCHAR(20),
    description CLOB,
    added_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (added_by) REFERENCES users(id)
);

-- Applications table
CREATE TABLE applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_name VARCHAR(100) NOT NULL,
    client_id BIGINT NOT NULL,
    environment ENUM('PRODUCTION', 'STAGING', 'DEVELOPMENT', 'TESTING') DEFAULT 'PRODUCTION',
    technology_stack CLOB,
    documentation_path VARCHAR(255),
    added_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    FOREIGN KEY (added_by) REFERENCES users(id)
);

-- Test Plans / Vulnerabilities table
CREATE TABLE test_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vulnerability_name VARCHAR(200) NOT NULL,
    severity ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO') NOT NULL,
    description CLOB,
    test_procedure CLOB NOT NULL,
    test_case_id VARCHAR(20) UNIQUE NOT NULL,
    added_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (added_by) REFERENCES users(id)
);

-- Defects table
CREATE TABLE defects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    defect_id VARCHAR(20) UNIQUE NOT NULL,
    client_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    test_plan_id BIGINT NOT NULL,
    severity ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO') NOT NULL,
    description CLOB NOT NULL,
    testing_procedure CLOB,
    poc_path VARCHAR(255),
    assigned_to BIGINT,
    status ENUM('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST', 'CLOSED') DEFAULT 'NEW',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    FOREIGN KEY (test_plan_id) REFERENCES test_plans(id),
    FOREIGN KEY (assigned_to) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Defect status history table for timeline tracking
CREATE TABLE defect_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    defect_id BIGINT NOT NULL,
    old_status ENUM('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST', 'CLOSED'),
    new_status ENUM('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST', 'CLOSED') NOT NULL,
    changed_by BIGINT,
    change_reason CLOB,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (defect_id) REFERENCES defects(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id)
);

-- Reports table for scheduled/auto-generated reports
CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_name VARCHAR(100) NOT NULL,
    report_type ENUM('WEEKLY', 'MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY') NOT NULL,
    generated_by BIGINT,
    file_path VARCHAR(255),
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (generated_by) REFERENCES users(id)
);

-- VAPT Reports table
CREATE TABLE vapt_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    report_name VARCHAR(255) NOT NULL,
    status ENUM('INITIALIZED', 'IN_PROGRESS', 'COMPLETED') DEFAULT 'INITIALIZED',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- VAPT Test Cases table
CREATE TABLE vapt_test_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    test_plan_id BIGINT NOT NULL,
    status ENUM('NOT_STARTED', 'IN_PROGRESS', 'PASSED', 'FAILED', 'NOT_APPLICABLE') DEFAULT 'NOT_STARTED',
    findings CLOB,
    severity ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (report_id) REFERENCES vapt_reports(id) ON DELETE CASCADE,
    FOREIGN KEY (test_plan_id) REFERENCES test_plans(id)
);

-- VAPT POC (Proof of Concept) table
CREATE TABLE vapt_pocs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_case_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    description CLOB,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (test_case_id) REFERENCES vapt_test_cases(id) ON DELETE CASCADE
);
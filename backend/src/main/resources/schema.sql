-- Defect Tracker Database Schema for H2
-- H2 compatible version of the schema

-- Users table for authentication and roles
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CLIENT' CHECK (role IN ('ADMIN', 'TESTER', 'CLIENT', 'PM_DM')),
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
    email VARCHAR(100) UNIQUE NOT NULL,
    contact_number VARCHAR(20),
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
    environment VARCHAR(20) DEFAULT 'PRODUCTION' CHECK (environment IN ('PRODUCTION', 'STAGING', 'DEVELOPMENT', 'TESTING')),
    technology_stack TEXT,
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
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    description TEXT,
    test_procedure TEXT NOT NULL,
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
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    description TEXT NOT NULL,
    testing_procedure TEXT,
    poc_path VARCHAR(255),
    assigned_to BIGINT,
    status VARCHAR(20) DEFAULT 'NEW' CHECK (status IN ('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST', 'CLOSED')),
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
    old_status VARCHAR(20) CHECK (old_status IN ('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST', 'CLOSED')),
    new_status VARCHAR(20) NOT NULL CHECK (new_status IN ('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST', 'CLOSED')),
    changed_by BIGINT,
    change_reason TEXT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (defect_id) REFERENCES defects(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id)
);

-- Reports table for scheduled/auto-generated reports
CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_name VARCHAR(100) NOT NULL,
    report_type VARCHAR(20) NOT NULL CHECK (report_type IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY')),
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
    status VARCHAR(20) DEFAULT 'INITIALIZED' CHECK (status IN ('INITIALIZED', 'IN_PROGRESS', 'COMPLETED')),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- New fields for enhanced report template
    assessment_date DATE,
    report_version VARCHAR(50),
    prepared_by VARCHAR(100),
    reviewed_by VARCHAR(100),
    submitted_to VARCHAR(100),
    objective TEXT,
    scope TEXT,
    approach TEXT,
    key_highlights TEXT,
    overall_risk VARCHAR(50),
    asset_type VARCHAR(100),
    urls TEXT,
    start_date DATE,
    end_date DATE,
    total_days BIGINT,
    testers VARCHAR(255),
    critical_count BIGINT DEFAULT 0,
    high_count BIGINT DEFAULT 0,
    medium_count BIGINT DEFAULT 0,
    low_count BIGINT DEFAULT 0,
    info_count BIGINT DEFAULT 0,
    posture_level VARCHAR(50),
    recommendations TEXT,
    next_steps TEXT,
    approved_by VARCHAR(100),
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- VAPT Test Cases table
CREATE TABLE vapt_test_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vapt_report_id BIGINT NOT NULL,
    test_plan_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'NOT_STARTED' CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'PASSED', 'FAILED', 'NOT_APPLICABLE')),
    findings TEXT,
    description TEXT,
    test_procedure TEXT,
    severity VARCHAR(20) CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    vulnerability_status VARCHAR(20) DEFAULT 'OPEN' CHECK (vulnerability_status IN ('OPEN', 'CLOSED')),
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vapt_report_id) REFERENCES vapt_reports(id) ON DELETE CASCADE,
    FOREIGN KEY (test_plan_id) REFERENCES test_plans(id),
    FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- VAPT POC (Proof of Concept) table
CREATE TABLE vapt_pocs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vapt_test_case_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    description TEXT,
    evidences TEXT,
    uploaded_by BIGINT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vapt_test_case_id) REFERENCES vapt_test_cases(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id)
);
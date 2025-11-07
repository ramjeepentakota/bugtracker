-- Defect Tracker Database Schema for VAPT Cybersecurity Testing
-- MySQL 8.0+ compatible

-- Create database
CREATE DATABASE IF NOT EXISTS defect_tracker;
USE defect_tracker;

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role)
);

-- Clients table
CREATE TABLE clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_name VARCHAR(100) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    contact_number VARCHAR(20),
    description TEXT,
    added_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (added_by) REFERENCES users(id),
    INDEX idx_client_name (client_name),
    INDEX idx_company_name (company_name),
    INDEX idx_email (email)
);

-- Applications table
CREATE TABLE applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_name VARCHAR(100) NOT NULL,
    client_id BIGINT NOT NULL,
    environment ENUM('PRODUCTION', 'STAGING', 'DEVELOPMENT', 'TESTING') DEFAULT 'PRODUCTION',
    technology_stack TEXT,
    documentation_path VARCHAR(255),
    added_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    FOREIGN KEY (added_by) REFERENCES users(id),
    INDEX idx_application_name (application_name),
    INDEX idx_client_id (client_id),
    INDEX idx_environment (environment)
);

-- Test Plans / Vulnerabilities table
CREATE TABLE test_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vulnerability_name VARCHAR(200) NOT NULL,
    severity ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO') NOT NULL,
    description TEXT,
    test_procedure TEXT NOT NULL,
    test_case_id VARCHAR(20) UNIQUE NOT NULL, -- TP-001, TP-002, etc.
    added_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (added_by) REFERENCES users(id),
    INDEX idx_vulnerability_name (vulnerability_name),
    INDEX idx_severity (severity),
    INDEX idx_test_case_id (test_case_id)
);

-- Defects table
CREATE TABLE defects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    defect_id VARCHAR(20) UNIQUE NOT NULL, -- DEF-001, DEF-002, etc.
    client_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    test_plan_id BIGINT NOT NULL,
    severity ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO') NOT NULL,
    description TEXT NOT NULL,
    testing_procedure TEXT,
    poc_path VARCHAR(255), -- Proof of Concept file path
    assigned_to BIGINT,
    status ENUM('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST', 'CLOSED') DEFAULT 'NEW',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    FOREIGN KEY (test_plan_id) REFERENCES test_plans(id),
    FOREIGN KEY (assigned_to) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_defect_id (defect_id),
    INDEX idx_client_id (client_id),
    INDEX idx_application_id (application_id),
    INDEX idx_test_plan_id (test_plan_id),
    INDEX idx_severity (severity),
    INDEX idx_status (status),
    INDEX idx_assigned_to (assigned_to),
    INDEX idx_created_at (created_at)
);

-- Defect status history table for timeline tracking
CREATE TABLE defect_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    defect_id BIGINT NOT NULL,
    old_status ENUM('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST', 'CLOSED'),
    new_status ENUM('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST', 'CLOSED') NOT NULL,
    changed_by BIGINT,
    change_reason TEXT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (defect_id) REFERENCES defects(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id),
    INDEX idx_defect_id (defect_id),
    INDEX idx_changed_at (changed_at)
);

-- Reports table for scheduled/auto-generated reports
CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_name VARCHAR(100) NOT NULL,
    report_type ENUM('WEEKLY', 'MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY') NOT NULL,
    generated_by BIGINT,
    file_path VARCHAR(255),
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (generated_by) REFERENCES users(id),
    INDEX idx_report_type (report_type),
    INDEX idx_generated_at (generated_at)
);

-- Insert sample data (passwords are BCrypt hashed for 'password')
INSERT INTO users (username, email, password_hash, role, first_name, last_name) VALUES
('admin', 'admin@defecttracker.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 'System', 'Administrator'),
('tester1', 'tester1@defecttracker.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'TESTER', 'John', 'Tester'),
('client1', 'client1@company.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'CLIENT', 'Jane', 'Client');

INSERT INTO clients (client_name, company_name, email, contact_number, description, added_by) VALUES
('John Doe', 'TechCorp Inc.', 'john.doe@techcorp.com', '+1-555-0123', 'Leading technology company', 1),
('Alice Smith', 'FinanceHub Ltd.', 'alice.smith@financehub.com', '+1-555-0456', 'Financial services provider', 1);

INSERT INTO applications (application_name, client_id, environment, technology_stack, added_by) VALUES
('Branch Portal', 1, 'PRODUCTION', 'React, Node.js, MySQL', 1),
('Customer Portal', 1, 'STAGING', 'Angular, Spring Boot, PostgreSQL', 1),
('E-commerce Platform', 2, 'PRODUCTION', 'Vue.js, Django, MongoDB', 1);

INSERT INTO test_plans (vulnerability_name, severity, description, test_procedure, test_case_id, added_by) VALUES
('SQL Injection', 'CRITICAL', 'Database injection vulnerability', 'Test input fields with SQL payloads', 'TP-001', 2),
('XSS Attack', 'HIGH', 'Cross-site scripting vulnerability', 'Inject script tags in input fields', 'TP-002', 2),
('CSRF Attack', 'MEDIUM', 'Cross-site request forgery', 'Test for missing CSRF tokens', 'TP-003', 2),
('Weak Password Policy', 'LOW', 'Insufficient password requirements', 'Check password complexity rules', 'TP-004', 2);

INSERT INTO defects (defect_id, client_id, application_id, test_plan_id, severity, description, testing_procedure, assigned_to, status, created_by) VALUES
('DEF-001', 1, 1, 1, 'CRITICAL', 'SQL injection in login form', 'Injected malicious SQL payload', 2, 'OPEN', 2),
('DEF-002', 1, 2, 2, 'HIGH', 'XSS in comment section', 'Posted script in comment field', 2, 'IN_PROGRESS', 2),
('DEF-003', 2, 3, 3, 'MEDIUM', 'CSRF vulnerability in transfer form', 'Forged request without token', 2, 'CLOSED', 2);
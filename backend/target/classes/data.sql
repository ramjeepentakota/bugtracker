-- Insert sample data for MySQL database
-- Using BCrypt hashed passwords for security
INSERT INTO users (username, email, password_hash, role, first_name, last_name, is_active) VALUES
('ramjee', 'ramjee@defecttracker.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 'Ramjee', 'Admin', true);

INSERT INTO clients (client_name, email, contact_number, added_by) VALUES
('TechCorp Inc.', 'john.doe@techcorp.com', '+1-555-0123', 1),
('FinanceHub Ltd.', 'alice.smith@financehub.com', '+1-555-0456', 1);

INSERT INTO applications (application_name, client_id, environment, technology_stack, added_by) VALUES
('Branch Portal', 1, 'PRODUCTION', 'React, Node.js, MySQL', 1),
('Customer Portal', 1, 'STAGING', 'Angular, Spring Boot, PostgreSQL', 1),
('E-commerce Platform', 2, 'PRODUCTION', 'Vue.js, Django, MongoDB', 1);

INSERT INTO test_plans (vulnerability_name, severity, description, test_procedure, test_case_id, added_by) VALUES
('SQL Injection', 'CRITICAL', 'Database injection vulnerability', 'Test input fields with SQL payloads', 'TP-001', 1),
('XSS Attack', 'HIGH', 'Cross-site scripting vulnerability', 'Inject script tags in input fields', 'TP-002', 1),
('CSRF Attack', 'MEDIUM', 'Cross-site request forgery', 'Test for missing CSRF tokens', 'TP-003', 1),
('Weak Password Policy', 'LOW', 'Insufficient password requirements', 'Check password complexity rules', 'TP-004', 1);

INSERT INTO defects (defect_id, client_id, application_id, test_plan_id, severity, description, testing_procedure, assigned_to, status, created_by) VALUES
('DEF-001', 1, 1, 1, 'CRITICAL', 'SQL injection in login form', 'Injected malicious SQL payload', 1, 'OPEN', 1),
('DEF-002', 1, 2, 2, 'HIGH', 'XSS in comment section', 'Posted script in comment field', 1, 'IN_PROGRESS', 1),
('DEF-003', 2, 3, 3, 'MEDIUM', 'CSRF vulnerability in transfer form', 'Forged request without token', 1, 'CLOSED', 1);

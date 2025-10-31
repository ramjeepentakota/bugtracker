-- Insert sample data for H2 database
<<<<<<< HEAD
INSERT INTO users (username, email, password_hash, role, first_name, last_name, is_active) VALUES
('admin', 'admin@defecttracker.com', 'password', 'ADMIN', 'System', 'Administrator', true);
=======
-- Using clear text passwords as requested
INSERT INTO users (username, email, password_hash, role, first_name, last_name, is_active) VALUES
('admin', 'admin@defecttracker.com', 'password', 'ADMIN', 'System', 'Administrator', true),
('user', 'user@defecttracker.com', 'password', 'CLIENT', 'John', 'Doe', true),
('tester1', 'tester1@defecttracker.com', 'password', 'TESTER', 'John', 'Tester', true),
('client1', 'client1@company.com', 'password', 'CLIENT', 'Jane', 'Client', true);

INSERT INTO clients (client_name, company_name, email, contact_number, description, added_by) VALUES
('John Doe', 'TechCorp Inc.', 'john.doe@techcorp.com', '+1-555-0123', 'Leading technology company', 1),
('Alice Smith', 'FinanceHub Ltd.', 'alice.smith@financehub.com', '+1-555-0456', 'Financial services provider', 1);

INSERT INTO applications (application_name, client_id, environment, technology_stack, added_by) VALUES
('Branch Portal', 1, 'PRODUCTION', 'React, Node.js, MySQL', 1),
('Customer Portal', 1, 'STAGING', 'Angular, Spring Boot, PostgreSQL', 1),
('E-commerce Platform', 2, 'PRODUCTION', 'Vue.js, Django, MongoDB', 1);

INSERT INTO test_plans (vulnerability_name, severity, description, test_procedure, test_case_id, added_by) VALUES
('SQL Injection', 'CRITICAL', 'Database injection vulnerability', 'Test input fields with SQL payloads', 'TP-001', 3),
('XSS Attack', 'HIGH', 'Cross-site scripting vulnerability', 'Inject script tags in input fields', 'TP-002', 3),
('CSRF Attack', 'MEDIUM', 'Cross-site request forgery', 'Test for missing CSRF tokens', 'TP-003', 3),
('Weak Password Policy', 'LOW', 'Insufficient password requirements', 'Check password complexity rules', 'TP-004', 3);

INSERT INTO defects (defect_id, client_id, application_id, test_plan_id, severity, description, testing_procedure, assigned_to, status, created_by) VALUES
('DEF-001', 1, 1, 1, 'CRITICAL', 'SQL injection in login form', 'Injected malicious SQL payload', 3, 'OPEN', 3),
('DEF-002', 1, 2, 2, 'HIGH', 'XSS in comment section', 'Posted script in comment field', 3, 'IN_PROGRESS', 3),
('DEF-003', 2, 3, 3, 'MEDIUM', 'CSRF vulnerability in transfer form', 'Forged request without token', 3, 'CLOSED', 3);
>>>>>>> e5f1c9cd (new dashboard design and login page fix)

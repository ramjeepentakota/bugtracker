-- Migration to enhance RBAC with proper role-to-client/application assignments
-- Run this after deploying the code changes

-- Ensure PM_DM role is properly defined in users table
ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CLIENT' CHECK (role IN ('ADMIN', 'TESTER', 'CLIENT', 'PM_DM'));

-- Create user_client_assignments table if it doesn't exist
CREATE TABLE IF NOT EXISTS user_client_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    application_id BIGINT NULL, -- NULL means access to all applications of the client
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(id),
    UNIQUE KEY unique_user_client_app (user_id, client_id, application_id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_client_assignments_user_id ON user_client_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_user_client_assignments_client_id ON user_client_assignments(client_id);
CREATE INDEX IF NOT EXISTS idx_user_client_assignments_application_id ON user_client_assignments(application_id);

-- Insert sample data for testing (only if tables are empty)
INSERT IGNORE INTO users (username, email, password_hash, role, first_name, last_name, is_active) VALUES
('admin_user', 'admin@defecttracker.com', '$2a$10$example.hash.here', 'ADMIN', 'System', 'Administrator', true),
('pm_user', 'pm@defecttracker.com', '$2a$10$example.hash.here', 'PM_DM', 'Project', 'Manager', true),
('tester_user', 'tester@defecttracker.com', '$2a$10$example.hash.here', 'TESTER', 'Security', 'Tester', true),
('client_user', 'client@defecttracker.com', '$2a$10$example.hash.here', 'CLIENT', 'Client', 'User', true);

-- Insert sample clients and applications if they don't exist
INSERT IGNORE INTO clients (client_name, email, contact_number, added_by) VALUES
('TechCorp Inc.', 'contact@techcorp.com', '+1-555-0101', 1),
('FinanceHub Ltd.', 'contact@financehub.com', '+1-555-0102', 1);

INSERT IGNORE INTO applications (application_name, client_id, environment, technology_stack, added_by) VALUES
('TechCorp Web App', 1, 'PRODUCTION', 'React, Node.js, PostgreSQL', 1),
('TechCorp Mobile App', 1, 'PRODUCTION', 'React Native, Firebase', 1),
('FinanceHub Banking App', 2, 'PRODUCTION', 'Angular, Java, Oracle', 1),
('FinanceHub Investment Portal', 2, 'STAGING', 'Vue.js, Python, MongoDB', 1);

-- Assign PM_DM user to clients/applications
INSERT IGNORE INTO user_client_assignments (user_id, client_id, application_id, assigned_by) VALUES
(2, 1, NULL, 1), -- PM_DM assigned to TechCorp Inc. (all applications)
(2, 2, 3, 1);   -- PM_DM assigned to specific FinanceHub application

-- Insert sample VAPT reports for testing
INSERT IGNORE INTO vapt_reports (client_id, application_id, report_name, status, created_by) VALUES
(1, 1, 'TechCorp Web App Security Assessment', 'COMPLETED', 3),
(1, 2, 'TechCorp Mobile App Security Assessment', 'COMPLETED', 3),
(2, 3, 'FinanceHub Banking App Security Assessment', 'COMPLETED', 3),
(2, 4, 'FinanceHub Investment Portal Security Assessment', 'COMPLETED', 3);
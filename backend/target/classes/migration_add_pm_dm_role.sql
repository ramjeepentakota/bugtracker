-- Migration to add PM_DM role support
-- Run this after deploying the code changes

-- Update existing schema to allow PM_DM role
ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CLIENT' CHECK (role IN ('ADMIN', 'TESTER', 'CLIENT', 'PM_DM'));

-- Create user_client_assignments table for PM/DM to client/application assignments
CREATE TABLE user_client_assignments (
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

-- Insert sample PM_DM user
INSERT INTO users (username, email, password_hash, role, first_name, last_name, is_active) VALUES
('pm_user', 'pm@defecttracker.com', '$2a$10$example.hash.here', 'PM_DM', 'Project', 'Manager', true);

-- Assign PM_DM user to clients/applications
INSERT INTO user_client_assignments (user_id, client_id, application_id, assigned_by) VALUES
(2, 1, NULL, 1), -- PM_DM user assigned to TechCorp Inc. (all applications)
(2, 2, 3, 1);   -- PM_DM user assigned to specific application in FinanceHub Ltd.
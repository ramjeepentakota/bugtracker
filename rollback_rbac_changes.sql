-- Rollback script for RBAC implementation
-- Run this to undo all RBAC-related changes

-- Remove PM_DM role from users table (revert to original roles)
-- Note: This will set any PM_DM users to CLIENT role as fallback
UPDATE users SET role = 'CLIENT' WHERE role = 'PM_DM';
ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CLIENT' CHECK (role IN ('ADMIN', 'TESTER', 'CLIENT'));

-- Drop user_client_assignments table
DROP TABLE IF EXISTS user_client_assignments;

-- Remove sample data inserted by migration (optional - uncomment if needed)
-- DELETE FROM vapt_reports WHERE report_name LIKE '%Security Assessment%';
-- DELETE FROM applications WHERE application_name LIKE '%Security Assessment%';
-- DELETE FROM clients WHERE client_name IN ('TechCorp Inc.', 'FinanceHub Ltd.');
-- DELETE FROM users WHERE username IN ('admin_user', 'pm_user', 'tester_user', 'client_user');

-- Note: The AuthorizationFilter.java changes need to be manually reverted
-- The frontend changes (Sidebar.jsx, App.jsx, VaptReports.jsx) need to be manually reverted
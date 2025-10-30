-- Insert sample data for H2 database
INSERT INTO users (username, email, password_hash, role, first_name, last_name, is_active) VALUES
('admin', 'admin@defecttracker.com', 'password', 'ADMIN', 'System', 'Administrator', true);
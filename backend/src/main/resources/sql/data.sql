-- =============================================================================
-- OrderCraft – Manufacturing Order Management System (OMS)
-- Database DML Seed Data Script (MySQL 8.0+)
-- =============================================================================

USE ordercraft_db;

-- 1. Insert Roles
INSERT INTO roles (id, name, description) VALUES
(1, 'ROLE_ADMIN', 'System Administrator with full access'),
(2, 'ROLE_MANAGER', 'Manufacturing and Operations Manager'),
(3, 'ROLE_EMPLOYEE', 'Production floor employee')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 2. Insert Users (Password: 'password123' BCrypt hash)
INSERT INTO users (id, username, email, password, full_name, phone, active) VALUES
(1, 'admin', 'admin@ordercraft.com', '$2a$10$v7g9L8qX1k7yC3uQZ8.yxe1r9F2uM1iE2H9lO5nE0eS6g9L8qX1k7', 'Alexander Vance', '+1-555-0101', TRUE),
(2, 'manager', 'manager@ordercraft.com', '$2a$10$v7g9L8qX1k7yC3uQZ8.yxe1r9F2uM1iE2H9lO5nE0eS6g9L8qX1k7', 'Elena Rostova', '+1-555-0102', TRUE),
(3, 'employee', 'employee@ordercraft.com', '$2a$10$v7g9L8qX1k7yC3uQZ8.yxe1r9F2uM1iE2H9lO5nE0eS6g9L8qX1k7', 'Marcus Chen', '+1-555-0103', TRUE)
ON DUPLICATE KEY UPDATE username=VALUES(username);

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),
(1, 2),
(2, 2),
(3, 3)
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

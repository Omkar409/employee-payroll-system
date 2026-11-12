-- Employee Payroll Management System v2
-- Database Schema Definition (MySQL 8.0.16+)

CREATE DATABASE IF NOT EXISTS payroll_db
 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE payroll_db;

-- ========== USERS (authentication & RBAC) ==========
CREATE TABLE IF NOT EXISTS users (
 user_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
 username VARCHAR(50) NOT NULL,
 password_hash VARCHAR(128) NOT NULL, -- Base64 PBKDF2 output
 password_salt VARCHAR(64) NOT NULL,  -- Base64 random salt
 full_name VARCHAR(100) NOT NULL,
 email VARCHAR(100) NOT NULL,
 role ENUM('ADMIN','HR') NOT NULL DEFAULT 'HR',
 is_active TINYINT(1) NOT NULL DEFAULT 1,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 last_login_at TIMESTAMP NULL,
 PRIMARY KEY (user_id),
 CONSTRAINT uq_users_username UNIQUE (username),
 CONSTRAINT uq_users_email UNIQUE (email),
 CONSTRAINT ck_users_username CHECK (CHAR_LENGTH(username) >= 4)
) ENGINE=InnoDB;

-- ========== EMPLOYEES (single table + type discriminator) ==========
CREATE TABLE IF NOT EXISTS employees (
 employee_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
 name VARCHAR(100) NOT NULL,
 basic_salary DECIMAL(12,2) NOT NULL,
 employee_type ENUM('PERMANENT','CONTRACT') NOT NULL,
 -- PERMANENT-only attributes (NULL for contract staff)
 bonus DECIMAL(12,2) NULL,
 -- CONTRACT-only attributes (NULL for permanent staff)
 contract_allowance DECIMAL(12,2) NULL,
 contract_months SMALLINT UNSIGNED NULL,
 created_by INT UNSIGNED NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY (employee_id),
 CONSTRAINT ck_emp_salary CHECK (basic_salary >= 0),
 CONSTRAINT ck_emp_type_fields CHECK (
  (employee_type = 'PERMANENT' AND contract_allowance IS NULL AND contract_months IS NULL)
  OR (employee_type = 'CONTRACT' AND bonus IS NULL)
 ),
 CONSTRAINT fk_emp_created_by FOREIGN KEY (created_by)
  REFERENCES users(user_id) ON DELETE SET NULL,
 INDEX idx_emp_name (name),
 INDEX idx_emp_type (employee_type)
) ENGINE=InnoDB;

-- ========== PAYROLL_RESULTS (output of batch runs) ==========
CREATE TABLE IF NOT EXISTS payroll_results (
 result_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
 run_id VARCHAR(36) NOT NULL, -- UUID per batch run
 employee_id INT UNSIGNED NOT NULL,
 gross_salary DECIMAL(12,2) NOT NULL,
 deductions DECIMAL(12,2) NOT NULL,
 net_salary DECIMAL(12,2) NOT NULL,
 processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY (result_id),
 CONSTRAINT fk_pr_employee FOREIGN KEY (employee_id)
  REFERENCES employees(employee_id) ON DELETE CASCADE,
 INDEX idx_pr_run (run_id)
) ENGINE=InnoDB;

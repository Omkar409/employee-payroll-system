-- Employee Payroll Management System v2
-- Seed Data

USE payroll_db;

-- ========== 1. SEED DEFAULT USERS ==========
-- Passwords:
-- admin:   Admin@1234 (Role: ADMIN)
-- hr_user: HrUser@1234 (Role: HR)

INSERT INTO users (user_id, username, password_hash, password_salt, full_name, email, role, is_active)
VALUES 
(1, 'admin', 'PQ1La6Onah13cqhv3WpMVTy4R7FAxwO3xqJyFaH/z4A=', 'Gb//jloFVTCCk8GkT8s2PQ==', 'System Administrator', 'admin@srm.edu', 'ADMIN', 1),
(2, 'hr_user', 'p9FZ4Xo3KWw7IY5O6ipvRl0LTuR9p1qZxcFXAaFD9ek=', 'GoMByubzYUxSse3K2dBv8Q==', 'Human Resources Lead', 'hr@srm.edu', 'HR', 1)
ON DUPLICATE KEY UPDATE 
  password_hash = VALUES(password_hash),
  password_salt = VALUES(password_salt),
  full_name = VALUES(full_name),
  role = VALUES(role),
  is_active = VALUES(is_active);

-- ========== 2. SEED INITIAL EMPLOYEES ==========
INSERT INTO employees (employee_id, name, basic_salary, employee_type, bonus, contract_allowance, contract_months, created_by)
VALUES
(1, 'Alice Johnson', 75000.00, 'PERMANENT', 12000.00, NULL, NULL, 1),
(2, 'Bob Williams', 62000.00, 'PERMANENT', 8500.00, NULL, NULL, 1),
(3, 'Charlie Davis', 54000.00, 'CONTRACT', NULL, 7500.00, 12, 2),
(4, 'Diana Evans', 88000.00, 'PERMANENT', 15000.00, NULL, NULL, 1),
(5, 'Evan Miller', 48000.00, 'CONTRACT', NULL, 5000.00, 6, 2)
ON DUPLICATE KEY UPDATE 
  name = VALUES(name),
  basic_salary = VALUES(basic_salary),
  employee_type = VALUES(employee_type);

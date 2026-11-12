-- Employee Payroll Management System v2
-- Seed Data

USE payroll_db;

-- ========== 1. SEED DEFAULT USERS ==========
-- Logins & Passwords:
-- omkar:      Omkar@1234      (Role: ADMIN)
-- neeraj:     Neeraj@1234     (Role: HR)
-- kushwanth:  Kushwanth@1234  (Role: ADMIN)
-- surya:      Surya@1234      (Role: HR)
-- admin:      Admin@1234      (Role: ADMIN)

INSERT INTO users (user_id, username, password_hash, password_salt, full_name, email, role, is_active)
VALUES 
(1, 'admin', 'PQ1La6Onah13cqhv3WpMVTy4R7FAxwO3xqJyFaH/z4A=', 'Gb//jloFVTCCk8GkT8s2PQ==', 'System Administrator', 'admin@srm.edu', 'ADMIN', 1),
(5, 'omkar', 'Xf31VfkJ65nruxKTe7JKkq6p4P+5qVW5TAoql/a0MLc=', '+UJnY/2oBW+cVr/VOYqpcg==', 'Omkar', 'omkar@srm.edu', 'ADMIN', 1),
(6, 'neeraj', 'v+nEgO+6OqvK7+icEHNzJPLLUJJNdC7UuiK2US5XQF4=', 'ZaNYDW5FSYocr1JJbwiXkA==', 'Neeraj', 'neeraj@srm.edu', 'HR', 1),
(7, 'kushwanth', '+LajWzDhCY8y+MIbwVJl9ug729KE0DURp7z44yDDiw0=', 'WzF7Zcb6QQL2KYLWq+TQAA==', 'Kushwanth', 'kushwanth@srm.edu', 'ADMIN', 1),
(8, 'surya', 'MiswuFvY0p0un5ImW+elc2/CXJV2KHtaohgBBERe3qg=', 'OFyBWiVfxAZaMxTjS5jNEg==', 'Surya', 'surya@srm.edu', 'HR', 1)
ON DUPLICATE KEY UPDATE 
  password_hash = VALUES(password_hash),
  password_salt = VALUES(password_salt),
  full_name = VALUES(full_name),
  role = VALUES(role),
  is_active = VALUES(is_active);

-- ========== 2. SEED EMPLOYEES ==========
INSERT INTO employees (employee_id, name, basic_salary, employee_type, bonus, contract_allowance, contract_months, created_by)
VALUES
(1, 'Omkar', 85000.00, 'PERMANENT', 15000.00, NULL, NULL, 5),
(2, 'Neeraj', 80000.00, 'PERMANENT', 12000.00, NULL, NULL, 5),
(3, 'Kushwanth', 75000.00, 'CONTRACT', NULL, 10000.00, 12, 6),
(4, 'Surya', 70000.00, 'CONTRACT', NULL, 8000.00, 6, 6)
ON DUPLICATE KEY UPDATE 
  name = VALUES(name),
  basic_salary = VALUES(basic_salary),
  employee_type = VALUES(employee_type),
  bonus = VALUES(bonus),
  contract_allowance = VALUES(contract_allowance),
  contract_months = VALUES(contract_months);

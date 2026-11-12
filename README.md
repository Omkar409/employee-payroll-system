# Employee Payroll Management System v2 (CSE 309)

Architectural Blueprint & Implementation based on Java AWT, MySQL (JDBC), Layered Architecture, and Multithreading.

---

## 1. System Overview & Architecture
- **Language & Runtime**: Java 17+ (Standard Edition)
- **GUI Framework**: Strictly Java AWT (`java.awt.*`, `java.awt.event.*`). No Swing or JavaFX.
- **Database**: MySQL 8.0+ via JDBC (`mysql-connector-j-26.10.0`), `PreparedStatement` only.
- **Architecture**: Strict 4-layer downward dependency (`UI -> Service -> DAO -> Config -> MySQL`).
- **Package Root**: `com.srm.payroll`

### Package Structure
```
src/main/java/com/srm/payroll/
├── Main.java                                   # Application entry point (EDT launcher)
├── config/
│   └── DatabaseConnection.java                 # Thread-safe singleton connection factory
├── model/
│   ├── User.java                               # User model
│   ├── Role.java                               # Enum (ADMIN, HR)
│   ├── Employee.java                           # Abstract employee base
│   ├── PermanentEmployee.java                  # Permanent employee (bonus & PF/tax)
│   ├── ContractEmployee.java                   # Contract employee (allowance & duration)
│   ├── EmployeeType.java                       # Enum (PERMANENT, CONTRACT)
│   ├── PayrollResult.java                      # Payroll calculation model
│   └── BatchSummary.java                       # Batch metrics record
├── dao/
│   ├── UserDAO.java                            # User DAO interface
│   ├── EmployeeDAO.java                        # Employee DAO interface
│   ├── PayrollResultDAO.java                   # Batch results DAO interface
│   ├── DataAccessException.java               # Friendly checked DB exception
│   └── impl/
│       ├── UserDAOImpl.java                    # PreparedStatement MySQL implementation
│       ├── EmployeeDAOImpl.java                # Polymorphic row mapping & binding
│       ├── PayrollResultDAOImpl.java           # Batch transaction execution
│       └── InMemoryEmployeeDAO.java            # Concurrent mock for unit tests
├── service/
│   ├── AuthService.java                        # PBKDF2 authentication & registration
│   ├── EmployeeService.java                    # CRUD & business rule validation
│   ├── PayrollCalculator.java                  # Strategy interface
│   ├── StandardPayrollCalculator.java          # Polymorphic calculation engine
│   ├── PayrollProgressListener.java            # Concurrency event listener
│   ├── PayrollTask.java                        # Background worker task (Runnable)
│   ├── PayrollBatchService.java                # Thread manager with atomic guards
│   ├── SessionContext.java                     # Static authenticated user holder
│   └── exception/
│       ├── AuthenticationException.java
│       ├── ValidationException.java
│       └── DuplicateUserException.java
├── ui/
│   ├── LoginFrame.java                         # AWT Login window
│   ├── RegisterFrame.java                      # AWT Registration window
│   ├── PayrollFrame.java                       # AWT Main Employee & Payroll dashboard
│   ├── AlertDialog.java                        # Custom modal AWT alert dialog
│   └── ConfirmDialog.java                      # Custom modal AWT confirm dialog
└── util/
    ├── PasswordHasher.java                     # PBKDF2-HMAC-SHA256 (600,000 iterations)
    ├── InputValidator.java                     # Form & data sanitizer
    └── ReportExporter.java                     # CSV and TXT report generation
```

---

## 2. Seed Accounts & Credentials

| Role | Username | Password | Email | Full Name |
| :--- | :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `Admin@1234` | `admin@srm.edu` | System Administrator |
| **HR** | `hr_user` | `HrUser@1234` | `hr@srm.edu` | Human Resources Lead |

> Passwords are encrypted with salt and PBKDF2WithHmacSHA256 with 600,000 rounds.

---

## 3. Database Setup (`db.properties`)

```properties
db.url=jdbc:mysql://localhost:3306/payroll_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000&socketTimeout=10000
db.username=payroll_app
db.password=Payroll@1234
```

Database scripts:
- `sql/schema.sql`: Full DDL with CHECK constraints and foreign keys.
- `sql/seed_data.sql`: Seed data for initial users and sample employees.

---

## 4. How to Run

### Run Application:
```powershell
java -cp "out/production/java;lib/mysql-connector-j-26.10.0.jar" com.srm.payroll.Main
```

### Run Test Matrix Suite (T01 - T28 verification):
```powershell
java -cp "out/production/java;out/test/java;lib/mysql-connector-j-26.10.0.jar" com.srm.payroll.TestMatrixSuite
```

---

## 5. Architectural Highlights & Design Rules Enforced
1. **Strict Downward Dependencies**: The UI layer communicates only with Service interfaces. No `java.sql.*` imports in UI.
2. **EDT Isolation**: All database operations and long-running tasks execute on dedicated worker threads (`payroll-batch`, worker threads), with UI updates posted via `java.awt.EventQueue.invokeLater(...)`.
3. **Pure AWT**: No Swing (`JFrame`, `JTable`, `JOptionPane`, `JProgressBar`) used. Replaced with `java.awt.Frame`, `java.awt.List`, `java.awt.Dialog` subclasses (`AlertDialog`, `ConfirmDialog`), and status labels.
4. **PreparedStatement Only**: 100% parameter binding; fully immune to SQL injection attacks (tested against `' OR '1'='1' --`, `admin'--`, and payload strings).
5. **Polymorphic Payroll Processing**: Permanent and Contract employees calculate gross, deductions, and net salary polymorphically without manual type casting.
6. **Atomic Transactions**: Batch payroll runs write all calculated results inside a single transactional block (`setAutoCommit(false)` / `executeBatch()` / `commit()` / `rollback()`).

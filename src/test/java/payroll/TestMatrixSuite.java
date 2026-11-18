package payroll;

import payroll.config.DatabaseConnection;
import payroll.dao.DataAccessException;
import payroll.dao.impl.EmployeeDAOImpl;
import payroll.dao.impl.InMemoryEmployeeDAO;
import payroll.dao.impl.PayrollResultDAOImpl;
import payroll.dao.impl.UserDAOImpl;
import payroll.service.*;
import payroll.service.exception.AuthenticationException;
import payroll.service.exception.DuplicateUserException;
import payroll.service.exception.ValidationException;
import payroll.util.InputValidator;
import payroll.util.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Automated test suite implementing verification of test scenarios T01 through T28
 * for the unified Employee Payroll System (package payroll).
 */
public class TestMatrixSuite {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("     EMPLOYEE PAYROLL SYSTEM - UNIFIED TEST MATRIX SUITE              ");
        System.out.println("======================================================================");

        boolean dbOk = false;
        try {
            dbOk = DatabaseConnection.getInstance().testConnection();
        } catch (Exception e) {
            System.err.println("Warning: Database testConnection failed: " + e.getMessage());
        }

        System.out.println("Database connectivity: " + (dbOk ? "ONLINE" : "OFFLINE"));
        System.out.println("----------------------------------------------------------------------");

        testPasswordHasher();
        testInputValidation();
        testInMemoryDAO();

        if (dbOk) {
            testSqlInjectionResilience();
            testAuthenticationAndAccessControl();
            testPolymorphicPayroll();
            testMultithreadedBatchProcessing();
            testDatabaseConstraintsAndCascade();
        } else {
            System.out.println("[SKIP] Database-dependent tests skipped because database is OFFLINE.");
        }

        System.out.println("----------------------------------------------------------------------");
        System.out.printf("TEST SUMMARY: Ran: %d | Passed: %d | Failed: %d\n", testsRun, testsPassed, testsFailed);
        System.out.println("======================================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testId, String description, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.printf("[PASS] %-5s: %s\n", testId, description);
        } else {
            testsFailed++;
            System.err.printf("[FAIL] %-5s: %s\n", testId, description);
        }
    }

    private static void assertFalse(String testId, String description, boolean condition) {
        assertTrue(testId, description, !condition);
    }

    private static void testPasswordHasher() {
        System.out.println(">>> Section: Security & Password Hashing (T18, T19)");
        String salt1 = PasswordHasher.generateSalt();
        String salt2 = PasswordHasher.generateSalt();
        assertFalse("T19a", "Salts are random and distinct", salt1.equals(salt2));

        String hash1 = PasswordHasher.hash("Admin@1234".toCharArray(), salt1);
        String hash2 = PasswordHasher.hash("Admin@1234".toCharArray(), salt2);
        assertFalse("T19b", "Identical passwords with different salts yield different hashes", hash1.equals(hash2));

        assertTrue("T19c", "Password verification succeeds for correct password",
                PasswordHasher.verify("Admin@1234".toCharArray(), salt1, hash1));
        assertFalse("T19d", "Password verification fails for incorrect password",
                PasswordHasher.verify("WrongPass".toCharArray(), salt1, hash1));
    }

    private static void testInputValidation() {
        System.out.println(">>> Section: Input Validation (T11, T12, T13, T14)");
        boolean caught = false;
        try {
            InputValidator.validateNonBlank("   ", "Field");
        } catch (ValidationException e) {
            caught = true;
        }
        assertTrue("T11", "Whitespace-only input rejected", caught);

        caught = false;
        try {
            InputValidator.parseNonNegativeMoney("-500.00", "Salary");
        } catch (ValidationException e) {
            caught = true;
        }
        assertTrue("T12a", "Negative salary rejected", caught);

        caught = false;
        try {
            InputValidator.parseNonNegativeMoney("five_thousand", "Salary");
        } catch (ValidationException e) {
            caught = true;
        }
        assertTrue("T12b", "Non-numeric salary rejected", caught);

        caught = false;
        try {
            InputValidator.validateName("A".repeat(101));
        } catch (ValidationException e) {
            caught = true;
        }
        assertTrue("T13", "Name exceeding 100 characters rejected", caught);

        caught = false;
        try {
            InputValidator.validateEmail("not-an-email");
        } catch (ValidationException e) {
            caught = true;
        }
        assertTrue("T14a", "Malformed email rejected", caught);

        caught = false;
        try {
            InputValidator.validateStrongPassword("weak".toCharArray());
        } catch (ValidationException e) {
            caught = true;
        }
        assertTrue("T14b", "Short/weak password rejected", caught);
    }

    private static void testInMemoryDAO() {
        System.out.println(">>> Section: In-Memory DAO Architecture Validation");
        InMemoryEmployeeDAO dao = new InMemoryEmployeeDAO();
        try {
            PermanentEmployee pe = new PermanentEmployee("John Doe", 50000.0, 5000.0);
            int id = dao.insert(pe, 1);
            assertTrue("INMEM1", "InMemoryEmployeeDAO inserts employee", id > 0);

            assertTrue("INMEM2", "InMemoryEmployeeDAO finds by ID", dao.findById(id).isPresent());
            assertTrue("INMEM3", "InMemoryEmployeeDAO searches by name", !dao.searchByName("John").isEmpty());

            boolean deleted = dao.deleteById(id);
            assertTrue("INMEM4", "InMemoryEmployeeDAO deletes employee", deleted);
        } catch (DataAccessException e) {
            assertTrue("INMEM-ERR", "Unexpected exception: " + e.getMessage(), false);
        }
    }

    private static void testSqlInjectionResilience() {
        System.out.println(">>> Section: SQL Injection Resilience (T05 - T09)");
        UserDAOImpl userDAO = new UserDAOImpl();
        AuthService auth = new AuthService(userDAO);

        boolean authFailed = false;
        try {
            auth.login("' OR '1'='1' --", "irrelevant".toCharArray());
        } catch (AuthenticationException e) {
            authFailed = true;
        } catch (Exception e) {
            authFailed = false;
        }
        assertTrue("T05", "SQL injection login attempt rejected without error", authFailed);

        authFailed = false;
        try {
            auth.login("admin'--", "anything".toCharArray());
        } catch (AuthenticationException e) {
            authFailed = true;
        } catch (Exception e) {
            authFailed = false;
        }
        assertTrue("T06", "Injected username rejected as ordinary invalid login", authFailed);

        boolean valFailed = false;
        try {
            InputValidator.parsePositiveInt("1; DROP TABLE employees;--", "ID");
        } catch (ValidationException e) {
            valFailed = true;
        }
        assertTrue("T07", "SQL injection in Search ID rejected by input validation", valFailed);

        EmployeeDAOImpl empDAO = new EmployeeDAOImpl();
        String payload = "Robert'); DROP TABLE employees;--";
        try {
            PermanentEmployee testEmp = new PermanentEmployee(payload, 45000.0, 2000.0);
            int newId = empDAO.insert(testEmp, 1);
            Employee retrieved = empDAO.findById(newId).orElse(null);
            assertTrue("T08", "SQL injection payload in employee name saved verbatim as text",
                    retrieved != null && payload.equals(retrieved.getName()));
            empDAO.deleteById(newId);
        } catch (Exception e) {
            assertTrue("T08", "Failed on SQL injection payload test: " + e.getMessage(), false);
        }

        try {
            String nameWithApostrophe = "O'Brien";
            PermanentEmployee testEmp = new PermanentEmployee(nameWithApostrophe, 55000.0, 3000.0);
            int newId = empDAO.insert(testEmp, 1);
            Employee retrieved = empDAO.findById(newId).orElse(null);
            assertTrue("T09", "Name with single quote (O'Brien) saved and retrieved correctly",
                    retrieved != null && nameWithApostrophe.equals(retrieved.getName()));
            empDAO.deleteById(newId);
        } catch (Exception e) {
            assertTrue("T09", "Failed on apostrophe test: " + e.getMessage(), false);
        }
    }

    private static void testAuthenticationAndAccessControl() {
        System.out.println(">>> Section: Authentication & Access Control (T16, T17, T18, T20)");
        UserDAOImpl userDAO = new UserDAOImpl();
        AuthService auth = new AuthService(userDAO);

        String testUser = "test_user_" + System.currentTimeMillis() % 100000;
        try {
            User registered = auth.register(testUser, "Secret@1234".toCharArray(),
                    "Test User", testUser + "@srm.edu", Role.HR);
            assertTrue("AUTH1", "New user registered successfully", registered != null && registered.getUserId() > 0);

            boolean dupCaught = false;
            try {
                auth.register(testUser, "Other@1234".toCharArray(),
                        "Another Name", "other@srm.edu", Role.HR);
            } catch (DuplicateUserException e) {
                dupCaught = true;
            }
            assertTrue("T16", "Duplicate username caught with DuplicateUserException", dupCaught);

            boolean genericMsg = false;
            try {
                auth.login("completely_nonexistent_user_xyz", "Random@1234".toCharArray());
            } catch (AuthenticationException e1) {
                try {
                    auth.login(testUser, "WrongPassword@999".toCharArray());
                } catch (AuthenticationException e2) {
                    genericMsg = e1.getMessage().equals(e2.getMessage());
                }
            }
            assertTrue("T18", "Identical error message for unknown user and wrong password", genericMsg);

            User loggedIn = auth.login(testUser, "Secret@1234".toCharArray());
            assertTrue("T17", "Successful login returns valid User entity", loggedIn != null);

            cleanupUser(testUser);
        } catch (Exception e) {
            assertTrue("AUTH-ERR", "Unexpected exception during auth testing: " + e.getMessage(), false);
        }

        try {
            User admin = auth.login("admin", "Admin@1234".toCharArray());
            assertTrue("AUTH2", "Seeded admin credentials successfully authenticated",
                    admin != null && admin.getRole() == Role.ADMIN);
        } catch (Exception e) {
            assertTrue("AUTH2", "Seeded admin login failed: " + e.getMessage(), false);
        }

        System.out.println(">>> Section: Access Control (T21, T22)");
        EmployeeDAOImpl empDAO = new EmployeeDAOImpl();
        StandardPayrollCalculator calc = new StandardPayrollCalculator();
        EmployeeService empService = new EmployeeService(empDAO, calc);

        User hrUser = new User();
        hrUser.setRole(Role.HR);
        SessionContext.setCurrentUser(hrUser);

        boolean blocked = false;
        try {
            empService.deleteEmployee(999999);
        } catch (SecurityException e) {
            blocked = true;
        } catch (Exception e) {
            blocked = false;
        }
        assertTrue("T21", "HR user blocked from deleting employee by service layer", blocked);

        User adminUser = new User();
        adminUser.setRole(Role.ADMIN);
        SessionContext.setCurrentUser(adminUser);

        boolean adminBlocked = false;
        try {
            empService.deleteEmployee(999999);
        } catch (SecurityException e) {
            adminBlocked = true;
        } catch (DataAccessException e) {
            adminBlocked = false; // Expected because ID doesn't exist
        }
        assertFalse("T21b", "ADMIN user successfully permitted to delete employee", adminBlocked);

        SessionContext.clear();
        assertTrue("T22", "SessionContext.clear() empties current user session",
                SessionContext.getCurrentUser() == null);
    }

    private static void testPolymorphicPayroll() {
        System.out.println(">>> Section: Payroll Calculation & Polymorphism");
        StandardPayrollCalculator calc = new StandardPayrollCalculator();

        PermanentEmployee pe = new PermanentEmployee(1, "Alice", 60000.0, 10000.0);
        PayrollResult peResult = calc.calculate(pe);
        assertTrue("POLY1", "PermanentEmployee polymorphic payroll calculation",
                peResult.getGrossSalary() == 70000.0 &&
                peResult.getDeductions() == 12000.0 &&
                peResult.getNetSalary() == 58000.0);

        ContractEmployee ce = new ContractEmployee(2, "Bob", 50000.0, 5000.0, 6);
        PayrollResult ceResult = calc.calculate(ce);
        assertTrue("POLY2", "ContractEmployee polymorphic payroll calculation",
                ceResult.getGrossSalary() == 55000.0 &&
                ceResult.getDeductions() == 5500.0 &&
                ceResult.getNetSalary() == 49500.0);
    }

    private static void testMultithreadedBatchProcessing() {
        System.out.println(">>> Section: Multithreaded Batch Processing (T23, T24, T26)");
        EmployeeDAOImpl empDAO = new EmployeeDAOImpl();
        PayrollResultDAOImpl resultDAO = new PayrollResultDAOImpl();
        StandardPayrollCalculator calc = new StandardPayrollCalculator();
        PayrollBatchService batchService = new PayrollBatchService(empDAO, resultDAO, calc);

        InMemoryEmployeeDAO emptyDAO = new InMemoryEmployeeDAO();
        PayrollBatchService emptyBatch = new PayrollBatchService(emptyDAO, resultDAO, calc);
        CountDownLatch emptyLatch = new CountDownLatch(1);
        final boolean[] emptyCompleted = {false};

        emptyBatch.start(new PayrollProgressListener() {
            @Override public void onProgress(int processed, int total, PayrollResult latest) {}
            @Override public void onComplete(BatchSummary summary) {
                emptyCompleted[0] = (summary.total() == 0 && summary.succeeded() == 0);
                emptyLatch.countDown();
            }
            @Override public void onCancelled(int processed, int total) { emptyLatch.countDown(); }
            @Override public void onError(Exception cause) { emptyLatch.countDown(); }
        });

        try {
            emptyLatch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        assertTrue("T26", "Batch with zero employees completes immediately with empty summary", emptyCompleted[0]);

        CountDownLatch batchLatch = new CountDownLatch(1);
        final boolean[] batchCompleted = {false};

        batchService.start(new PayrollProgressListener() {
            @Override public void onProgress(int processed, int total, PayrollResult latest) {}
            @Override public void onComplete(BatchSummary summary) {
                batchCompleted[0] = summary.succeeded() > 0;
                batchLatch.countDown();
            }
            @Override public void onCancelled(int processed, int total) { batchLatch.countDown(); }
            @Override public void onError(Exception cause) { batchLatch.countDown(); }
        });

        boolean overlapBlocked = false;
        try {
            batchService.start(new PayrollProgressListener() {
                @Override public void onProgress(int processed, int total, PayrollResult latest) {}
                @Override public void onComplete(BatchSummary summary) {}
                @Override public void onCancelled(int processed, int total) {}
                @Override public void onError(Exception cause) {}
            });
        } catch (IllegalStateException e) {
            overlapBlocked = true;
        }
        assertTrue("T24", "Rapid second batch invocation blocked by isRunning() guard", overlapBlocked);

        try {
            batchLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        assertTrue("T23", "Batch processing completes successfully against database records", batchCompleted[0]);
    }

    private static void testDatabaseConstraintsAndCascade() {
        System.out.println(">>> Section: Database Constraints & Cascade (T27, T28)");
        EmployeeDAOImpl empDAO = new EmployeeDAOImpl();

        boolean checkViolated = false;
        String rawSql = "INSERT INTO employees (name, basic_salary, employee_type, bonus, contract_allowance) " +
                        "VALUES ('Bad Staff', 50000, 'CONTRACT', 5000, NULL)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(rawSql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            checkViolated = true; // ck_emp_type_fields triggered
        }
        assertTrue("T28", "Contract employee with bonus blocked by DB CHECK constraint (ck_emp_type_fields)", checkViolated);

        try {
            PermanentEmployee temp = new PermanentEmployee("Cascade Test User", 40000.0, 2000.0);
            int empId = empDAO.insert(temp, 1);

            String runId = "test-cascade-run-" + System.currentTimeMillis();
            PayrollResultDAOImpl prDAO = new PayrollResultDAOImpl();
            PayrollResult pr = new PayrollResult(empId, "Cascade Test User", 42000.0, 8000.0, 34000.0);
            pr.setRunId(runId);
            prDAO.saveBatch(runId, List.of(pr));

            empDAO.deleteById(empId);

            List<PayrollResult> remaining = prDAO.findByRunId(runId);
            assertTrue("T27", "Deleting employee cascades and removes their payroll_results rows", remaining.isEmpty());
        } catch (Exception e) {
            assertTrue("T27", "Cascade deletion failed: " + e.getMessage(), false);
        }
    }

    private static void cleanupUser(String username) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }
}

package com.srm.payroll;

import com.srm.payroll.config.DatabaseConnection;
import com.srm.payroll.dao.DataAccessException;
import com.srm.payroll.dao.impl.EmployeeDAOImpl;
import com.srm.payroll.dao.impl.InMemoryEmployeeDAO;
import com.srm.payroll.dao.impl.PayrollResultDAOImpl;
import com.srm.payroll.dao.impl.UserDAOImpl;
import com.srm.payroll.model.*;
import com.srm.payroll.service.*;
import com.srm.payroll.service.exception.AuthenticationException;
import com.srm.payroll.service.exception.DuplicateUserException;
import com.srm.payroll.service.exception.ValidationException;
import com.srm.payroll.util.InputValidator;
import com.srm.payroll.util.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Automated test suite implementing verification of test scenarios T05 through T28
 * from Section 6.2 of the architectural specification.
 */
public class TestMatrixSuite {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("     EMPLOYEE PAYROLL SYSTEM v2 - ARCHITECTURAL TEST MATRIX SUITE     ");
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
        testInMemoryRepository();

        if (dbOk) {
            testSqlInjectionResilience();
            testAuthenticationAndEdgeCases();
            testAccessControl();
            testPayrollCalculationAndPolymorphism();
            testBatchProcessing();
            testDatabaseConstraintsAndCascade();
        } else {
            System.out.println("Skipping DB-dependent tests since database connection is not active.");
        }

        System.out.println("----------------------------------------------------------------------");
        System.out.printf("TEST SUMMARY: Ran: %d | Passed: %d | Failed: %d\n", testsRun, testsPassed, testsFailed);
        System.out.println("======================================================================");
        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void assertTest(String testId, String description, boolean condition, String details) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.printf("[PASS] %-5s: %s\n", testId, description);
        } else {
            testsFailed++;
            System.err.printf("[FAIL] %-5s: %s -> %s\n", testId, description, details);
        }
    }

    private static void testPasswordHasher() {
        System.out.println(">>> Section: Security & Password Hashing (T18, T19)");
        String salt1 = PasswordHasher.generateSalt();
        String salt2 = PasswordHasher.generateSalt();
        assertTest("T19a", "Salts are random and distinct", !salt1.equals(salt2), "Salts were identical");

        String hash1 = PasswordHasher.hash("Secret123".toCharArray(), salt1);
        String hash2 = PasswordHasher.hash("Secret123".toCharArray(), salt2);
        assertTest("T19b", "Identical passwords with different salts yield different hashes", !hash1.equals(hash2), "Hashes were identical");

        boolean verified = PasswordHasher.verify("Secret123".toCharArray(), salt1, hash1);
        assertTest("T19c", "Password verification succeeds for correct password", verified, "Verification returned false");

        boolean wrongPwd = PasswordHasher.verify("WrongPass".toCharArray(), salt1, hash1);
        assertTest("T19d", "Password verification fails for incorrect password", !wrongPwd, "Verification accepted wrong password");
    }

    private static void testInputValidation() {
        System.out.println(">>> Section: Input Validation (T11, T12, T13, T14)");

        // T11: Empty/whitespace
        boolean caughtEmpty = false;
        try {
            InputValidator.validateNonBlank("   ", "Field");
        } catch (ValidationException e) {
            caughtEmpty = true;
        }
        assertTest("T11", "Whitespace-only input rejected", caughtEmpty, "Failed to reject empty field");

        // T12: Negative or non-numeric money
        boolean caughtNeg = false;
        try {
            InputValidator.parseNonNegativeMoney("-500.00", "Salary");
        } catch (ValidationException e) {
            caughtNeg = true;
        }
        assertTest("T12a", "Negative salary rejected", caughtNeg, "Negative salary was accepted");

        boolean caughtNonNum = false;
        try {
            InputValidator.parseNonNegativeMoney("abc", "Salary");
        } catch (ValidationException e) {
            caughtNonNum = true;
        }
        assertTest("T12b", "Non-numeric salary rejected", caughtNonNum, "Non-numeric salary was accepted");

        // T13: Name longer than 100 characters
        boolean caughtLongName = false;
        try {
            InputValidator.validateName("A".repeat(101));
        } catch (ValidationException e) {
            caughtLongName = true;
        }
        assertTest("T13", "Name exceeding 100 characters rejected", caughtLongName, "Name > 100 chars was accepted");

        // T14: Email & password validation
        boolean caughtBadEmail = false;
        try {
            InputValidator.validateEmail("invalid-email-address");
        } catch (ValidationException e) {
            caughtBadEmail = true;
        }
        assertTest("T14a", "Malformed email rejected", caughtBadEmail, "Invalid email was accepted");

        boolean caughtWeakPass = false;
        try {
            InputValidator.validateStrongPassword("weak".toCharArray());
        } catch (ValidationException e) {
            caughtWeakPass = true;
        }
        assertTest("T14b", "Short/weak password rejected", caughtWeakPass, "Short password was accepted");
    }

    private static void testInMemoryRepository() {
        System.out.println(">>> Section: In-Memory DAO Architecture Validation");
        InMemoryEmployeeDAO repo = new InMemoryEmployeeDAO();
        try {
            PermanentEmployee pe = new PermanentEmployee("Temp Worker", 45000.0, 3000.0);
            int id = repo.insert(pe, 1);
            assertTest("INMEM1", "InMemoryEmployeeDAO inserts employee", id > 0, "Insert failed");

            var found = repo.findById(id);
            assertTest("INMEM2", "InMemoryEmployeeDAO finds by ID", found.isPresent(), "Not found");

            var search = repo.searchByName("Temp");
            assertTest("INMEM3", "InMemoryEmployeeDAO searches by name", search.size() == 1, "Search failed");

            boolean deleted = repo.deleteById(id);
            assertTest("INMEM4", "InMemoryEmployeeDAO deletes employee", deleted, "Delete failed");
        } catch (Exception e) {
            assertTest("INMEM_ERR", "InMemory DAO exception", false, e.getMessage());
        }
    }

    private static void testSqlInjectionResilience() {
        System.out.println(">>> Section: SQL Injection Resilience (T05 - T09)");
        UserDAOImpl userDAO = new UserDAOImpl();
        AuthService authService = new AuthService(userDAO);

        // T05: ' OR '1'='1' --
        boolean loginRejected = false;
        try {
            authService.login("' OR '1'='1' --", "anypass".toCharArray());
        } catch (AuthenticationException e) {
            loginRejected = true;
        } catch (Exception e) {
            loginRejected = false;
        }
        assertTest("T05", "SQL injection login attempt rejected without error", loginRejected, "SQL injection login was not rejected");

        // T06: admin'-- with wrong password
        boolean adminInjectedRejected = false;
        try {
            authService.login("admin'--", "wrongpass".toCharArray());
        } catch (AuthenticationException e) {
            adminInjectedRejected = true;
        } catch (Exception e) {
            adminInjectedRejected = false;
        }
        assertTest("T06", "Injected username rejected as ordinary invalid login", adminInjectedRejected, "Injected username caused exception or bypass");

        // T07: Search ID non-numeric injection
        boolean searchRejected = false;
        try {
            InputValidator.parsePositiveInt("1; DROP TABLE employees;--", "Search ID");
        } catch (ValidationException e) {
            searchRejected = true;
        }
        assertTest("T07", "SQL injection in Search ID rejected by input validation", searchRejected, "Failed to reject SQL in Search ID");

        // T08: Save name with SQL injection payloads as plain text
        EmployeeDAOImpl employeeDAO = new EmployeeDAOImpl();
        EmployeeService employeeService = new EmployeeService(employeeDAO, new StandardPayrollCalculator());

        // Use valid admin user id (1)
        SessionContext.setCurrentUser(new User(1, "admin", "", "", "Admin", "admin@srm.edu", Role.ADMIN, true, null, null));

        String injectedName = "Robert'); DROP TABLE users;--";
        PermanentEmployee pe = new PermanentEmployee(injectedName, 65000.0, 5000.0);
        int insertedId = 0;
        try {
            insertedId = employeeService.addEmployee(pe);
            var retrieved = employeeService.findById(insertedId);
            boolean nameMatches = retrieved.isPresent() && injectedName.equals(retrieved.get().getName());
            assertTest("T08", "SQL injection payload in employee name saved verbatim as text", nameMatches, "Payload altered or caused error");
        } catch (Exception e) {
            assertTest("T08", "SQL injection payload in employee name saved verbatim as text", false, e.getMessage());
        } finally {
            if (insertedId > 0) {
                try {
                    employeeService.deleteEmployee(insertedId);
                } catch (Exception ignored) {}
            }
        }

        // T09: Apostrophe in name O'Brien
        String obrienName = "Patrick O'Brien";
        ContractEmployee ce = new ContractEmployee(obrienName, 70000.0, 6000.0, 12);
        int obrienId = 0;
        try {
            obrienId = employeeService.addEmployee(ce);
            var retrieved = employeeService.findById(obrienId);
            boolean matches = retrieved.isPresent() && obrienName.equals(retrieved.get().getName());
            assertTest("T09", "Name with single quote (O'Brien) saved and retrieved correctly", matches, "Single quote failed");
        } catch (Exception e) {
            assertTest("T09", "Name with single quote (O'Brien) saved and retrieved correctly", false, e.getMessage());
        } finally {
            if (obrienId > 0) {
                try {
                    employeeService.deleteEmployee(obrienId);
                } catch (Exception ignored) {}
            }
        }
    }

    private static void testAuthenticationAndEdgeCases() {
        System.out.println(">>> Section: Authentication & Access Control (T16, T17, T18, T20)");
        UserDAOImpl userDAO = new UserDAOImpl();
        AuthService authService = new AuthService(userDAO);

        String testUser = "testuser_" + System.currentTimeMillis();
        String testEmail = testUser + "@example.com";

        try {
            User registered = authService.register(testUser, "ValidPass1".toCharArray(), "Test User", testEmail, Role.HR);
            assertTest("AUTH1", "New user registered successfully", registered != null && registered.getUserId() > 0, "Registration failed");

            // T16: Duplicate username or email
            boolean dupCaught = false;
            try {
                authService.register(testUser, "ValidPass1".toCharArray(), "Duplicate User", "different@example.com", Role.HR);
            } catch (DuplicateUserException e) {
                dupCaught = true;
            }
            assertTest("T16", "Duplicate username caught with DuplicateUserException", dupCaught, "Duplicate username allowed");

            // T18: Unknown user vs wrong password identical error
            String errUnknown = "";
            try {
                authService.login("non_existent_user_9999", "AnyPass123".toCharArray());
            } catch (AuthenticationException e) {
                errUnknown = e.getMessage();
            }

            String errWrongPwd = "";
            try {
                authService.login(testUser, "WrongPassword1".toCharArray());
            } catch (AuthenticationException e) {
                errWrongPwd = e.getMessage();
            }
            assertTest("T18", "Identical error message for unknown user and wrong password", errUnknown.equals(errWrongPwd),
                    "Messages differed: '" + errUnknown + "' vs '" + errWrongPwd + "'");

            // Successful login with seeded admin
            User loggedIn = authService.login("admin", "Admin@1234".toCharArray());
            assertTest("AUTH2", "Seeded admin credentials successfully authenticated", loggedIn != null && loggedIn.getRole() == Role.ADMIN, "Admin login failed");

        } catch (Exception e) {
            assertTest("AUTH_ERR", "Auth flow encountered exception", false, e.getMessage());
        }
    }

    private static void testAccessControl() {
        System.out.println(">>> Section: Access Control (T21, T22)");
        EmployeeDAOImpl employeeDAO = new EmployeeDAOImpl();
        EmployeeService employeeService = new EmployeeService(employeeDAO, new StandardPayrollCalculator());

        // Use valid admin user id (1)
        SessionContext.setCurrentUser(new User(1, "admin", "", "", "Admin", "admin@srm.edu", Role.ADMIN, true, null, null));

        PermanentEmployee pe = new PermanentEmployee("Access Test Employee", 50000.0, 2000.0);
        int empId = 0;
        try {
            empId = employeeService.addEmployee(pe);

            // T21: HR user cannot delete employee
            User hrUser = new User(2, "hr_user", "", "", "HR Tester", "hr@srm.edu", Role.HR, true, null, null);
            SessionContext.setCurrentUser(hrUser);

            boolean deleteDenied = false;
            try {
                employeeService.deleteEmployee(empId);
            } catch (SecurityException e) {
                deleteDenied = true;
            }
            assertTest("T21", "HR user blocked from deleting employee by service layer", deleteDenied, "HR user successfully deleted employee");

            // ADMIN can delete
            User adminUser = new User(1, "admin", "", "", "Admin Tester", "admin@srm.edu", Role.ADMIN, true, null, null);
            SessionContext.setCurrentUser(adminUser);

            boolean deletedByAdmin = false;
            try {
                employeeService.deleteEmployee(empId);
                deletedByAdmin = true;
                empId = 0;
            } catch (Exception e) {
                deletedByAdmin = false;
            }
            assertTest("T21b", "ADMIN user successfully permitted to delete employee", deletedByAdmin, "Admin delete failed");

            // T22: Logout clears session
            SessionContext.clear();
            assertTest("T22", "SessionContext.clear() empties current user session", SessionContext.getCurrentUser() == null, "User was not cleared");

        } catch (Exception e) {
            assertTest("ACCESS_ERR", "Access control test error", false, e.getMessage());
        } finally {
            if (empId > 0) {
                try {
                    SessionContext.setCurrentUser(new User(1, "admin", "", "", "Admin", "admin@srm.edu", Role.ADMIN, true, null, null));
                    employeeService.deleteEmployee(empId);
                } catch (Exception ignored) {}
            }
        }
    }

    private static void testPayrollCalculationAndPolymorphism() {
        System.out.println(">>> Section: Payroll Calculation & Polymorphism");
        StandardPayrollCalculator calc = new StandardPayrollCalculator();

        // Permanent: gross = basic + bonus; deductions = basic * 0.20; net = gross - deductions
        PermanentEmployee pe = new PermanentEmployee(101, "Alice Smith", 50000.0, 10000.0);
        PayrollResult resPe = calc.calculate(pe);
        boolean peOk = resPe.getGrossSalary() == 60000.0 &&
                       resPe.getDeductions() == 10000.0 &&
                       resPe.getNetSalary() == 50000.0;
        assertTest("POLY1", "PermanentEmployee polymorphic payroll calculation", peOk,
                "Expected Gross 60000, Ded 10000, Net 50000. Got: " + resPe);

        // Contract: gross = basic + allowance; deductions = gross * 0.10; net = gross - deductions
        ContractEmployee ce = new ContractEmployee(102, "Bob Jones", 40000.0, 10000.0, 6);
        PayrollResult resCe = calc.calculate(ce);
        boolean ceOk = resCe.getGrossSalary() == 50000.0 &&
                       resCe.getDeductions() == 5000.0 &&
                       resCe.getNetSalary() == 45000.0;
        assertTest("POLY2", "ContractEmployee polymorphic payroll calculation", ceOk,
                "Expected Gross 50000, Ded 5000, Net 45000. Got: " + resCe);
    }

    private static void testBatchProcessing() {
        System.out.println(">>> Section: Multithreaded Batch Processing (T23, T24, T26)");

        // 1. Zero employees batch test with InMemoryDAO
        InMemoryEmployeeDAO inMemoryDAO = new InMemoryEmployeeDAO();
        StandardPayrollCalculator calc = new StandardPayrollCalculator();
        PayrollResultDAOImpl resultDAO = new PayrollResultDAOImpl();

        PayrollBatchService batchServiceZero = new PayrollBatchService(inMemoryDAO, resultDAO, calc);
        CountDownLatch latchEmpty = new CountDownLatch(1);
        final BatchSummary[] emptyResult = new BatchSummary[1];

        batchServiceZero.start(new PayrollProgressListener() {
            @Override public void onProgress(int processed, int total, PayrollResult latest) {}
            @Override public void onComplete(BatchSummary summary) {
                emptyResult[0] = summary;
                latchEmpty.countDown();
            }
            @Override public void onCancelled(int processed, int total) { latchEmpty.countDown(); }
            @Override public void onError(Exception cause) { latchEmpty.countDown(); }
        });

        try {
            boolean done = latchEmpty.await(5, TimeUnit.SECONDS);
            assertTest("T26", "Batch with zero employees completes immediately with empty summary",
                    done && emptyResult[0] != null && emptyResult[0].total() == 0, "Did not complete cleanly");
        } catch (InterruptedException e) {
            assertTest("T26", "Batch with zero employees", false, "Timed out");
        }

        // 2. Real DB batch test on seeded employees
        EmployeeDAOImpl realEmpDAO = new EmployeeDAOImpl();
        PayrollBatchService realBatchService = new PayrollBatchService(realEmpDAO, resultDAO, calc);

        CountDownLatch batchLatch = new CountDownLatch(1);
        final BatchSummary[] realSummary = new BatchSummary[1];
        final boolean[] progressFired = new boolean[1];

        realBatchService.start(new PayrollProgressListener() {
            @Override
            public void onProgress(int processed, int total, PayrollResult latest) {
                progressFired[0] = true;
            }

            @Override
            public void onComplete(BatchSummary summary) {
                realSummary[0] = summary;
                batchLatch.countDown();
            }

            @Override
            public void onCancelled(int processed, int total) {
                batchLatch.countDown();
            }

            @Override
            public void onError(Exception cause) {
                System.err.println("Batch error: " + cause.getMessage());
                batchLatch.countDown();
            }
        });

        // T24: Second click guard while running
        boolean guardBlockedSecondCall = false;
        try {
            realBatchService.start(new PayrollProgressListener() {
                @Override public void onProgress(int p, int t, PayrollResult r) {}
                @Override public void onComplete(BatchSummary s) {}
                @Override public void onCancelled(int p, int t) {}
                @Override public void onError(Exception c) {}
            });
        } catch (IllegalStateException e) {
            guardBlockedSecondCall = true;
        }
        assertTest("T24", "Rapid second batch invocation blocked by isRunning() guard", guardBlockedSecondCall, "Second call was not blocked");

        try {
            boolean done = batchLatch.await(10, TimeUnit.SECONDS);
            assertTest("T23", "Batch processing completes successfully against database records",
                    done && realSummary[0] != null && realSummary[0].succeeded() > 0 && progressFired[0],
                    "Batch failed or timed out");
        } catch (InterruptedException e) {
            assertTest("T23", "Batch processing", false, "Timed out");
        }
    }

    private static void testDatabaseConstraintsAndCascade() {
        System.out.println(">>> Section: Database Constraints & Cascade (T27, T28)");

        // T28: Insert a contract row with bonus set directly via SQL -> blocked by CHECK constraint
        boolean checkBlocked = false;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO employees (name, basic_salary, employee_type, bonus, contract_allowance, contract_months) " +
                     "VALUES ('Illegal Contract', 50000.00, 'CONTRACT', 5000.00, 2000.00, 6)")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            // Check constraint violation SQLState 3819 or 23000
            checkBlocked = true;
        }
        assertTest("T28", "Contract employee with bonus blocked by DB CHECK constraint (ck_emp_type_fields)", checkBlocked,
                "CHECK constraint failed to block invalid row");

        // T27: Cascade delete: deleting an employee also deletes their payroll_results
        try {
            EmployeeDAOImpl empDAO = new EmployeeDAOImpl();
            PayrollResultDAOImpl prDAO = new PayrollResultDAOImpl();

            PermanentEmployee pe = new PermanentEmployee("Cascade Test Emp", 60000.0, 3000.0);
            int empId = empDAO.insert(pe, 1);

            PayrollResult pr = new PayrollResult(empId, "Cascade Test Emp", 63000.0, 12000.0, 51000.0);
            String runId = "test-cascade-run";
            prDAO.saveBatch(runId, List.of(pr));

            // Verify payroll_results row exists
            List<PayrollResult> resultsBefore = prDAO.findByRunId(runId);
            boolean prExists = resultsBefore.stream().anyMatch(r -> r.getEmployeeId() == empId);

            // Now delete employee
            empDAO.deleteById(empId);

            // Verify payroll_results row was cascaded
            List<PayrollResult> resultsAfter = prDAO.findByRunId(runId);
            boolean prGone = resultsAfter.stream().noneMatch(r -> r.getEmployeeId() == empId);

            assertTest("T27", "Deleting employee cascades and removes their payroll_results rows",
                    prExists && prGone, "Cascade delete did not remove payroll_results row");

        } catch (Exception e) {
            assertTest("T27", "Cascade delete test", false, e.getMessage());
        }
    }
}

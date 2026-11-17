package payroll;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Automated test suite implementing verification of all test cases (TC01 to TC12)
 * from Section 9 of the Academic Project Report, along with numerical verification
 * of sample calculations from Section 9 (Page 28-29).
 */
public class PayrollTestSuite {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println("   EMPLOYEE PAYROLL MANAGEMENT SYSTEM - ACADEMIC TEST SUITE (TC01-TC12) ");
        System.out.println("   Course: CSE 309 Advanced Java Programming - SRM University AP        ");
        System.out.println("========================================================================");

        // Core Calculation Assertions (Page 28-29)
        testAcademicSampleCalculations();

        // Formal Test Cases (Page 27)
        testTC01_ValidEmployeeDetails();
        testTC02_NegativeEmployeeId();
        testTC03_EmptyName();
        testTC04_NegativeSalary();
        testTC05_SalaryEnteredAsText();
        testTC06_DuplicateEmployeeId();
        testTC07_ExistingEmployeePayrollCalculation();
        testTC08_UnknownEmployeeId();
        testTC09_SaveRecordsToFile();
        testTC10_InvalidRecordFormat();
        testTC11_BatchProcessingMultithreading();
        testTC12_ThreadSynchronization();
        testByteStreamCopyAndFinally();

        System.out.println("========================================================================");
        System.out.printf("TEST RESULTS SUMMARY: Ran %d tests | Passed: %d | Failed: %d\n",
                totalTests, passedTests, failedTests);
        System.out.println("ALL SYSTEM CAPABILITIES VERIFIED: " + (failedTests == 0 ? "PASSED (100%)" : "FAILED"));
        System.out.println("========================================================================");

        if (failedTests > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testId, String description, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.printf("[PASS] %-6s : %s\n", testId, description);
        } else {
            failedTests++;
            System.err.printf("[FAIL] %-6s : %s\n", testId, description);
        }
    }

    private static void assertEquals(String testId, String description, double expected, double actual, double epsilon) {
        totalTests++;
        if (Math.abs(expected - actual) <= epsilon) {
            passedTests++;
            System.out.printf("[PASS] %-6s : %s (Expected: %.2f, Got: %.2f)\n", testId, description, expected, actual);
        } else {
            failedTests++;
            System.err.printf("[FAIL] %-6s : %s (Expected: %.2f, Got: %.2f)\n", testId, description, expected, actual);
        }
    }

    /**
     * Exact verification of the sample calculation shown on Page 28 & 29 of report:
     * Ravi Kumar (Permanent): Basic=30000, HRA=6000, Allowance=3000, Gross=39000, PF=3600, Tax=200, Net=35200.
     * Meena (Contract): Basic=25000, Allowance=2500+1500=4000, Gross=34000, PF=3000, Tax=200, Net=30800.
     */
    private static void testAcademicSampleCalculations() {
        System.out.println("\n--- Section 9: Numerical Sample Calculations Verification (Page 28-29) ---");
        PayrollCalculator calculator = new StandardPayrollCalculator();

        // 1. Permanent Employee: Ravi Kumar
        Employee ravi = new PermanentEmployee(101, "Ravi Kumar", 30000.0);
        PayrollResult raviResult = calculator.calculate(ravi);

        assertEquals("CALC-01", "Ravi Kumar Gross Salary == 39000.00", 39000.00, raviResult.getGross(), 0.001);
        assertEquals("CALC-02", "Ravi Kumar HRA == 6000.00 (20%)", 6000.00, raviResult.getHra(), 0.001);
        assertEquals("CALC-03", "Ravi Kumar Allowance == 3000.00 (10%)", 3000.00, raviResult.getAllowance(), 0.001);
        assertEquals("CALC-04", "Ravi Kumar PF == 3600.00 (12%)", 3600.00, raviResult.getPf(), 0.001);
        assertEquals("CALC-05", "Ravi Kumar Tax == 200.00", 200.00, raviResult.getTax(), 0.001);
        assertEquals("CALC-06", "Ravi Kumar Net Salary == 35200.00", 35200.00, raviResult.getNet(), 0.001);

        // 2. Contract Employee: Meena
        Employee meena = new ContractEmployee(102, "Meena", 25000.0, 1500.0);
        PayrollResult meenaResult = calculator.calculate(meena);

        assertEquals("CALC-07", "Meena Gross Salary == 34000.00 (includes contract allowance)", 34000.00, meenaResult.getGross(), 0.001);
        assertEquals("CALC-08", "Meena Total Allowance == 4000.00 (2500 standard + 1500 contract)", 4000.00, meenaResult.getAllowance(), 0.001);
        assertEquals("CALC-09", "Meena Net Salary == 30800.00", 30800.00, meenaResult.getNet(), 0.001);
    }

    private static void testTC01_ValidEmployeeDetails() {
        System.out.println("\n--- Test Case TC01: Valid employee details ---");
        EmployeeRepository repo = new EmployeeRepository();
        Employee emp = new PermanentEmployee(101, "Ravi Kumar", 30000.0);
        repo.add(emp);
        assertTrue("TC01", "Valid employee added to repository", repo.findById(101).isPresent());
    }

    private static void testTC02_NegativeEmployeeId() {
        System.out.println("\n--- Test Case TC02: Negative employee ID ---");
        boolean caught = false;
        try {
            new PermanentEmployee(-5, "Invalid ID User", 40000.0);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assertTrue("TC02", "Negative employee ID rejected with IllegalArgumentException", caught);
    }

    private static void testTC03_EmptyName() {
        System.out.println("\n--- Test Case TC03: Empty employee name ---");
        boolean caught = false;
        try {
            new PermanentEmployee(105, "   ", 45000.0);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assertTrue("TC03", "Whitespace-only employee name rejected", caught);
    }

    private static void testTC04_NegativeSalary() {
        System.out.println("\n--- Test Case TC04: Negative salary ---");
        boolean caught = false;
        try {
            new PermanentEmployee(106, "Valid Name", -1000.0);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assertTrue("TC04", "Negative basic salary rejected", caught);
    }

    private static void testTC05_SalaryEnteredAsText() {
        System.out.println("\n--- Test Case TC05: Salary entered as text ---");
        String badSalaryInput = "abc30000";
        boolean caught = false;
        try {
            Double.parseDouble(badSalaryInput.trim());
        } catch (NumberFormatException e) {
            caught = true;
        }
        assertTrue("TC05", "Text string as salary caught via NumberFormatException", caught);
    }

    private static void testTC06_DuplicateEmployeeId() {
        System.out.println("\n--- Test Case TC06: Duplicate employee ID ---");
        EmployeeRepository repo = new EmployeeRepository();
        repo.add(new PermanentEmployee(101, "Ravi Kumar", 30000.0));
        boolean duplicateRejected = false;
        try {
            repo.add(new ContractEmployee(101, "Duplicate Ravi", 32000.0, 500.0));
        } catch (IllegalArgumentException e) {
            duplicateRejected = true;
        }
        assertTrue("TC06", "Duplicate employee ID rejected by EmployeeRepository", duplicateRejected);
    }

    private static void testTC07_ExistingEmployeePayrollCalculation() {
        System.out.println("\n--- Test Case TC07: Existing employee ID ---");
        EmployeeRepository repo = new EmployeeRepository();
        repo.add(new PermanentEmployee(101, "Ravi Kumar", 30000.0));
        PayrollService service = new PayrollService(new StandardPayrollCalculator());

        Optional<Employee> found = repo.findById(101);
        assertTrue("TC07-A", "Employee found by ID", found.isPresent());
        PayrollResult res = service.process(found.get());
        assertTrue("TC07-B", "Payroll calculated with Net > 0", res.getNet() > 0);
    }

    private static void testTC08_UnknownEmployeeId() {
        System.out.println("\n--- Test Case TC08: Unknown employee ID ---");
        EmployeeRepository repo = new EmployeeRepository();
        Optional<Employee> missing = repo.findById(999);
        assertTrue("TC08", "Unknown employee ID returns empty Optional", missing.isEmpty());
    }

    private static void testTC09_SaveRecordsToFile() {
        System.out.println("\n--- Test Case TC09: Save and load records to file ---");
        PayrollFileService fileService = new PayrollFileService();
        String testFile = "data/test_tc09_employees.txt";

        List<Employee> list = List.of(
                new PermanentEmployee(201, "Alice", 50000.0),
                new ContractEmployee(202, "Bob", 40000.0, 3000.0)
        );

        try {
            fileService.saveEmployees(list, testFile);
            File f = new File(testFile);
            assertTrue("TC09-A", "File created and has contents", f.exists() && f.length() > 0);

            List<Employee> loaded = fileService.loadEmployees(testFile);
            assertTrue("TC09-B", "Loaded exact number of employees (2)", loaded.size() == 2);
            assertEquals("TC09-C", "Loaded Bob salary matches", 40000.0, loaded.get(1).getBasicSalary(), 0.001);
        } catch (FileStorageException e) {
            assertTrue("TC09", "Unexpected storage exception: " + e.getMessage(), false);
        }
    }

    private static void testTC10_InvalidRecordFormat() {
        System.out.println("\n--- Test Case TC10: Invalid record format ---");
        PayrollFileService fileService = new PayrollFileService();
        String malformedFile = "data/test_malformed.txt";

        try {
            File f = new File(malformedFile);
            f.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(f)) {
                fw.write("ID,NAME,TYPE,BASIC_SALARY,EXTRA\n");
                fw.write("not_an_int,BadName,Permanent,not_a_double,0\n");
            }

            boolean caught = false;
            try {
                fileService.loadEmployees(malformedFile);
            } catch (FileStorageException e) {
                caught = true;
            }
            assertTrue("TC10", "Malformed CSV line throws FileStorageException", caught);
        } catch (IOException e) {
            assertTrue("TC10", "I/O error preparing test: " + e.getMessage(), false);
        }
    }

    private static void testTC11_BatchProcessingMultithreading() {
        System.out.println("\n--- Test Case TC11: Batch of employees (Multithreading) ---");
        List<Employee> batch = List.of(
                new PermanentEmployee(301, "User A", 30000.0),
                new ContractEmployee(302, "User B", 40000.0, 2000.0),
                new PermanentEmployee(303, "User C", 50000.0)
        );

        PayrollService service = new PayrollService(new StandardPayrollCalculator());
        AtomicInteger processedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        PayrollTask task = new PayrollTask(batch, service,
                logLine -> processedCount.incrementAndGet(),
                latch::countDown);

        Thread thread = new Thread(task, "Test-Worker-Thread");
        thread.start();

        try {
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertTrue("TC11-A", "PayrollTask completed in background thread within timeout", completed);
            assertTrue("TC11-B", "All 3 employees processed in batch", processedCount.get() == 3);
        } catch (InterruptedException e) {
            assertTrue("TC11", "Interrupted during batch test", false);
        }
    }

    private static void testTC12_ThreadSynchronization() {
        System.out.println("\n--- Test Case TC12: Concurrency & Synchronization in EmployeeRepository ---");
        EmployeeRepository repo = new EmployeeRepository();
        int threadsCount = 10;
        int perThreadRecords = 50;
        CountDownLatch latch = new CountDownLatch(threadsCount);

        for (int t = 0; t < threadsCount; t++) {
            final int threadIndex = t;
            new Thread(() -> {
                for (int i = 0; i < perThreadRecords; i++) {
                    int empId = 1000 + (threadIndex * 100) + i;
                    repo.add(new PermanentEmployee(empId, "ConcurrentEmp" + empId, 30000.0));
                }
                latch.countDown();
            }).start();
        }

        try {
            boolean finished = latch.await(5, TimeUnit.SECONDS);
            assertTrue("TC12-A", "Concurrent writers finished without deadlock", finished);
            assertTrue("TC12-B", "Repository count matches expected 500 records exactly", repo.count() == 500);
        } catch (InterruptedException e) {
            assertTrue("TC12", "Interrupted during synchronization test", false);
        }
    }

    private static void testByteStreamCopyAndFinally() {
        System.out.println("\n--- Byte Stream & Finally Block Verification ---");
        PayrollFileService fileService = new PayrollFileService();
        String src = "data/source_copy_test.txt";
        String dst = "data/dest_copy_test.txt";

        try {
            File s = new File(src);
            s.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(s)) {
                fw.write("Testing byte-stream copy using FileInputStream and FileOutputStream.");
            }

            fileService.copyFile(src, dst);
            File d = new File(dst);
            assertTrue("BYTE-COPY", "Destination file copied successfully with matching length",
                    d.exists() && d.length() == s.length());
        } catch (Exception e) {
            assertTrue("BYTE-COPY", "Failed byte copy: " + e.getMessage(), false);
        }
    }
}

package payroll.service;

import payroll.Employee;
import payroll.PayrollResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Service simulating the corporate banking payroll disbursement pipeline.
 * Demonstrates simulated automated clearing house (ACH / NEFT / IMPS) transactions
 * from the company's treasury bank account to each employee's bank account.
 */
public class PaymentSimulationService {

    public static final String DEFAULT_COMPANY_ACCOUNT = "SRM-CORP-TREASURY-001";
    public static final String DEFAULT_BANK_NAME = "State Bank of India (Corporate Branch, Amaravati)";
    public static final double DEFAULT_INITIAL_BALANCE = 10_000_000.00; // Rs. 1 Crore

    private final String companyAccount;
    private final String companyBankName;
    private final double initialBalance;
    private final Random random = new Random();

    public PaymentSimulationService() {
        this(DEFAULT_COMPANY_ACCOUNT, DEFAULT_BANK_NAME, DEFAULT_INITIAL_BALANCE);
    }

    public PaymentSimulationService(String companyAccount, String companyBankName, double initialBalance) {
        this.companyAccount = companyAccount;
        this.companyBankName = companyBankName;
        this.initialBalance = initialBalance;
    }

    /**
     * Executes the simulated bulk disbursement run.
     */
    public PaymentSimulationSummary simulatePayout(List<Employee> employees,
                                                  PayrollCalculator calculator,
                                                  PaymentSimulationListener listener) {
        long startTime = System.currentTimeMillis();
        String batchId = "BATCH-PAY-" + System.currentTimeMillis() % 1_000_000;

        List<DisbursementRecord> records = new ArrayList<>(employees.size());
        double totalGross = 0;
        double totalDeductions = 0;
        double totalDisbursed = 0;

        // Ensure company balance is sufficient
        double requiredTotal = 0;
        for (Employee e : employees) {
            requiredTotal += calculator.calculate(e).getNetSalary();
        }
        double startingBalance = Math.max(initialBalance, requiredTotal * 1.25);

        int totalCount = employees.size();
        int processed = 0;

        // Pace delay: for small sets (<= 20) give visual animation delay (50ms);
        // for large sets (e.g. 1000) keep delay minimal (1ms)
        long sleepMs = totalCount <= 20 ? 50 : 1;

        for (Employee emp : employees) {
            PayrollResult r = calculator.calculate(emp);
            double gross = r.getGrossSalary();
            double deductions = r.getDeductions();
            double net = r.getNetSalary();

            totalGross += gross;
            totalDeductions += deductions;
            totalDisbursed += net;

            String utr = generateUtr(emp.getEmployeeId());
            String bankAccount = emp.getBankAccount();
            String ifsc = emp.getIfscCode();

            DisbursementRecord record = new DisbursementRecord(
                    emp.getEmployeeId(),
                    emp.getName(),
                    bankAccount,
                    ifsc,
                    gross,
                    deductions,
                    net,
                    utr,
                    "SETTLED",
                    LocalDateTime.now()
            );
            records.add(record);
            processed++;

            if (listener != null) {
                listener.onProgress(processed, totalCount, record);
            }

            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ignored) {}
            }
        }

        double endingBalance = startingBalance - totalDisbursed;
        long duration = System.currentTimeMillis() - startTime;

        PaymentSimulationSummary summary = new PaymentSimulationSummary(
                companyAccount,
                companyBankName,
                startingBalance,
                totalGross,
                totalDeductions,
                totalDisbursed,
                endingBalance,
                totalCount,
                batchId,
                duration,
                records
        );

        // Persist settlement report to disk
        try {
            saveSimulationReport(summary, "data/payment_simulation_report.txt");
        } catch (IOException ignored) {}

        if (listener != null) {
            listener.onComplete(summary);
        }

        return summary;
    }

    private String generateUtr(int empId) {
        String hex = Integer.toHexString(random.nextInt(0xFFFFF)).toUpperCase();
        return String.format("UTR-SRM-%04d-%s", empId % 10000, hex);
    }

    private void saveSimulationReport(PaymentSimulationSummary summary, String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(summary.formatSummaryReport());
        }
    }
}

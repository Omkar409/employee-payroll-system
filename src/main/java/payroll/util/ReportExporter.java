package payroll.util;

import payroll.BatchSummary;
import payroll.PayrollResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility for exporting payroll batch reports to CSV and formatted text files.
 */
public final class ReportExporter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReportExporter() {}

    public static File exportToCsv(String runId, List<PayrollResult> results, BatchSummary summary, String destinationDir)
            throws IOException {
        File dir = new File(destinationDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, "payroll_batch_" + runId + ".csv");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Run ID,Employee ID,Employee Name,Gross Salary,Deductions,Net Salary,Processed At");
            writer.newLine();

            for (PayrollResult r : results) {
                writer.write(String.format("%s,%d,\"%s\",%.2f,%.2f,%.2f,%s",
                        r.getRunId(),
                        r.getEmployeeId(),
                        r.getEmployeeName() != null ? r.getEmployeeName().replace("\"", "\"\"") : "",
                        r.getGrossSalary(),
                        r.getDeductions(),
                        r.getNetSalary(),
                        r.getProcessedAt() != null ? r.getProcessedAt().format(FORMATTER) : ""));
                writer.newLine();
            }

            writer.newLine();
            writer.write(String.format("# Summary: Total=%d, Succeeded=%d, Failed=%d, Gross=%.2f, Net=%.2f, DurationMs=%d",
                    summary.total(), summary.succeeded(), summary.failed(),
                    summary.totalGross(), summary.totalNet(), summary.durationMillis()));
            writer.newLine();
        }
        return file;
    }

    public static File exportToText(String runId, List<PayrollResult> results, BatchSummary summary, String destinationDir)
            throws IOException {
        File dir = new File(destinationDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, "payroll_batch_" + runId + ".txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("=========================================================================================\n");
            writer.write("                              PAYROLL BATCH EXECUTION REPORT                             \n");
            writer.write("=========================================================================================\n");
            writer.write("Run ID:         " + runId + "\n");
            writer.write("Generated At:   " + LocalDateTime.now().format(FORMATTER) + "\n");
            writer.write("Total Records:  " + summary.total() + "\n");
            writer.write("Succeeded:      " + summary.succeeded() + "\n");
            writer.write("Failed:         " + summary.failed() + "\n");
            writer.write(String.format("Total Gross:    Rs. %,.2f\n", summary.totalGross()));
            writer.write(String.format("Total Net:      Rs. %,.2f\n", summary.totalNet()));
            writer.write("Duration:       " + summary.durationMillis() + " ms\n");
            writer.write("-----------------------------------------------------------------------------------------\n");
            writer.write(String.format("%-6s | %-24s | %12s | %12s | %12s\n",
                    "EMP ID", "NAME", "GROSS", "DEDUCTIONS", "NET"));
            writer.write("-----------------------------------------------------------------------------------------\n");

            for (PayrollResult r : results) {
                writer.write(String.format("%-6d | %-24s | %12.2f | %12.2f | %12.2f\n",
                        r.getEmployeeId(),
                        r.getEmployeeName() != null ? r.getEmployeeName() : "",
                        r.getGrossSalary(),
                        r.getDeductions(),
                        r.getNetSalary()));
            }
            writer.write("=========================================================================================\n");
        }
        return file;
    }
}

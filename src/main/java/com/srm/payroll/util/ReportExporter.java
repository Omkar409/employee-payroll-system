package com.srm.payroll.util;

import com.srm.payroll.model.BatchSummary;
import com.srm.payroll.model.Employee;
import com.srm.payroll.model.PayrollResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ReportExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReportExporter() {
        // Utility class
    }

    public static void exportResultsToCsv(File file, List<PayrollResult> results) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("ResultID,RunID,EmployeeID,EmployeeName,GrossSalary,Deductions,NetSalary,ProcessedAt");
            writer.newLine();

            for (PayrollResult r : results) {
                String processedAtStr = r.getProcessedAt() != null ? r.getProcessedAt().format(DATE_FORMATTER) : "";
                writer.write(String.format("%d,%s,%d,\"%s\",%.2f,%.2f,%.2f,%s",
                        r.getResultId(),
                        r.getRunId() != null ? r.getRunId() : "",
                        r.getEmployeeId(),
                        r.getEmployeeName() != null ? r.getEmployeeName().replace("\"", "\"\"") : "",
                        r.getGrossSalary(),
                        r.getDeductions(),
                        r.getNetSalary(),
                        processedAtStr));
                writer.newLine();
            }
        }
    }

    public static void exportResultsToTxt(File file, String runId, BatchSummary summary, List<PayrollResult> results) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("========================================================================================\n");
            writer.write("                       EMPLOYEE PAYROLL BATCH PROCESSING REPORT                         \n");
            writer.write("========================================================================================\n");
            writer.write("Run ID:         " + (runId != null ? runId : "N/A") + "\n");
            writer.write("Generated At:   " + LocalDateTime.now().format(DATE_FORMATTER) + "\n");
            if (summary != null) {
                writer.write(String.format("Total Employees: %d | Succeeded: %d | Failed: %d\n",
                        summary.total(), summary.succeeded(), summary.failed()));
                writer.write(String.format("Total Gross:     $%,.2f\n", summary.totalGross()));
                writer.write(String.format("Total Net:       $%,.2f\n", summary.totalNet()));
                writer.write(String.format("Duration:        %d ms\n", summary.durationMillis()));
            }
            writer.write("----------------------------------------------------------------------------------------\n");
            writer.write(String.format("%-8s | %-24s | %-14s | %-14s | %-14s\n",
                    "EMP ID", "EMPLOYEE NAME", "GROSS SALARY", "DEDUCTIONS", "NET SALARY"));
            writer.write("----------------------------------------------------------------------------------------\n");

            for (PayrollResult r : results) {
                writer.write(String.format("%-8d | %-24s | %14.2f | %14.2f | %14.2f\n",
                        r.getEmployeeId(),
                        r.getEmployeeName() != null ? r.getEmployeeName() : "",
                        r.getGrossSalary(),
                        r.getDeductions(),
                        r.getNetSalary()));
            }
            writer.write("========================================================================================\n");
        }
    }

    public static void exportEmployeesToCsv(File file, List<Employee> employees) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("EmployeeID,Name,Type,BasicSalary,ExtraInfo");
            writer.newLine();

            for (Employee e : employees) {
                writer.write(String.format("%d,\"%s\",%s,%.2f,\"%s\"",
                        e.getEmployeeId(),
                        e.getName().replace("\"", "\"\""),
                        e.getEmployeeType().name(),
                        e.getBasicSalary(),
                        e.toString().replace("\"", "\"\"")));
                writer.newLine();
            }
        }
    }
}


package payroll.service;

import payroll.*;
import payroll.dao.DataAccessException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing File Input/Output operations according to the project report:
 * - Persistent storage (save/load employee records using Character Streams)
 * - Method overloading for salary report generation
 * - Byte stream demonstration (FileInputStream / FileOutputStream with finally block)
 */
public class PayrollFileService {

    private static final String CSV_HEADER = "ID,NAME,TYPE,BASIC_SALARY,BONUS_OR_ALLOWANCE,CONTRACT_MONTHS";

    /**
     * Saves all employee records to a structured CSV / text file.
     * Demonstrates: Character streams (BufferedWriter, FileWriter), try-with-resources.
     */
    public void saveEmployees(List<Employee> employees, String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(CSV_HEADER);
            writer.newLine();

            for (Employee emp : employees) {
                String line;
                if (emp instanceof PermanentEmployee pe) {
                    line = String.format("%d,%s,PERMANENT,%.2f,%.2f,0",
                            pe.getEmployeeId(), pe.getName(), pe.getBasicSalary(), pe.getBonus());
                } else if (emp instanceof ContractEmployee ce) {
                    line = String.format("%d,%s,CONTRACT,%.2f,%.2f,%d",
                            ce.getEmployeeId(), ce.getName(), ce.getBasicSalary(),
                            ce.getContractAllowance(), ce.getContractMonths());
                } else {
                    line = String.format("%d,%s,%s,%.2f,0.00,0",
                            emp.getEmployeeId(), emp.getName(), emp.getEmployeeType().name(), emp.getBasicSalary());
                }
                writer.write(line);
                writer.newLine();
            }
        }
    }

    /**
     * Loads employee records from a file and returns them as an Employee list.
     * Demonstrates: Character streams (BufferedReader, FileReader), input parsing and validation.
     */
    public List<Employee> loadEmployees(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Data file not found at: " + filePath);
        }

        List<Employee> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String header = reader.readLine(); // skip header
            if (header == null) {
                return list;
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length < 4) {
                    throw new IOException("Malformed record at line " + lineNum + ": " + line);
                }

                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    String typeStr = parts[2].trim();
                    double basic = Double.parseDouble(parts[3].trim());

                    if ("CONTRACT".equalsIgnoreCase(typeStr)) {
                        double allowance = parts.length > 4 ? Double.parseDouble(parts[4].trim()) : 0.0;
                        int months = parts.length > 5 ? Integer.parseInt(parts[5].trim()) : 12;
                        list.add(new ContractEmployee(id, name, basic, allowance, months));
                    } else {
                        double bonus = parts.length > 4 ? Double.parseDouble(parts[4].trim()) : 0.0;
                        list.add(new PermanentEmployee(id, name, basic, bonus));
                    }
                } catch (NumberFormatException nfe) {
                    throw new IOException("Numeric format error at line " + lineNum + ": " + line, nfe);
                }
            }
        }
        return list;
    }

    // =========================================================================
    // METHOD OVERLOADING DEMONSTRATION (Report Formatting Helpers)
    // =========================================================================

    /**
     * Overload 1: Formats single employee payslip summary.
     */
    public String formatSummary(Employee employee, PayrollResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("EMPLOYEE PAYROLL SUMMARY\n");
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("Employee ID:      %d\n", employee.getEmployeeId()));
        sb.append(String.format("Employee Name:    %s\n", employee.getName()));
        sb.append(String.format("Employee Type:    %s\n", employee.getEmployeeType().name()));
        sb.append(String.format("Basic Salary:     Rs. %,10.2f\n", employee.getBasicSalary()));
        sb.append(String.format("Gross Salary:     Rs. %,10.2f\n", result.getGrossSalary()));
        sb.append(String.format("Deductions:       Rs. %,10.2f\n", result.getDeductions()));
        sb.append(String.format("Net Salary:       Rs. %,10.2f\n", result.getNetSalary()));
        sb.append("--------------------------------------------------\n");
        return sb.toString();
    }

    /**
     * Overload 2: Formats single employee payslip with a custom header banner.
     */
    public String formatSummary(Employee employee, PayrollResult result, String customHeader) {
        return (customHeader != null ? customHeader + "\n" : "") + formatSummary(employee, result);
    }

    /**
     * Overload 3: Formats multi-employee batch report table with overall totals.
     */
    public String formatSummary(List<Employee> employees, PayrollCalculator calculator) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================================================\n");
        sb.append("                               BATCH PAYROLL SUMMARY REPORT                              \n");
        sb.append("=========================================================================================\n");
        sb.append(String.format("%-6s | %-20s | %-10s | %12s | %12s | %12s | %12s\n",
                "ID", "NAME", "CATEGORY", "BASIC", "GROSS", "DEDUCTIONS", "NET"));
        sb.append("-----------------------------------------------------------------------------------------\n");

        double totalBasic = 0;
        double totalGross = 0;
        double totalDeductions = 0;
        double totalNet = 0;

        for (Employee emp : employees) {
            PayrollResult r = calculator.calculate(emp);
            sb.append(String.format("%-6d | %-20s | %-10s | %12.2f | %12.2f | %12.2f | %12.2f\n",
                    emp.getEmployeeId(), emp.getName(), emp.getEmployeeType().name(),
                    emp.getBasicSalary(), r.getGrossSalary(), r.getDeductions(), r.getNetSalary()));

            totalBasic += emp.getBasicSalary();
            totalGross += r.getGrossSalary();
            totalDeductions += r.getDeductions();
            totalNet += r.getNetSalary();
        }

        sb.append("=========================================================================================\n");
        sb.append(String.format("TOTAL EMPLOYEES: %d\n", employees.size()));
        sb.append(String.format("TOTAL BASIC SALARY:      Rs. %,14.2f\n", totalBasic));
        sb.append(String.format("TOTAL GROSS PAYOUT:      Rs. %,14.2f\n", totalGross));
        sb.append(String.format("TOTAL DEDUCTIONS:        Rs. %,14.2f\n", totalDeductions));
        sb.append(String.format("TOTAL NET DISBURSEMENT:  Rs. %,14.2f\n", totalNet));
        sb.append("=========================================================================================\n");
        return sb.toString();
    }

    /**
     * Generates a text report to disk using formatSummary.
     */
    public void generateReport(List<Employee> employees, PayrollCalculator calculator, String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        String reportText = formatSummary(employees, calculator);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(reportText);
        }
    }

    // =========================================================================
    // BYTE STREAM & FINALLY BLOCK DEMONSTRATION
    // (FileInputStream, FileOutputStream, and finally keyword from Syllabus)
    // =========================================================================

    /**
     * Copies a file byte-for-byte using raw byte streams.
     * Demonstrates FileInputStream, FileOutputStream, and explicit cleanup in a finally block.
     */
    public void copyFile(String sourcePath, String destPath) throws IOException {
        FileInputStream inStream = null;
        FileOutputStream outStream = null;

        try {
            File src = new File(sourcePath);
            if (!src.exists()) {
                throw new FileNotFoundException("Source file not found: " + sourcePath);
            }

            File dest = new File(destPath);
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            inStream = new FileInputStream(src);
            outStream = new FileOutputStream(dest);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            outStream.flush();
        } finally {
            // Demonstrates explicit cleanup in finally block
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ignored) {}
            }
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException ignored) {}
            }
        }
    }
}

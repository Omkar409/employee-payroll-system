package payroll;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service handling persistent storage, reporting, and file I/O operations.
 * Demonstrates:
 * - File I/O: character streams (BufferedReader, BufferedWriter, FileReader, FileWriter)
 * - Byte streams: FileInputStream and FileOutputStream for file copy/backup
 * - Method overloading: multiple overloaded report formatting methods
 * - Exception handling: try, catch, throw, throws, and finally blocks
 */
public class PayrollFileService {

    private static final String HEADER = "ID,NAME,TYPE,BASIC_SALARY,EXTRA";

    /**
     * Saves a list of employees to a text file.
     * Demonstrates Character Stream writing, throws, and try-with-resources.
     */
    public void saveEmployees(List<Employee> employees, String filePath) throws FileStorageException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(HEADER);
            writer.newLine();

            for (Employee emp : employees) {
                String line;
                if (emp instanceof ContractEmployee) {
                    ContractEmployee ce = (ContractEmployee) emp;
                    line = String.format("%d,%s,%s,%.2f,%.2f",
                            emp.getId(), emp.getName(), emp.getEmployeeType(), emp.getBasicSalary(), ce.getContractAllowance());
                } else if (emp instanceof ExecutiveEmployee) {
                    ExecutiveEmployee ee = (ExecutiveEmployee) emp;
                    line = String.format("%d,%s,%s,%.2f,%s:%.2f",
                            emp.getId(), emp.getName(), "Executive", emp.getBasicSalary(), ee.getDepartment(), ee.getExecutiveBonus());
                } else {
                    line = String.format("%d,%s,%s,%.2f,0.00",
                            emp.getId(), emp.getName(), emp.getEmployeeType(), emp.getBasicSalary());
                }
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new FileStorageException("Failed to save employee records to " + filePath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Loads a list of employees from a text file.
     * Demonstrates Character Stream reading and error recovery.
     */
    public List<Employee> loadEmployees(String filePath) throws FileStorageException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileStorageException("File does not exist: " + filePath);
        }

        List<Employee> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // Header line
            if (line == null) {
                return list;
            }

            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 4) {
                    throw new FileStorageException("Malformed record at line " + lineNum + ": " + line);
                }

                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    String type = parts[2].trim();
                    double basic = Double.parseDouble(parts[3].trim());

                    if ("Contract".equalsIgnoreCase(type)) {
                        double allowance = parts.length > 4 ? Double.parseDouble(parts[4].trim()) : 0.0;
                        list.add(new ContractEmployee(id, name, basic, allowance));
                    } else if (type.toLowerCase().startsWith("executive")) {
                        String dept = "General";
                        double bonus = 0.0;
                        if (parts.length > 4 && parts[4].contains(":")) {
                            String[] sub = parts[4].split(":");
                            dept = sub[0];
                            bonus = Double.parseDouble(sub[1]);
                        }
                        list.add(new ExecutiveEmployee(id, name, basic, dept, bonus));
                    } else {
                        list.add(new PermanentEmployee(id, name, basic));
                    }
                } catch (NumberFormatException nfe) {
                    throw new FileStorageException("Invalid numeric data in line " + lineNum + ": " + line, nfe);
                } catch (IllegalArgumentException iae) {
                    throw new FileStorageException("Validation failure in line " + lineNum + ": " + iae.getMessage(), iae);
                }
            }
        } catch (IOException e) {
            throw new FileStorageException("Error reading employee records from " + filePath + ": " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // METHOD OVERLOADING DEMONSTRATION (Report Formatting Helpers)
    // =========================================================================

    /**
     * Overload 1: Formats a single employee payroll summary (Academic standard layout).
     */
    public String formatSummary(Employee employee, PayrollResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("EMPLOYEE PAYROLL SUMMARY\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("Employee ID:      %d\n", employee.getId()));
        sb.append(String.format("Employee Name:    %s\n", employee.getName()));
        sb.append(String.format("Employee Type:    %s\n", employee.getEmployeeType()));
        sb.append(String.format("Basic Salary:     %.2f\n", result.getBasic()));
        sb.append(String.format("HRA:              %.2f\n", result.getHra()));
        sb.append(String.format("Other Allowance:  %.2f\n", result.getAllowance()));
        sb.append(String.format("Gross Salary:     %.2f\n", result.getGross()));
        sb.append(String.format("Provident Fund:   %.2f\n", result.getPf()));
        sb.append(String.format("Professional Tax: %.2f\n", result.getTax()));
        sb.append(String.format("Net Salary:       %.2f\n", result.getNet()));
        sb.append("----------------------------------------\n");
        return sb.toString();
    }

    /**
     * Overload 2: Formats a single employee payroll summary with optional header banner.
     */
    public String formatSummary(Employee employee, PayrollResult result, String customHeader) {
        return (customHeader != null ? customHeader + "\n" : "") + formatSummary(employee, result);
    }

    /**
     * Overload 3: Formats a complete batch summary for multiple employees in tabular layout.
     */
    public String formatSummary(List<Employee> employees, PayrollService service) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================================================\n");
        sb.append("                               BATCH PAYROLL SUMMARY REPORT                              \n");
        sb.append("=========================================================================================\n");
        sb.append(String.format("%-6s | %-16s | %-12s | %10s | %10s | %10s | %10s\n",
                "ID", "NAME", "TYPE", "BASIC", "GROSS", "PF+TAX", "NET"));
        sb.append("-----------------------------------------------------------------------------------------\n");

        double totalBasic = 0;
        double totalGross = 0;
        double totalDeductions = 0;
        double totalNet = 0;

        for (Employee emp : employees) {
            PayrollResult r = service.process(emp);
            double deductions = r.getPf() + r.getTax();
            sb.append(String.format("%-6d | %-16s | %-12s | %10.2f | %10.2f | %10.2f | %10.2f\n",
                    emp.getId(), emp.getName(), emp.getEmployeeType(), r.getBasic(), r.getGross(), deductions, r.getNet()));
            totalBasic += r.getBasic();
            totalGross += r.getGross();
            totalDeductions += deductions;
            totalNet += r.getNet();
        }

        sb.append("=========================================================================================\n");
        sb.append(String.format("TOTAL EMPLOYEES: %d\n", employees.size()));
        sb.append(String.format("TOTAL GROSS PAYOUT:      Rs. %12.2f\n", totalGross));
        sb.append(String.format("TOTAL DEDUCTIONS:        Rs. %12.2f\n", totalDeductions));
        sb.append(String.format("TOTAL NET DISBURSEMENT:  Rs. %12.2f\n", totalNet));
        sb.append("=========================================================================================\n");
        return sb.toString();
    }

    /**
     * Exports a batch payroll summary report to a destination text file.
     */
    public void generateReport(List<Employee> employees, PayrollService service, String filePath) throws FileStorageException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        String reportText = formatSummary(employees, service);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(reportText);
        } catch (IOException e) {
            throw new FileStorageException("Failed to write report to " + filePath + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // BYTE STREAM & EXPLICIT FINALLY DEMONSTRATION
    // (Fulfills Syllabus: FileInputStream, FileOutputStream, and finally keyword)
    // =========================================================================

    /**
     * Copies a file byte-for-byte using raw byte streams.
     * Demonstrates FileInputStream, FileOutputStream, and explicit cleanup in a finally block.
     */
    public void copyFile(String sourcePath, String destPath) throws FileStorageException {
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
        } catch (IOException e) {
            throw new FileStorageException("Byte stream copy failed: " + e.getMessage(), e);
        } finally {
            // Demonstrates the 'finally' clause: executes regardless of whether an exception was thrown
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

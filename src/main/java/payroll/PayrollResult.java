package payroll;

import java.time.LocalDateTime;

/**
 * Payroll calculation result for one employee in a batch run.
 */
public class PayrollResult {
    private long resultId;
    private String runId;
    private int employeeId;
    private String employeeName;
    private double grossSalary;
    private double deductions;
    private double netSalary;
    private LocalDateTime processedAt;

    public PayrollResult(int employeeId, String employeeName,
                         double grossSalary, double deductions, double netSalary) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.grossSalary = grossSalary;
        this.deductions = deductions;
        this.netSalary = netSalary;
        this.processedAt = LocalDateTime.now();
    }

    public PayrollResult(long resultId, String runId, int employeeId, String employeeName,
                         double grossSalary, double deductions, double netSalary, LocalDateTime processedAt) {
        this.resultId = resultId;
        this.runId = runId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.grossSalary = grossSalary;
        this.deductions = deductions;
        this.netSalary = netSalary;
        this.processedAt = processedAt;
    }

    public long getResultId() { return resultId; }
    public void setResultId(long resultId) { this.resultId = resultId; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public double getGrossSalary() { return grossSalary; }
    public void setGrossSalary(double grossSalary) { this.grossSalary = grossSalary; }

    public double getDeductions() { return deductions; }
    public void setDeductions(double deductions) { this.deductions = deductions; }

    public double getNetSalary() { return netSalary; }
    public void setNetSalary(double netSalary) { this.netSalary = netSalary; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    @Override
    public String toString() {
        return String.format("Emp ID: %-4d | Name: %-18s | Gross: %10.2f | Deductions: %10.2f | Net: %10.2f",
                employeeId, employeeName != null ? employeeName : "", grossSalary, deductions, netSalary);
    }
}

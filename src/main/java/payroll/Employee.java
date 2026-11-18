package payroll;

import java.time.LocalDateTime;

/**
 * Abstract base class for all employee types.
 * Demonstrates: abstract class, encapsulation, inheritance contract,
 * polymorphic calculateGrossSalary/calculateDeductions/calculateNetSalary.
 */
public abstract class Employee implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private int employeeId;
    private String name;
    private double basicSalary;
    private final EmployeeType employeeType;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Employee(int employeeId, String name, double basicSalary, EmployeeType employeeType) {
        this.employeeId = employeeId;
        this.name = name;
        this.basicSalary = basicSalary;
        this.employeeType = employeeType;
    }

    /** Polymorphic gross salary - each subclass defines its own formula. */
    public abstract double calculateGrossSalary();

    /** Polymorphic deductions - each subclass defines its own formula. */
    public abstract double calculateDeductions();

    public double calculateNetSalary() {
        return calculateGrossSalary() - calculateDeductions();
    }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBasicSalary() { return basicSalary; }
    public void setBasicSalary(double basicSalary) { this.basicSalary = basicSalary; }

    public EmployeeType getEmployeeType() { return employeeType; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("[%d] %-20s | %-9s | Basic: %.2f",
                employeeId, name, employeeType.name(), basicSalary);
    }
}

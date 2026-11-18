package payroll;

/**
 * Permanent employee: earns basic salary + bonus.
 * Deductions: 12% PF + 8% income tax on basic salary (20% total).
 */
public class PermanentEmployee extends Employee {
    private double bonus;

    public PermanentEmployee(int employeeId, String name, double basicSalary, double bonus) {
        super(employeeId, name, basicSalary, EmployeeType.PERMANENT);
        this.bonus = bonus;
    }

    public PermanentEmployee(String name, double basicSalary, double bonus) {
        this(0, name, basicSalary, bonus);
    }

    @Override
    public double calculateGrossSalary() {
        return getBasicSalary() + bonus;
    }

    @Override
    public double calculateDeductions() {
        return getBasicSalary() * 0.20; // 12% PF + 8% tax
    }

    public double getBonus() { return bonus; }
    public void setBonus(double bonus) { this.bonus = bonus; }

    @Override
    public String toString() {
        return String.format("[%d] %-20s | PERMANENT | Basic: %.2f | Bonus: %.2f",
                getEmployeeId(), getName(), getBasicSalary(), bonus);
    }
}

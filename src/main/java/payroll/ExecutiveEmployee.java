package payroll;

/**
 * Concrete class completing the 3-level inheritance hierarchy:
 * Employee -> OrganizationEmployee -> ExecutiveEmployee.
 */
public class ExecutiveEmployee extends OrganizationEmployee {
    private final double executiveBonus;

    public ExecutiveEmployee(int id, String name, double salary, String department, double executiveBonus) {
        super(id, name, salary, department);
        if (!Double.isFinite(executiveBonus) || executiveBonus < 0) {
            throw new IllegalArgumentException("Invalid executive bonus.");
        }
        this.executiveBonus = executiveBonus;
    }

    public double getExecutiveBonus() {
        return executiveBonus;
    }

    @Override
    public String getEmployeeType() {
        return "Executive (" + getDepartment() + ")";
    }
}

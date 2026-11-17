package payroll;

/**
 * Demonstrates multilevel inheritance hierarchy in accordance with Section 4 (Page 11).
 * Intermediate abstract subclass extending Employee.
 */
public abstract class OrganizationEmployee extends Employee {
    private final String department;

    public OrganizationEmployee(int id, String name, double salary, String department) {
        super(id, name, salary);
        if (department == null || department.isBlank()) {
            throw new IllegalArgumentException("Department is required.");
        }
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }
}

package payroll;

/**
 * Represents a permanent employee category.
 * Demonstrates:
 * - Inheritance (extends Employee)
 * - super keyword to call parent constructor
 * - Method overriding (@Override getEmployeeType)
 */
public class PermanentEmployee extends Employee {

    public PermanentEmployee(int id, String name, double basicSalary) {
        super(id, name, basicSalary);
    }

    @Override
    public String getEmployeeType() {
        return "Permanent";
    }
}

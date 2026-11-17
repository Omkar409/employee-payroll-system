package payroll;

import java.io.Serializable;

/**
 * Abstract base class representing common employee attributes and behaviors.
 * Demonstrates:
 * - Classes and objects
 * - Abstract class
 * - Constructors and this reference
 * - final keyword for immutable fields
 * - Access specifiers (private fields, public methods)
 * - Encapsulation and validation
 */
public abstract class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final double basicSalary;

    public Employee(int id, String name, double basicSalary) {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee ID must be a positive integer.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name cannot be empty.");
        }
        if (!Double.isFinite(basicSalary) || basicSalary < 0) {
            throw new IllegalArgumentException("Basic salary must be a non-negative number.");
        }
        this.id = id;
        this.name = name.trim();
        this.basicSalary = basicSalary;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    /**
     * Abstract method to be implemented by subclasses to identify their category.
     * Demonstrates runtime polymorphism when invoked via Employee reference.
     */
    public abstract String getEmployeeType();

    @Override
    public String toString() {
        return String.format("Employee[id=%d, name='%s', type=%s, basic=%.2f]",
                id, name, getEmployeeType(), basicSalary);
    }
}

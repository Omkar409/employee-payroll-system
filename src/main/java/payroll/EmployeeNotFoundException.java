package payroll;

/**
 * Thrown when an employee record is not found in the repository.
 */
public class EmployeeNotFoundException extends PayrollException {
    private static final long serialVersionUID = 1L;

    public EmployeeNotFoundException(String message) {
        super(message);
    }
}

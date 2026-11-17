package payroll;

/**
 * Thrown when attempting to add an employee with an existing ID.
 */
public class DuplicateEmployeeException extends PayrollException {
    private static final long serialVersionUID = 1L;

    public DuplicateEmployeeException(String message) {
        super(message);
    }
}

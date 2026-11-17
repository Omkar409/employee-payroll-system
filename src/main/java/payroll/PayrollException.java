package payroll;

/**
 * Base checked exception class for payroll domain errors.
 * Demonstrates:
 * - Exception hierarchy
 * - Custom user-defined exception
 */
public class PayrollException extends Exception {
    private static final long serialVersionUID = 1L;

    public PayrollException(String message) {
        super(message);
    }

    public PayrollException(String message, Throwable cause) {
        super(message, cause);
    }
}

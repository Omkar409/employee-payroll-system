package payroll;

/**
 * Thrown when employee input data fails validation rules.
 */
public class ValidationException extends PayrollException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}

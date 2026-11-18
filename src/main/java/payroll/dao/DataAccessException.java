package payroll.dao;

/**
 * Custom checked exception wrapping SQL and database access failures.
 */
public class DataAccessException extends Exception {
    private final String userMessage;

    public DataAccessException(String userMessage) {
        super(userMessage);
        this.userMessage = userMessage;
    }

    public DataAccessException(String userMessage, Throwable cause) {
        super(userMessage + (cause != null && cause.getMessage() != null
                ? " (" + cause.getMessage() + ")" : ""), cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() { return userMessage; }
}

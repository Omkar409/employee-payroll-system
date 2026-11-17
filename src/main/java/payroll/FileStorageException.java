package payroll;

/**
 * Thrown when an error occurs during file input/output persistence operations.
 */
public class FileStorageException extends PayrollException {
    private static final long serialVersionUID = 1L;

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

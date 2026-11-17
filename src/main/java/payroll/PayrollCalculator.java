package payroll;

/**
 * Interface defining the contract for calculating employee payroll.
 * Demonstrates:
 * - Interface definition
 * - Polymorphism contract
 */
public interface PayrollCalculator {
    PayrollResult calculate(Employee employee);
}

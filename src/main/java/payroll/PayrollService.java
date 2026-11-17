package payroll;

/**
 * Service class orchestrating payroll operations.
 * Demonstrates:
 * - Dependency injection via constructor
 * - Runtime polymorphism: delegates calculations to PayrollCalculator abstraction
 */
public class PayrollService {
    private final PayrollCalculator calculator;

    public PayrollService(PayrollCalculator calculator) {
        if (calculator == null) {
            throw new IllegalArgumentException("PayrollCalculator cannot be null.");
        }
        this.calculator = calculator;
    }

    public PayrollResult process(Employee employee) {
        return calculator.calculate(employee);
    }
}

package payroll;

/**
 * Standard implementation of the PayrollCalculator interface.
 * Implements salary formulas specified in the project specification:
 * - HRA = 20% of basic salary
 * - Other allowance = 10% of basic salary (+ contract allowance if ContractEmployee, or bonus if ExecutiveEmployee)
 * - Provident Fund (PF) = 12% of basic salary
 * - Professional Tax = ₹200.00 per payroll period
 * - Gross = Basic + HRA + Allowance
 * - Net = Gross - PF - Tax
 *
 * Demonstrates:
 * - Interface implementation (implements PayrollCalculator)
 * - static final configuration constants
 * - instanceof type checking and casting
 * - Method overriding (@Override calculate)
 */
public class StandardPayrollCalculator implements PayrollCalculator {
    private static final double HRA_RATE = 0.20;
    private static final double ALLOWANCE_RATE = 0.10;
    private static final double PF_RATE = 0.12;
    private static final double TAX = 200.00;

    @Override
    public PayrollResult calculate(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null.");
        }

        double basic = employee.getBasicSalary();
        double hra = basic * HRA_RATE;
        double allowance = basic * ALLOWANCE_RATE;
        double pf = basic * PF_RATE;

        if (employee instanceof ContractEmployee) {
            ContractEmployee contract = (ContractEmployee) employee;
            allowance += contract.getContractAllowance();
        } else if (employee instanceof ExecutiveEmployee) {
            ExecutiveEmployee exec = (ExecutiveEmployee) employee;
            allowance += exec.getExecutiveBonus();
        }

        return new PayrollResult(basic, hra, allowance, pf, TAX);
    }
}

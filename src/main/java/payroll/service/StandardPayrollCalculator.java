package payroll.service;

import payroll.Employee;
import payroll.PayrollResult;

/**
 * Calculates payroll using the employee's own polymorphic methods.
 */
public class StandardPayrollCalculator implements PayrollCalculator {

    @Override
    public PayrollResult calculate(Employee employee) {
        if (employee == null)
            throw new IllegalArgumentException("Employee cannot be null.");
        double gross = employee.calculateGrossSalary();
        double deductions = employee.calculateDeductions();
        double net = employee.calculateNetSalary();
        return new PayrollResult(employee.getEmployeeId(), employee.getName(), gross, deductions, net);
    }
}

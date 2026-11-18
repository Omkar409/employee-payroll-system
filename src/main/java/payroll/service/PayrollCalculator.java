package payroll.service;

import payroll.Employee;
import payroll.PayrollResult;

/**
 * Strategy interface for calculating employee payroll.
 */
public interface PayrollCalculator {
    PayrollResult calculate(Employee employee);
}

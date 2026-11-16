package com.srm.payroll.service;

import com.srm.payroll.model.Employee;
import com.srm.payroll.model.PayrollResult;

public class StandardPayrollCalculator implements PayrollCalculator {

    @Override
    public PayrollResult calculate(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null for payroll calculation.");
        }
        double gross = employee.calculateGrossSalary();
        double deductions = employee.calculateDeductions();
        double net = employee.calculateNetSalary();
        return new PayrollResult(employee.getEmployeeId(), employee.getName(), gross, deductions, net);
    }
}


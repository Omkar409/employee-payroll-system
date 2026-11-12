package com.srm.payroll.service;

import com.srm.payroll.model.Employee;
import com.srm.payroll.model.PayrollResult;

public interface PayrollCalculator {
    PayrollResult calculate(Employee employee);
}

package com.srm.payroll.model;

public class ContractEmployee extends Employee {
    private double contractAllowance;
    private int contractMonths;

    public ContractEmployee(int employeeId, String name, double basicSalary,
                            double contractAllowance, int contractMonths) {
        super(employeeId, name, basicSalary, EmployeeType.CONTRACT);
        this.contractAllowance = contractAllowance;
        this.contractMonths = contractMonths;
    }

    public ContractEmployee(String name, double basicSalary,
                            double contractAllowance, int contractMonths) {
        this(0, name, basicSalary, contractAllowance, contractMonths);
    }

    @Override
    public double calculateGrossSalary() {
        return getBasicSalary() + contractAllowance;
    }

    @Override
    public double calculateDeductions() {
        // Standard 10% flat TDS deduction on gross salary
        return calculateGrossSalary() * 0.10;
    }

    public double getContractAllowance() { return contractAllowance; }
    public void setContractAllowance(double contractAllowance) { this.contractAllowance = contractAllowance; }

    public int getContractMonths() { return contractMonths; }
    public void setContractMonths(int contractMonths) { this.contractMonths = contractMonths; }

    @Override
    public String toString() {
        return String.format("[%d] %-20s | CONTRACT  | Basic: %.2f | Allowance: %.2f | Months: %d",
                getEmployeeId(), getName(), getBasicSalary(), contractAllowance, contractMonths);
    }
}


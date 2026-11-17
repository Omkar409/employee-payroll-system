package payroll;

/**
 * Represents a contract employee category with a specific contract allowance.
 * Demonstrates:
 * - Inheritance (extends Employee)
 * - Additional specialized state (contractAllowance)
 * - Validation in constructor
 * - super() constructor invocation
 * - Method overriding
 */
public class ContractEmployee extends Employee {
    private final double contractAllowance;

    public ContractEmployee(int id, String name, double basicSalary, double contractAllowance) {
        super(id, name, basicSalary);
        if (!Double.isFinite(contractAllowance) || contractAllowance < 0) {
            throw new IllegalArgumentException("Invalid contract allowance.");
        }
        this.contractAllowance = contractAllowance;
    }

    public double getContractAllowance() {
        return contractAllowance;
    }

    @Override
    public String getEmployeeType() {
        return "Contract";
    }
}

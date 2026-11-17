package payroll;

/**
 * Immutable value object holding calculated payroll components for an employee.
 * Demonstrates:
 * - final class and final fields (immutability)
 * - Encapsulation with getter methods
 * - Precise mathematical calculation of gross, deductions, and net salary
 */
public final class PayrollResult {
    private final double basic;
    private final double hra;
    private final double allowance;
    private final double gross;
    private final double pf;
    private final double tax;
    private final double net;

    public PayrollResult(double basic, double hra, double allowance, double pf, double tax) {
        this.basic = basic;
        this.hra = hra;
        this.allowance = allowance;
        this.gross = basic + hra + allowance;
        this.pf = pf;
        this.tax = tax;
        this.net = gross - pf - tax;
    }

    public double getBasic() {
        return basic;
    }

    public double getHra() {
        return hra;
    }

    public double getAllowance() {
        return allowance;
    }

    public double getGross() {
        return gross;
    }

    public double getPf() {
        return pf;
    }

    public double getTax() {
        return tax;
    }

    public double getNet() {
        return net;
    }

    @Override
    public String toString() {
        return String.format("PayrollResult[Basic=%.2f, HRA=%.2f, Allowance=%.2f, Gross=%.2f, PF=%.2f, Tax=%.2f, Net=%.2f]",
                basic, hra, allowance, gross, pf, tax, net);
    }
}

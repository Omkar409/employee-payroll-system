package payroll.service;

import java.time.LocalDateTime;

/**
 * Immutable record representing a single salary disbursement transaction.
 */
public record DisbursementRecord(
        int employeeId,
        String employeeName,
        String bankAccount,
        double grossAmount,
        double deductions,
        double netAmount,
        String utr,
        String status,
        LocalDateTime timestamp
) {
    @Override
    public String toString() {
        return String.format("[%s] Emp #%d (%s) -> A/C %s | Net: Rs. %,.2f | UTR: %s",
                status, employeeId, employeeName, bankAccount, netAmount, utr);
    }
}

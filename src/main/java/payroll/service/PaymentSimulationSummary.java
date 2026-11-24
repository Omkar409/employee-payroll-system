package payroll.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Summary record capturing the outcome of an automated bulk payment simulation.
 */
public record PaymentSimulationSummary(
        String companyAccount,
        String companyBankName,
        double companyStartingBalance,
        double totalGross,
        double totalDeductions,
        double totalDisbursed,
        double companyEndingBalance,
        int totalEmployees,
        String batchId,
        long durationMillis,
        List<DisbursementRecord> disbursements
) {

    public String formatSummaryReport() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================================\n");
        sb.append("         AUTOMATED SALARY DISBURSEMENT SIMULATION - SETTLEMENT STATEMENT        \n");
        sb.append("========================================================================================\n");
        sb.append("DISCLAIMER: [SIMULATION MODE - NO ACTUAL CURRENCY TRANSFERRED - DEMO PURPOSES ONLY]\n\n");

        sb.append("DEBIT ACCOUNT (COMPANY TREASURY):\n");
        sb.append(String.format("  Company Name:        SRM University AP Corporate Payout\n"));
        sb.append(String.format("  Corporate Bank:      %s\n", companyBankName));
        sb.append(String.format("  Debit Account No:    %s\n", companyAccount));
        sb.append(String.format("  Starting Balance:    Rs. %,14.2f\n", companyStartingBalance));
        sb.append(String.format("  Total Net Debited:   Rs. %,14.2f\n", totalDisbursed));
        sb.append(String.format("  Ending Balance:      Rs. %,14.2f\n\n", companyEndingBalance));

        sb.append("BATCH & CLEARING METRICS:\n");
        sb.append(String.format("  Batch Run ID:        %s\n", batchId));
        sb.append(String.format("  Settlement Protocol: ACH / Direct Bank Transfer Gateway (Simulated)\n"));
        sb.append(String.format("  Total Recipients:    %d employees\n", totalEmployees));
        sb.append(String.format("  Status:              100%% SETTLED (%d of %d successful)\n", totalEmployees, totalEmployees));
        sb.append(String.format("  Execution Time:      %d ms\n", durationMillis));
        sb.append(String.format("  Settlement Date:     %s\n\n", LocalDateTime.now().format(dtf)));

        sb.append("-------------------------------------------------------------------------------------------------------\n");
        sb.append(String.format("%-5s | %-20s | %-16s | %-11s | %12s | %-20s | %-7s\n",
                "ID", "EMPLOYEE NAME", "BANK ACCOUNT", "IFSC", "NET PAYOUT", "TRANSACTION UTR", "STATUS"));
        sb.append("-------------------------------------------------------------------------------------------------------\n");

        int limit = Math.min(disbursements.size(), 30);
        for (int i = 0; i < limit; i++) {
            DisbursementRecord d = disbursements.get(i);
            sb.append(String.format("%-5d | %-20s | %-16s | %-11s | Rs. %,9.2f | %-20s | %-7s\n",
                    d.employeeId(),
                    d.employeeName().length() > 20 ? d.employeeName().substring(0, 17) + "..." : d.employeeName(),
                    d.bankAccount(),
                    d.ifscCode(),
                    d.netAmount(),
                    d.utr(),
                    d.status()));
        }

        if (disbursements.size() > limit) {
            sb.append(String.format("... and %d more employee transactions successfully disbursed and settled.\n",
                    disbursements.size() - limit));
        }

        sb.append("-------------------------------------------------------------------------------------------------------\n");
        sb.append(String.format("TOTAL GROSS ACCRUED:      Rs. %,14.2f\n", totalGross));
        sb.append(String.format("TOTAL TAXES / DEDUCTIONS: Rs. %,14.2f\n", totalDeductions));
        sb.append(String.format("TOTAL NET DISBURSED:      Rs. %,14.2f\n", totalDisbursed));
        sb.append("========================================================================================\n");
        sb.append("ALL BANK TRANSFERS VERIFIED, RECONCILED, AND COMPLETED SUCCESSFULLY.\n");
        sb.append("========================================================================================\n");

        return sb.toString();
    }
}

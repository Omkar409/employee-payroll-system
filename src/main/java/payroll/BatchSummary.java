package payroll;

public record BatchSummary(
        int total,
        int succeeded,
        int failed,
        double totalGross,
        double totalNet,
        long durationMillis
) {}

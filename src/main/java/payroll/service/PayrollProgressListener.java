package payroll.service;

import payroll.BatchSummary;
import payroll.PayrollResult;

public interface PayrollProgressListener {
    void onProgress(int processed, int total, PayrollResult latest);
    void onComplete(BatchSummary summary);
    void onCancelled(int processed, int total);
    void onError(Exception cause);
}

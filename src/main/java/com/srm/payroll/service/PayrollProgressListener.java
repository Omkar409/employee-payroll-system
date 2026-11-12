package com.srm.payroll.service;

import com.srm.payroll.model.BatchSummary;
import com.srm.payroll.model.PayrollResult;

public interface PayrollProgressListener {
    void onProgress(int processed, int total, PayrollResult latest);
    void onComplete(BatchSummary summary);
    void onCancelled(int processed, int total);
    void onError(Exception cause);
}

package com.srm.payroll.service;

import com.srm.payroll.dao.EmployeeDAO;
import com.srm.payroll.dao.PayrollResultDAO;
import com.srm.payroll.model.BatchSummary;
import com.srm.payroll.model.Employee;
import com.srm.payroll.model.PayrollResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker task that processes payroll for all employees in a background thread.
 * Never throws exceptions directly; reports progress, cancellation, completion,
 * or errors through the provided PayrollProgressListener.
 */
public class PayrollTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(PayrollTask.class.getName());
    private static final int CHUNK_SIZE = 250;

    private final EmployeeDAO employeeDAO;
    private final PayrollResultDAO resultDAO;
    private final PayrollCalculator calculator;
    private final PayrollProgressListener listener;
    private final String runId;
    private volatile boolean cancelled;

    public PayrollTask(EmployeeDAO employeeDAO, PayrollResultDAO resultDAO,
                       PayrollCalculator calculator, PayrollProgressListener listener) {
        this.employeeDAO = employeeDAO;
        this.resultDAO = resultDAO;
        this.calculator = calculator;
        this.listener = listener;
        this.runId = UUID.randomUUID().toString();
        this.cancelled = false;
    }

    public String getRunId() {
        return runId;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        int totalEmployees = 0;
        int processed = 0;
        int succeeded = 0;
        int failed = 0;
        double totalGross = 0.0;
        double totalNet = 0.0;
        List<PayrollResult> allResults = new ArrayList<>();

        try {
            totalEmployees = employeeDAO.count();

            if (totalEmployees == 0) {
                // T26: Batch with zero employees completes immediately with empty summary
                BatchSummary emptySummary = new BatchSummary(0, 0, 0, 0.0, 0.0, System.currentTimeMillis() - startTime);
                listener.onComplete(emptySummary);
                return;
            }

            int offset = 0;
            while (offset < totalEmployees) {
                if (cancelled) {
                    listener.onCancelled(processed, totalEmployees);
                    return;
                }

                List<Employee> chunk = employeeDAO.findPage(offset, CHUNK_SIZE);
                if (chunk.isEmpty()) {
                    break;
                }

                for (Employee emp : chunk) {
                    if (cancelled) {
                        listener.onCancelled(processed, totalEmployees);
                        return;
                    }

                    try {
                        PayrollResult result = calculator.calculate(emp);
                        result.setRunId(runId);
                        allResults.add(result);
                        succeeded++;
                        totalGross += result.getGrossSalary();
                        totalNet += result.getNetSalary();
                        processed++;
                        listener.onProgress(processed, totalEmployees, result);
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Failed to calculate payroll for employee ID " + emp.getEmployeeId(), ex);
                        failed++;
                        processed++;
                    }
                }
                offset += chunk.size();
            }

            if (cancelled) {
                listener.onCancelled(processed, totalEmployees);
                return;
            }

            // Save batch to database in one atomic transaction (T03)
            if (!allResults.isEmpty()) {
                resultDAO.saveBatch(runId, allResults);
            }

            long duration = System.currentTimeMillis() - startTime;
            BatchSummary summary = new BatchSummary(totalEmployees, succeeded, failed, totalGross, totalNet, duration);
            listener.onComplete(summary);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during payroll batch execution", e);
            listener.onError(e);
        }
    }
}

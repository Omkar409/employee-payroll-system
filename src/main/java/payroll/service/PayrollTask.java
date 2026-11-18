package payroll.service;

import payroll.*;
import payroll.dao.DataAccessException;
import payroll.dao.EmployeeDAO;
import payroll.dao.PayrollResultDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background Runnable worker that processes payroll for all employees.
 * Reports progress via PayrollProgressListener.
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

    public String getRunId() { return runId; }
    public void cancel() { this.cancelled = true; }
    public boolean isCancelled() { return cancelled; }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        int totalEmployees = 0, processed = 0, succeeded = 0, failed = 0;
        double totalGross = 0.0, totalNet = 0.0;
        List<PayrollResult> allResults = new ArrayList<>();

        try {
            totalEmployees = employeeDAO.count();

            if (totalEmployees == 0) {
                listener.onComplete(new BatchSummary(0, 0, 0, 0.0, 0.0, System.currentTimeMillis() - startTime));
                return;
            }

            int offset = 0;
            while (offset < totalEmployees) {
                if (cancelled) { listener.onCancelled(processed, totalEmployees); return; }

                List<Employee> chunk = employeeDAO.findPage(offset, CHUNK_SIZE);
                if (chunk.isEmpty()) break;

                for (Employee emp : chunk) {
                    if (cancelled) { listener.onCancelled(processed, totalEmployees); return; }
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
                        LOGGER.log(Level.WARNING, "Failed for employee ID " + emp.getEmployeeId(), ex);
                        failed++;
                        processed++;
                    }
                }
                offset += chunk.size();
            }

            if (cancelled) { listener.onCancelled(processed, totalEmployees); return; }

            if (!allResults.isEmpty()) resultDAO.saveBatch(runId, allResults);

            long duration = System.currentTimeMillis() - startTime;
            listener.onComplete(new BatchSummary(totalEmployees, succeeded, failed, totalGross, totalNet, duration));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during payroll batch", e);
            listener.onError(e);
        }
    }
}

package payroll.service;

import payroll.dao.EmployeeDAO;
import payroll.dao.PayrollResultDAO;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages background batch payroll jobs.
 * Enforces single execution at a time using AtomicBoolean.
 */
public class PayrollBatchService {

    private static final Logger LOGGER = Logger.getLogger(PayrollBatchService.class.getName());

    private final EmployeeDAO employeeDAO;
    private final PayrollResultDAO resultDAO;
    private final PayrollCalculator calculator;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private PayrollTask currentTask;
    private Thread batchThread;
    private volatile String lastRunId;

    public PayrollBatchService(EmployeeDAO employeeDAO, PayrollResultDAO resultDAO, PayrollCalculator calculator) {
        this.employeeDAO = employeeDAO;
        this.resultDAO = resultDAO;
        this.calculator = calculator;
    }

    public synchronized void start(PayrollProgressListener listener) {
        if (!running.compareAndSet(false, true))
            throw new IllegalStateException("A payroll batch run is already in progress.");

        PayrollProgressListener wrapped = new PayrollProgressListener() {
            @Override
            public void onProgress(int processed, int total, payroll.PayrollResult latest) {
                listener.onProgress(processed, total, latest);
            }
            @Override
            public void onComplete(payroll.BatchSummary summary) {
                running.set(false);
                listener.onComplete(summary);
            }
            @Override
            public void onCancelled(int processed, int total) {
                running.set(false);
                listener.onCancelled(processed, total);
            }
            @Override
            public void onError(Exception cause) {
                running.set(false);
                listener.onError(cause);
            }
        };

        this.currentTask = new PayrollTask(employeeDAO, resultDAO, calculator, wrapped);
        this.lastRunId = this.currentTask.getRunId();
        this.batchThread = new Thread(this.currentTask, "payroll-batch");
        this.batchThread.setDaemon(true);
        this.batchThread.start();
    }

    public synchronized void cancel() {
        if (running.get() && currentTask != null) currentTask.cancel();
    }

    public boolean isRunning() { return running.get(); }
    public String getLastRunId() { return lastRunId; }

    public void shutdown() {
        cancel();
        if (batchThread != null && batchThread.isAlive()) {
            try { batchThread.join(2000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

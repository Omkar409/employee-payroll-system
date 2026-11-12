package com.srm.payroll.service;

import com.srm.payroll.dao.EmployeeDAO;
import com.srm.payroll.dao.PayrollResultDAO;
import com.srm.payroll.model.BatchSummary;
import com.srm.payroll.model.PayrollResult;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing background multithreaded payroll batch jobs.
 * Enforces single execution, thread naming, graceful cancellation, and clean shutdown.
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
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("A payroll batch run is already in progress.");
        }

        PayrollProgressListener wrappedListener = new PayrollProgressListener() {
            @Override
            public void onProgress(int processed, int total, PayrollResult latest) {
                listener.onProgress(processed, total, latest);
            }

            @Override
            public void onComplete(BatchSummary summary) {
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

        this.currentTask = new PayrollTask(employeeDAO, resultDAO, calculator, wrappedListener);
        this.lastRunId = this.currentTask.getRunId();
        this.batchThread = new Thread(this.currentTask, "payroll-batch");
        this.batchThread.setDaemon(true);
        this.batchThread.start();
    }

    public synchronized void cancel() {
        if (running.get() && currentTask != null) {
            currentTask.cancel();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public String getLastRunId() {
        return lastRunId;
    }

    public void shutdown() {
        cancel();
        if (batchThread != null && batchThread.isAlive()) {
            try {
                batchThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Interrupted while waiting for batch thread to join", e);
            }
        }
    }
}

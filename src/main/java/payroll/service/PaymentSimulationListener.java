package payroll.service;

/**
 * Listener interface for monitoring real-time bank payment simulation events.
 */
public interface PaymentSimulationListener {

    /**
     * Fired when an individual salary disbursement transaction completes.
     *
     * @param processed count of processed transactions so far
     * @param total     total number of employees to disburse to
     * @param record    details of the completed disbursement transaction
     */
    void onProgress(int processed, int total, DisbursementRecord record);

    /**
     * Fired when the entire payment simulation run finishes successfully.
     *
     * @param summary complete settlement summary
     */
    void onComplete(PaymentSimulationSummary summary);

    /**
     * Fired if an unrecoverable error interrupts the simulation.
     *
     * @param error root cause exception
     */
    void onError(Exception error);
}

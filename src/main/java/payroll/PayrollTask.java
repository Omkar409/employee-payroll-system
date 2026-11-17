package payroll;

import java.util.List;
import java.util.function.Consumer;

/**
 * Background worker task implementing Runnable for batch payroll processing.
 * Demonstrates:
 * - Multithreading: Runnable interface
 * - Decoupling long-running operations from the AWT Event Dispatch Thread (EDT)
 * - Thread safety with immutable employee snapshot (List.copyOf)
 */
public class PayrollTask implements Runnable {
    private final List<Employee> employees;
    private final PayrollService service;
    private final Consumer<String> progressListener;
    private final Runnable onCompletion;

    public PayrollTask(List<Employee> employees, PayrollService service) {
        this(employees, service, null, null);
    }

    public PayrollTask(List<Employee> employees, PayrollService service, Consumer<String> progressListener) {
        this(employees, service, progressListener, null);
    }

    public PayrollTask(List<Employee> employees, PayrollService service,
                       Consumer<String> progressListener, Runnable onCompletion) {
        this.employees = List.copyOf(employees);
        this.service = service;
        this.progressListener = progressListener;
        this.onCompletion = onCompletion;
    }

    @Override
    public void run() {
        System.out.println("PayrollTask started on thread: " + Thread.currentThread().getName());
        for (Employee employee : employees) {
            PayrollResult result = service.process(employee);
            String logLine = employee.getId() + " - "
                    + employee.getName() + " - "
                    + String.format("%.2f", result.getNet());

            // Standard console output as specified in syllabus (Page 20)
            System.out.println(logLine);

            if (progressListener != null) {
                progressListener.accept(logLine);
            }

            // Small delay to simulate realistic batch computation and thread execution
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("PayrollTask interrupted: " + e.getMessage());
                break;
            }
        }

        System.out.println("PayrollTask completed for " + employees.size() + " employees.");
        if (onCompletion != null) {
            onCompletion.run();
        }
    }
}

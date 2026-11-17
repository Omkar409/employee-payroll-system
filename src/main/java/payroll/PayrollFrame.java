package payroll;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

/**
 * Graphical User Interface for the Employee Payroll Management System.
 * Built strictly using Java AWT (java.awt.* and java.awt.event.*).
 *
 * Demonstrates:
 * - AWT Container: Frame, Panel
 * - AWT Components: Label, TextField, Button, TextArea, Choice
 * - Layout Managers: BorderLayout, GridLayout, FlowLayout
 * - Event Handling: ActionListener, ItemListener
 * - Adapter Classes: WindowAdapter
 * - Thread concurrency: Invoking background PayrollTask without blocking EDT
 * - Exception handling and error dialogues/messages
 */
public class PayrollFrame extends Frame implements ActionListener, ItemListener {

    private final TextField idField = new TextField(15);
    private final TextField nameField = new TextField(15);
    private final Choice typeChoice = new Choice();
    private final TextField salaryField = new TextField(15);
    private final TextField extraField = new TextField(15);
    private final Label extraLabel = new Label("Contract Allowance:");

    private final TextArea output = new TextArea(14, 65);

    // Primary action buttons
    private final Button addButton = new Button("Add Employee");
    private final Button calculateButton = new Button("Calculate");
    private final Button searchButton = new Button("Search by ID");
    private final Button deleteButton = new Button("Delete by ID");
    private final Button viewAllButton = new Button("View All");
    private final Button batchButton = new Button("Batch Process");

    // File operations buttons
    private final Button saveButton = new Button("Save Records");
    private final Button loadButton = new Button("Load Records");
    private final Button reportButton = new Button("Export Report");
    private final Button clearButton = new Button("Clear Output");

    // Domain models and services
    private final EmployeeRepository repository = new EmployeeRepository();
    private final PayrollService payrollService = new PayrollService(new StandardPayrollCalculator());
    private final PayrollFileService fileService = new PayrollFileService();

    private static final String DATA_FILE = "data/employees.txt";
    private static final String REPORT_FILE = "data/payroll_report.txt";

    public PayrollFrame() {
        setTitle("Employee Payroll Management System - SRM University AP");
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 247, 250));

        // Configure choices
        typeChoice.add("Permanent");
        typeChoice.add("Contract");
        typeChoice.add("Executive");
        typeChoice.addItemListener(this);

        // Header Panel
        Panel headerPanel = new Panel(new FlowLayout(FlowLayout.CENTER));
        Label headerTitle = new Label("SRM UNIVERSITY AP - EMPLOYEE PAYROLL MANAGEMENT SYSTEM");
        headerTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        headerPanel.add(headerTitle);

        // Form Panel (GridLayout: 5 rows x 2 columns)
        Panel form = new Panel(new GridLayout(5, 2, 8, 8));
        form.setFont(new Font("SansSerif", Font.PLAIN, 12));

        form.add(new Label("Employee ID (int):"));
        form.add(idField);

        form.add(new Label("Employee Name:"));
        form.add(nameField);

        form.add(new Label("Employee Category:"));
        form.add(typeChoice);

        form.add(new Label("Basic Salary (Rs.):"));
        form.add(salaryField);

        form.add(extraLabel);
        form.add(extraField);
        extraField.setEnabled(false); // Initially disabled for Permanent

        // Top container combining Header and Form
        Panel topContainer = new Panel(new BorderLayout(5, 5));
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(form, BorderLayout.CENTER);

        // Buttons Panel (2 rows of buttons)
        Panel buttonContainer = new Panel(new GridLayout(2, 1, 4, 4));

        Panel row1 = new Panel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        row1.add(addButton);
        row1.add(calculateButton);
        row1.add(searchButton);
        row1.add(deleteButton);
        row1.add(viewAllButton);

        Panel row2 = new Panel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        row2.add(batchButton);
        row2.add(saveButton);
        row2.add(loadButton);
        row2.add(reportButton);
        row2.add(clearButton);

        buttonContainer.add(row1);
        buttonContainer.add(row2);

        // Output Area styling
        output.setEditable(false);
        output.setFont(new Font("Monospaced", Font.PLAIN, 12));
        output.setBackground(Color.WHITE);

        // Add panels to Frame
        add(topContainer, BorderLayout.NORTH);
        add(output, BorderLayout.CENTER);
        add(buttonContainer, BorderLayout.SOUTH);

        // Register Action Listeners
        addButton.addActionListener(this);
        calculateButton.addActionListener(this);
        searchButton.addActionListener(this);
        deleteButton.addActionListener(this);
        viewAllButton.addActionListener(this);
        batchButton.addActionListener(this);
        saveButton.addActionListener(this);
        loadButton.addActionListener(this);
        reportButton.addActionListener(this);
        clearButton.addActionListener(this);

        // Register Window Listener for clean close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                dispose();
                System.exit(0);
            }
        });

        // Initialize with standard sample employees from academic specification
        seedInitialEmployees();

        setSize(780, 560);
        setLocationRelativeTo(null);
        setVisible(true);

        output.setText("Welcome to Employee Payroll Management System\n"
                + "Academic Project for CSE 309 - Advanced Java Programming\n"
                + "Repository initialized with " + repository.count() + " default employee records.\n"
                + "Select an operation above to begin.\n"
                + "=========================================================================\n");
    }

    private void seedInitialEmployees() {
        try {
            repository.add(new PermanentEmployee(101, "Ravi Kumar", 30000.00));
            repository.add(new ContractEmployee(102, "Meena", 25000.00, 1500.00));
            repository.add(new PermanentEmployee(103, "Neeraj Kumar", 80000.00));
            repository.add(new ContractEmployee(104, "P. Surya", 70000.00, 2000.00));
            repository.add(new ExecutiveEmployee(105, "Omkar", 85000.00, "Engineering", 5000.00));
            repository.add(new PermanentEmployee(106, "Kushwanth", 75000.00));
        } catch (Exception e) {
            System.err.println("Note seeding default employees: " + e.getMessage());
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        String selected = typeChoice.getSelectedItem();
        if ("Contract".equalsIgnoreCase(selected)) {
            extraLabel.setText("Contract Allowance (Rs.):");
            extraField.setEnabled(true);
            extraField.setText("1500.00");
        } else if ("Executive".equalsIgnoreCase(selected)) {
            extraLabel.setText("Executive Bonus (Rs.):");
            extraField.setEnabled(true);
            extraField.setText("5000.00");
        } else {
            extraLabel.setText("Allowance / Bonus (N/A):");
            extraField.setEnabled(false);
            extraField.setText("");
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();

        if (src == addButton) {
            addEmployee();
        } else if (src == calculateButton) {
            calculatePayroll();
        } else if (src == searchButton) {
            searchEmployee();
        } else if (src == deleteButton) {
            deleteEmployee();
        } else if (src == viewAllButton) {
            viewAllEmployees();
        } else if (src == batchButton) {
            startBatchProcessing();
        } else if (src == saveButton) {
            saveRecordsToFile();
        } else if (src == loadButton) {
            loadRecordsFromFile();
        } else if (src == reportButton) {
            exportReportToFile();
        } else if (src == clearButton) {
            output.setText("");
        }
    }

    private void addEmployee() {
        try {
            String idStr = idField.getText().trim();
            if (idStr.isEmpty()) {
                output.setText("Error: Employee ID is required.");
                return;
            }
            int id = Integer.parseInt(idStr);

            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                output.setText("Error: Employee name cannot be empty.");
                return;
            }

            String salStr = salaryField.getText().trim();
            if (salStr.isEmpty()) {
                output.setText("Error: Basic salary is required.");
                return;
            }
            double salary = Double.parseDouble(salStr);

            String type = typeChoice.getSelectedItem();
            Employee employee;

            if ("Contract".equalsIgnoreCase(type)) {
                String extraStr = extraField.getText().trim();
                double allowance = extraStr.isEmpty() ? 0.0 : Double.parseDouble(extraStr);
                employee = new ContractEmployee(id, name, salary, allowance);
            } else if ("Executive".equalsIgnoreCase(type)) {
                String extraStr = extraField.getText().trim();
                double bonus = extraStr.isEmpty() ? 0.0 : Double.parseDouble(extraStr);
                employee = new ExecutiveEmployee(id, name, salary, "Management", bonus);
            } else {
                employee = new PermanentEmployee(id, name, salary);
            }

            repository.add(employee);
            output.setText("SUCCESS: Employee added successfully.\n"
                    + "ID:   " + employee.getId() + "\n"
                    + "Name: " + employee.getName() + "\n"
                    + "Type: " + employee.getEmployeeType() + "\n"
                    + "Total active employees: " + repository.count() + "\n");

            clearInputFields();
        } catch (NumberFormatException e) {
            output.setText("Numeric Input Error: Please enter valid numbers for ID, Salary, and Allowance.");
        } catch (IllegalArgumentException e) {
            output.setText("Validation Error: " + e.getMessage());
        }
    }

    private void calculatePayroll() {
        try {
            String idStr = idField.getText().trim();
            if (idStr.isEmpty()) {
                output.setText("Enter an employee ID to calculate payroll.");
                return;
            }

            int id = Integer.parseInt(idStr);
            Employee employee = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + id));

            PayrollResult result = payrollService.process(employee);
            String summary = fileService.formatSummary(employee, result);
            output.setText(summary);
        } catch (NumberFormatException e) {
            output.setText("Error: Enter a valid integer employee ID.");
        } catch (IllegalArgumentException e) {
            output.setText("Error: " + e.getMessage());
        }
    }

    private void searchEmployee() {
        try {
            String idStr = idField.getText().trim();
            if (idStr.isEmpty()) {
                output.setText("Enter an Employee ID to search.");
                return;
            }
            int id = Integer.parseInt(idStr);
            repository.findById(id).ifPresentOrElse(emp -> {
                idField.setText(String.valueOf(emp.getId()));
                nameField.setText(emp.getName());
                salaryField.setText(String.format("%.2f", emp.getBasicSalary()));
                if (emp instanceof ContractEmployee) {
                    typeChoice.select("Contract");
                    extraField.setEnabled(true);
                    extraField.setText(String.format("%.2f", ((ContractEmployee) emp).getContractAllowance()));
                } else if (emp instanceof ExecutiveEmployee) {
                    typeChoice.select("Executive");
                    extraField.setEnabled(true);
                    extraField.setText(String.format("%.2f", ((ExecutiveEmployee) emp).getExecutiveBonus()));
                } else {
                    typeChoice.select("Permanent");
                    extraField.setEnabled(false);
                    extraField.setText("");
                }
                output.setText("Employee record found:\n"
                        + "ID: " + emp.getId() + "\n"
                        + "Name: " + emp.getName() + "\n"
                        + "Type: " + emp.getEmployeeType() + "\n"
                        + "Basic Salary: Rs. " + String.format("%.2f", emp.getBasicSalary()) + "\n");
            }, () -> output.setText("No employee found with ID: " + id));
        } catch (NumberFormatException e) {
            output.setText("Error: Employee ID must be an integer.");
        }
    }

    private void deleteEmployee() {
        try {
            String idStr = idField.getText().trim();
            if (idStr.isEmpty()) {
                output.setText("Enter an Employee ID to delete.");
                return;
            }
            int id = Integer.parseInt(idStr);
            boolean removed = repository.delete(id);
            if (removed) {
                output.setText("Employee ID " + id + " deleted successfully.\nRemaining employees: " + repository.count());
                clearInputFields();
            } else {
                output.setText("Cannot delete: Employee ID " + id + " does not exist.");
            }
        } catch (NumberFormatException e) {
            output.setText("Error: Enter a valid numeric employee ID.");
        }
    }

    private void viewAllEmployees() {
        List<Employee> all = repository.findAll();
        if (all.isEmpty()) {
            output.setText("No employee records currently in memory.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ALL REGISTERED EMPLOYEES (Count: ").append(all.size()).append(")\n");
        sb.append("-------------------------------------------------------------------------\n");
        sb.append(String.format("%-6s | %-20s | %-14s | %12s\n", "ID", "NAME", "CATEGORY", "BASIC SALARY"));
        sb.append("-------------------------------------------------------------------------\n");
        for (Employee emp : all) {
            sb.append(String.format("%-6d | %-20s | %-14s | %12.2f\n",
                    emp.getId(), emp.getName(), emp.getEmployeeType(), emp.getBasicSalary()));
        }
        sb.append("-------------------------------------------------------------------------\n");
        output.setText(sb.toString());
    }

    private void startBatchProcessing() {
        List<Employee> employeeList = repository.findAll();
        if (employeeList.isEmpty()) {
            output.setText("No employees available to process.");
            return;
        }

        output.setText("Launching Batch Payroll Processing on background Thread...\n");
        batchButton.setEnabled(false);

        // Demonstrates multithreading via Runnable (PayrollTask)
        PayrollTask task = new PayrollTask(
                employeeList,
                payrollService,
                logLine -> EventQueue.invokeLater(() -> output.append("Processed: " + logLine + "\n")),
                () -> EventQueue.invokeLater(() -> {
                    output.append("\nBatch Processing Completed Successfully for " + employeeList.size() + " employees!\n");
                    batchButton.setEnabled(true);
                })
        );

        Thread thread = new Thread(task, "Batch-Payroll-Worker");
        thread.start();
    }

    private void saveRecordsToFile() {
        try {
            List<Employee> list = repository.findAll();
            fileService.saveEmployees(list, DATA_FILE);

            // Also demonstrate byte-stream backup copy via FileInputStream/FileOutputStream
            String backupFile = "data/employees_backup.txt";
            fileService.copyFile(DATA_FILE, backupFile);

            output.setText("SUCCESS: Saved " + list.size() + " records to: " + DATA_FILE + "\n"
                    + "Backup created via byte streams at: " + backupFile);
        } catch (FileStorageException e) {
            output.setText("File Save Error: " + e.getMessage());
        }
    }

    private void loadRecordsFromFile() {
        try {
            List<Employee> loaded = fileService.loadEmployees(DATA_FILE);
            repository.clear();
            for (Employee emp : loaded) {
                repository.add(emp);
            }
            output.setText("SUCCESS: Loaded " + loaded.size() + " records from " + DATA_FILE + " into memory.\n");
            viewAllEmployees();
        } catch (FileStorageException e) {
            output.setText("File Load Error: " + e.getMessage());
        }
    }

    private void exportReportToFile() {
        try {
            List<Employee> list = repository.findAll();
            if (list.isEmpty()) {
                output.setText("No employees to include in report.");
                return;
            }
            fileService.generateReport(list, payrollService, REPORT_FILE);
            output.setText("SUCCESS: Batch Payroll Report generated and exported to:\n"
                    + new File(REPORT_FILE).getAbsolutePath() + "\n\n"
                    + fileService.formatSummary(list, payrollService));
        } catch (FileStorageException e) {
            output.setText("Report Export Error: " + e.getMessage());
        }
    }

    private void clearInputFields() {
        idField.setText("");
        nameField.setText("");
        salaryField.setText("");
        extraField.setText("");
    }
}

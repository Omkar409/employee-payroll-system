package payroll.ui;

import payroll.*;
import payroll.dao.DataAccessException;
import payroll.service.*;
import payroll.service.exception.ValidationException;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Main application dashboard (pure Java AWT).
 * Features: Add/Edit/Delete/Search employees, Calculate payroll, Run batch, File I/O operations.
 * Respects Role-based Access Control: ADMIN vs HR.
 */
public class PayrollFrame extends Frame implements ActionListener, ItemListener {

    private static final String DATA_FILE = "data/employees.txt";
    private static final String BACKUP_FILE = "data/employees_backup.txt";
    private static final String REPORT_FILE = "data/payroll_report.txt";

    private final User currentUser;
    private final EmployeeService employeeService;
    private final PayrollBatchService batchService;
    private final PayrollFileService fileService = new PayrollFileService();

    // Form fields
    private final TextField idField          = new TextField(12);
    private final TextField nameField        = new TextField(18);
    private final Choice   typeChoice       = new Choice();
    private final TextField salaryField      = new TextField(12);
    private final TextField extraField       = new TextField(12);
    private final Label     extraLabel       = new Label("Allowance/Bonus:");
    private final TextField bankAccountField = new TextField(18);

    // Output area
    private final TextArea output = new TextArea(14, 70);

    // Buttons
    private final Button addBtn        = new Button("Add");
    private final Button updateBtn     = new Button("Update Salary");
    private final Button calcBtn       = new Button("Calculate");
    private final Button searchBtn     = new Button("Search ID");
    private final Button deleteBtn     = new Button("Delete");
    private final Button viewAllBtn    = new Button("View All");
    private final Button batchBtn      = new Button("Batch Run");
    private final Button simBtn        = new Button("Payment Simulation");
    private final Button seed1000Btn   = new Button("Add 1000 People");
    private final Button cancelBtn     = new Button("Cancel Batch");
    private final Button clearBtn      = new Button("Clear");
    private final Button saveFileBtn   = new Button("Save Records");
    private final Button loadFileBtn   = new Button("Load Records");
    private final Button exportRepBtn  = new Button("Export Report");
    private final Button backupFileBtn = new Button("Backup File");

    private Label statusLabel;

    public PayrollFrame(User currentUser, EmployeeService employeeService, PayrollBatchService batchService) {
        super("Employee Payroll System  |  User: " + currentUser.getUsername()
                + " (" + currentUser.getRole().name() + ")");
        this.currentUser = currentUser;
        this.employeeService = employeeService;
        this.batchService = batchService;

        buildUI();
        applyRoleRestrictions();

        setSize(880, 680);
        setLocationRelativeTo(null);
        setVisible(false);

        output.setText("Welcome, " + currentUser.getFullName() + "!\n"
                + "Role: " + currentUser.getRole().name() + "\n"
                + "=========================================================\n"
                + "Use the form above to add, update, search, or calculate payroll.\n"
                + "Click 'Payment Simulation' to simulate bulk direct bank deposits.\n"
                + "File I/O buttons below allow saving, loading, report export, and backup.\n");
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        setBackground(new Color(245, 247, 250));

        // --- Header ---
        Label title = new Label("SRM University AP - Employee Payroll Management System", Label.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(title, BorderLayout.NORTH);

        // --- Form (GridLayout 6x2) ---
        Panel form = new Panel(new GridLayout(6, 2, 6, 6));
        typeChoice.add("Permanent");
        typeChoice.add("Contract");
        typeChoice.addItemListener(this);

        form.add(new Label("Employee ID:"));         form.add(idField);
        form.add(new Label("Name:"));                form.add(nameField);
        form.add(new Label("Category:"));            form.add(typeChoice);
        form.add(new Label("Basic Salary (Rs.):"));  form.add(salaryField);
        form.add(extraLabel);                        form.add(extraField);
        form.add(new Label("Bank Account No:"));     form.add(bankAccountField);
        extraField.setEnabled(false);

        // --- Buttons row 1: CRUD & Lookup ---
        Panel row1 = new Panel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        for (Button b : new Button[]{addBtn, updateBtn, calcBtn, searchBtn, deleteBtn, viewAllBtn})
            row1.add(b);

        // --- Buttons row 2: Batch & Payment Simulation ---
        Panel row2 = new Panel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        for (Button b : new Button[]{batchBtn, simBtn, seed1000Btn, cancelBtn, clearBtn})
            row2.add(b);

        // --- Buttons row 3: File I/O & Reports ---
        Panel row3 = new Panel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        for (Button b : new Button[]{saveFileBtn, loadFileBtn, exportRepBtn, backupFileBtn})
            row3.add(b);

        Panel btnContainer = new Panel(new GridLayout(3, 1, 4, 4));
        btnContainer.add(row1);
        btnContainer.add(row2);
        btnContainer.add(row3);

        // --- Status bar ---
        statusLabel = new Label(" ", Label.LEFT);
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(Color.DARK_GRAY);

        // --- Centre panel ---
        Panel centre = new Panel(new BorderLayout(6, 6));
        centre.add(form, BorderLayout.NORTH);

        // --- Output ---
        output.setEditable(false);
        output.setFont(new Font("Monospaced", Font.PLAIN, 12));
        output.setBackground(Color.WHITE);

        // --- South ---
        Panel south = new Panel(new BorderLayout(4, 4));
        south.add(btnContainer, BorderLayout.NORTH);
        south.add(statusLabel,  BorderLayout.SOUTH);

        add(centre, BorderLayout.CENTER);
        add(output, BorderLayout.CENTER);
        add(south,  BorderLayout.SOUTH);

        // Re-do layout: North=title+form, Center=output, South=buttons+status
        setLayout(new BorderLayout(8, 8));
        Panel topSection = new Panel(new BorderLayout(6, 6));
        topSection.add(title, BorderLayout.NORTH);
        topSection.add(form, BorderLayout.CENTER);
        add(topSection, BorderLayout.NORTH);
        add(output,     BorderLayout.CENTER);
        add(south,      BorderLayout.SOUTH);

        // Action listeners
        for (Button b : new Button[]{addBtn, updateBtn, calcBtn, searchBtn, deleteBtn, viewAllBtn,
                                     batchBtn, simBtn, seed1000Btn, cancelBtn, clearBtn,
                                     saveFileBtn, loadFileBtn, exportRepBtn, backupFileBtn})
            b.addActionListener(this);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                batchService.shutdown();
                SessionContext.clear();
                dispose();
                System.exit(0);
            }
        });
    }

    private void applyRoleRestrictions() {
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        deleteBtn.setEnabled(isAdmin);
        batchBtn.setEnabled(isAdmin);
        simBtn.setEnabled(isAdmin);
        seed1000Btn.setEnabled(isAdmin);
        loadFileBtn.setEnabled(isAdmin);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        String selected = typeChoice.getSelectedItem();
        if ("Contract".equalsIgnoreCase(selected)) {
            extraLabel.setText("Contract Allowance (Rs.):");
            extraField.setEnabled(true);
            extraField.setText("1500");
        } else {
            extraLabel.setText("Bonus (Rs.):");
            extraField.setEnabled(true);
            extraField.setText("5000");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == addBtn)             addEmployee();
        else if (src == updateBtn)      updateEmployee();
        else if (src == calcBtn)        calculatePayroll();
        else if (src == searchBtn)      searchEmployee();
        else if (src == deleteBtn)      deleteEmployee();
        else if (src == viewAllBtn)     viewAll();
        else if (src == batchBtn)       startBatch();
        else if (src == simBtn)         startPaymentSimulation();
        else if (src == seed1000Btn)    add1000Employees();
        else if (src == cancelBtn)      batchService.cancel();
        else if (src == clearBtn)       output.setText("");
        else if (src == saveFileBtn)    saveRecordsToFile();
        else if (src == loadFileBtn)    loadRecordsFromFile();
        else if (src == exportRepBtn)   exportReportToFile();
        else if (src == backupFileBtn)  backupDataFile();
    }

    // ========= Operations =========

    private void addEmployee() {
        try {
            int id = Integer.parseInt(idField.getText().trim());
            String name = nameField.getText().trim();
            if (name.isEmpty()) { output.setText("Error: Name is required."); return; }
            double salary = Double.parseDouble(salaryField.getText().trim());
            String type = typeChoice.getSelectedItem();

            Employee emp;
            if ("Contract".equalsIgnoreCase(type)) {
                double allowance = extraField.getText().trim().isEmpty() ? 0 : Double.parseDouble(extraField.getText().trim());
                emp = new ContractEmployee(id, name, salary, allowance, 12);
            } else {
                double bonus = extraField.getText().trim().isEmpty() ? 0 : Double.parseDouble(extraField.getText().trim());
                emp = new PermanentEmployee(id, name, salary, bonus);
            }
            emp.setEmployeeId(0); // let DB assign
            int generated = employeeService.addEmployee(emp);

            String bankAcc = bankAccountField.getText().trim();
            if (bankAcc.isEmpty()) {
                bankAcc = BankAccountStore.generateDefaultAccount(generated);
            }
            emp.setBankAccount(bankAcc);
            BankAccountStore.setAccount(generated, bankAcc);

            output.setText("SUCCESS: Employee added with DB ID: " + generated + "\n"
                    + "Name: " + name + "\nType: " + type + "\nBasic: Rs. " + String.format("%.2f", salary)
                    + "\nBank Account: " + bankAcc);
            clearForm();
        } catch (NumberFormatException ex) {
            output.setText("Input Error: Please enter valid numbers for ID, Salary, and Allowance/Bonus.");
        } catch (ValidationException | DataAccessException ex) {
            output.setText("Error: " + ex.getMessage());
        }
    }

    private void updateEmployee() {
        try {
            String idStr = idField.getText().trim();
            if (idStr.isEmpty()) {
                output.setText("Error: Enter an Employee ID to update salary and credentials.");
                return;
            }
            int id = Integer.parseInt(idStr);
            String name = nameField.getText().trim();
            if (name.isEmpty()) { output.setText("Error: Name is required for update."); return; }
            double salary = Double.parseDouble(salaryField.getText().trim());
            String type = typeChoice.getSelectedItem();

            Employee emp;
            if ("Contract".equalsIgnoreCase(type)) {
                double allowance = extraField.getText().trim().isEmpty() ? 0 : Double.parseDouble(extraField.getText().trim());
                emp = new ContractEmployee(id, name, salary, allowance, 12);
            } else {
                double bonus = extraField.getText().trim().isEmpty() ? 0 : Double.parseDouble(extraField.getText().trim());
                emp = new PermanentEmployee(id, name, salary, bonus);
            }

            String bankAcc = bankAccountField.getText().trim();
            if (!bankAcc.isEmpty()) {
                emp.setBankAccount(bankAcc);
                BankAccountStore.setAccount(id, bankAcc);
            }

            employeeService.updateEmployee(emp);
            PayrollResult r = employeeService.calculatePayroll(id);

            StringBuilder sb = new StringBuilder();
            sb.append("=========================================================\n");
            sb.append("SUCCESS: EMPLOYEE SALARY & CREDENTIALS UPDATED\n");
            sb.append("=========================================================\n");
            sb.append(String.format("Employee ID:     %d\n", id));
            sb.append(String.format("Employee Name:   %s\n", name));
            sb.append(String.format("Category:        %s\n", type));
            sb.append(String.format("New Basic:       Rs. %,.2f\n", salary));
            if (emp instanceof PermanentEmployee pe) {
                sb.append(String.format("Bonus:           Rs. %,.2f\n", pe.getBonus()));
            } else if (emp instanceof ContractEmployee ce) {
                sb.append(String.format("Contract Allow.: Rs. %,.2f\n", ce.getContractAllowance()));
            }
            sb.append(String.format("Bank Account:    %s\n", emp.getBankAccount()));
            sb.append("---------------------------------------------------------\n");
            sb.append(String.format("Recalculated Net:Rs. %,.2f\n", r.getNetSalary()));
            sb.append("=========================================================\n");

            output.setText(sb.toString());
            statusLabel.setText("Employee ID " + id + " updated successfully.");
        } catch (NumberFormatException ex) {
            output.setText("Input Error: Valid numbers required for ID, Salary, and Allowance/Bonus.");
        } catch (ValidationException | DataAccessException ex) {
            output.setText("Update Error: " + ex.getMessage());
            AlertDialog.show(this, "Update Error", ex.getMessage());
        }
    }

    private void calculatePayroll() {
        try {
            String idStr = idField.getText().trim();
            if (idStr.isEmpty()) { output.setText("Enter an Employee ID to calculate payroll."); return; }
            int id = Integer.parseInt(idStr);
            PayrollResult r = employeeService.calculatePayroll(id);
            output.setText(formatPayrollResult(r));
        } catch (NumberFormatException e) {
            output.setText("Error: Employee ID must be a number.");
        } catch (DataAccessException e) {
            output.setText("Error: " + e.getMessage());
        }
    }

    private void searchEmployee() {
        try {
            String idStr = idField.getText().trim();
            if (!idStr.isEmpty()) {
                int id = Integer.parseInt(idStr);
                employeeService.findById(id).ifPresentOrElse(emp -> {
                    idField.setText(String.valueOf(emp.getEmployeeId()));
                    nameField.setText(emp.getName());
                    salaryField.setText(String.format("%.2f", emp.getBasicSalary()));
                    bankAccountField.setText(emp.getBankAccount());
                    if (emp instanceof ContractEmployee ce) {
                        typeChoice.select("Contract");
                        extraField.setEnabled(true);
                        extraField.setText(String.format("%.2f", ce.getContractAllowance()));
                    } else if (emp instanceof PermanentEmployee pe) {
                        typeChoice.select("Permanent");
                        extraField.setEnabled(true);
                        extraField.setText(String.format("%.2f", pe.getBonus()));
                    }
                    output.setText("Found: " + emp);
                }, () -> output.setText("No employee found with ID: " + idStr));
            } else {
                String name = nameField.getText().trim();
                if (!name.isEmpty()) {
                    List<Employee> results = employeeService.searchByName(name);
                    if (results.isEmpty()) { output.setText("No employees found matching: " + name); return; }
                    StringBuilder sb = new StringBuilder("Search Results (" + results.size() + "):\n");
                    results.forEach(emp -> sb.append(emp).append("\n"));
                    output.setText(sb.toString());
                } else {
                    output.setText("Enter an Employee ID or Name to search.");
                }
            }
        } catch (NumberFormatException e) {
            output.setText("Error: Employee ID must be a number.");
        } catch (DataAccessException e) {
            output.setText("Error: " + e.getMessage());
        }
    }

    private void deleteEmployee() {
        try {
            String idStr = idField.getText().trim();
            if (idStr.isEmpty()) { output.setText("Enter Employee ID to delete."); return; }
            int id = Integer.parseInt(idStr);
            boolean confirm = ConfirmDialog.ask(this, "Confirm Delete",
                    "Delete employee with ID " + id + "?\nThis action cannot be undone.");
            if (!confirm) { output.setText("Delete cancelled."); return; }
            employeeService.deleteEmployee(id);
            output.setText("SUCCESS: Employee ID " + id + " deleted.");
            clearForm();
        } catch (NumberFormatException e) {
            output.setText("Error: Employee ID must be a number.");
        } catch (SecurityException e) {
            output.setText("Access Denied: " + e.getMessage());
        } catch (DataAccessException e) {
            output.setText("Error: " + e.getMessage());
        }
    }

    private void viewAll() {
        try {
            List<Employee> all = employeeService.listAll();
            if (all.isEmpty()) { output.setText("No employee records in database."); return; }
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-6s | %-20s | %-10s | %-16s | %12s\n", "ID", "NAME", "TYPE", "BANK ACCOUNT", "BASIC SALARY"));
            sb.append("-".repeat(74)).append("\n");
            for (Employee emp : all)
                sb.append(String.format("%-6d | %-20s | %-10s | %-16s | %12.2f\n",
                        emp.getEmployeeId(),
                        emp.getName().length() > 20 ? emp.getName().substring(0, 17) + "..." : emp.getName(),
                        emp.getEmployeeType().name(),
                        emp.getBankAccount(),
                        emp.getBasicSalary()));
            sb.append("-".repeat(74)).append("\n");
            sb.append("Total: ").append(all.size()).append(" employees\n");
            output.setText(sb.toString());
        } catch (DataAccessException e) {
            output.setText("Error loading employees: " + e.getMessage());
        }
    }

    private void startBatch() {
        if (batchService.isRunning()) {
            output.setText("A batch run is already in progress. Use Cancel to stop it.");
            return;
        }
        output.setText("Starting batch payroll run on background thread (payroll-batch)...\n");
        batchBtn.setEnabled(false);

        batchService.start(new PayrollProgressListener() {
            @Override
            public void onProgress(int processed, int total, PayrollResult latest) {
                EventQueue.invokeLater(() -> {
                    statusLabel.setText(String.format("Processing %d/%d  |  %s",
                            processed, total, latest.toString()));
                    output.append("  " + latest + "\n");
                });
            }
            @Override
            public void onComplete(BatchSummary summary) {
                EventQueue.invokeLater(() -> {
                    output.append("\n=== BATCH COMPLETE ===\n");
                    output.append("Processed: " + summary.total() + " | Net Total: Rs. "
                            + String.format("%.2f", summary.totalNet())
                            + " | Duration: " + summary.durationMillis() + " ms\n");
                    statusLabel.setText("Batch complete. Run ID: " + batchService.getLastRunId());
                    batchBtn.setEnabled(currentUser.getRole() == Role.ADMIN);
                });
            }
            @Override
            public void onCancelled(int processed, int total) {
                EventQueue.invokeLater(() -> {
                    output.append("\nBatch cancelled after " + processed + "/" + total + " employees.\n");
                    statusLabel.setText("Batch cancelled.");
                    batchBtn.setEnabled(currentUser.getRole() == Role.ADMIN);
                });
            }
            @Override
            public void onError(Exception cause) {
                EventQueue.invokeLater(() -> {
                    output.append("\nBatch ERROR: " + cause.getMessage() + "\n");
                    statusLabel.setText("Batch failed.");
                    batchBtn.setEnabled(currentUser.getRole() == Role.ADMIN);
                    AlertDialog.show(PayrollFrame.this, "Batch Error", cause.getMessage());
                });
            }
        });
    }

    private String formatPayrollResult(PayrollResult r) {
        return "PAYROLL CALCULATION\n"
                + "--------------------------------------\n"
                + String.format("Employee ID:    %d\n", r.getEmployeeId())
                + String.format("Employee Name:  %s\n", r.getEmployeeName())
                + String.format("Gross Salary:   Rs. %,.2f\n", r.getGrossSalary())
                + String.format("Deductions:     Rs. %,.2f\n", r.getDeductions())
                + String.format("Net Salary:     Rs. %,.2f\n", r.getNetSalary())
                + "--------------------------------------\n";
    }

    private void add1000Employees() {
        if (!ConfirmDialog.ask(this, "Confirm Bulk Addition",
                "Are you sure you want to generate and insert 1,000 employees into MySQL?")) {
            return;
        }

        output.setText("Generating 1,000 employees...\n");
        seed1000Btn.setEnabled(false);
        statusLabel.setText("Inserting 1,000 employees into MySQL database in batch...");

        new Thread(() -> {
            try {
                long start = System.currentTimeMillis();
                List<Employee> batch = generate1000Employees();
                employeeService.addBatch(batch);
                long duration = System.currentTimeMillis() - start;
                int totalCount = employeeService.count();

                EventQueue.invokeLater(() -> {
                    output.append("=========================================================\n");
                    output.append("SUCCESS: Added 1,000 employees in " + duration + " ms!\n");
                    output.append("500 Permanent Employees (with bonuses)\n");
                    output.append("500 Contract Employees (with allowances & terms)\n");
                    output.append("Total employees in MySQL database now: " + totalCount + "\n");
                    output.append("=========================================================\n");
                    output.append("Tip: Click 'Batch Run' to calculate payroll for all of them!\n");
                    statusLabel.setText("1,000 employees inserted successfully in " + duration + " ms.");
                    seed1000Btn.setEnabled(currentUser.getRole() == Role.ADMIN);
                });
            } catch (Exception ex) {
                EventQueue.invokeLater(() -> {
                    output.append("ERROR inserting 1,000 employees: " + ex.getMessage() + "\n");
                    statusLabel.setText("Bulk insertion failed.");
                    seed1000Btn.setEnabled(currentUser.getRole() == Role.ADMIN);
                    AlertDialog.show(PayrollFrame.this, "Batch Insertion Error", ex.getMessage());
                });
            }
        }, "bulk-employee-inserter").start();
    }

    private List<Employee> generate1000Employees() {
        String[] firstNames = {
            "Aarav", "Aditi", "Akhil", "Ananya", "Arjun", "Bhavya", "Chetan", "Deepa",
            "Dev", "Divya", "Gautam", "Harish", "Ishaan", "Kavya", "Kiran", "Kushwanth",
            "Manish", "Meena", "Mohan", "Neha", "Neeraj", "Nikhil", "Omkar", "Pooja",
            "Pranav", "Rahul", "Rakesh", "Ravi", "Rohan", "Sandhya", "Sanjay", "Shreya",
            "Sneha", "Suresh", "Surya", "Tarun", "Varun", "Vikas", "Vinay", "Yamini"
        };
        String[] lastNames = {
            "Kumar", "Reddy", "Sharma", "Verma", "Patel", "Rao", "Nair", "Iyer",
            "Singh", "Choudhury", "Gupta", "Mishra", "Joshi", "Das", "Bose", "Menon"
        };

        List<Employee> list = new ArrayList<>(1000);
        Random rand = new Random();
        String[] bankPrefixes = {"SBIN", "HDFC", "ICIC", "PUNB", "BARB", "UBIN"};

        for (int i = 1; i <= 1000; i++) {
            String fName = firstNames[rand.nextInt(firstNames.length)];
            String lName = lastNames[rand.nextInt(lastNames.length)];
            String fullName = fName + " " + lName + " #" + i;

            // Salaries between 25,000 and 150,000 rounded to 500
            double basicSalary = 25000 + (rand.nextInt(250) * 500);

            Employee emp;
            if (i % 2 == 1) {
                // Permanent Employee
                double bonus = 3000 + (rand.nextInt(35) * 500);
                emp = new PermanentEmployee(0, fullName, basicSalary, bonus);
            } else {
                // Contract Employee
                double contractAllowance = 2000 + (rand.nextInt(20) * 500);
                int months = 6 + rand.nextInt(19); // 6 to 24 months
                emp = new ContractEmployee(0, fullName, basicSalary, contractAllowance, months);
            }

            String prefix = bankPrefixes[rand.nextInt(bankPrefixes.length)];
            String bankAccount = prefix + String.format("%010d", 1000000 + i);
            emp.setBankAccount(bankAccount);
            list.add(emp);
        }
        return list;
    }

    // ========= Bank Salary Payment Simulation =========

    private void startPaymentSimulation() {
        if (batchService.isRunning()) {
            output.setText("A batch process is currently running. Please wait for it to finish.");
            return;
        }

        try {
            List<Employee> all = employeeService.listAll();
            if (all.isEmpty()) {
                output.setText("No employees found in database. Please add employees or click 'Add 1000 People' first.");
                return;
            }

            boolean confirm = ConfirmDialog.ask(this, "Bank Salary Disbursement Simulation",
                    "Simulate bulk automated direct bank deposits for " + all.size() + " employees\n"
                    + "from Company Treasury Account (SRM-CORP-TREASURY-001)?\n\n"
                    + "[Note: Simulation mode - of course no actual money will be transferred]");
            if (!confirm) {
                output.setText("Payment simulation cancelled.");
                return;
            }

            output.setText("========================================================================================\n");
            output.append("INITIALIZING AUTOMATED CLEARING HOUSE (ACH / NEFT) BANKING GATEWAY SIMULATION...\n");
            output.append("DEBIT ACCOUNT: State Bank of India | SRM-CORP-TREASURY-001\n");
            output.append("[NOTE: SIMULATION MODE - NO ACTUAL CURRENCY TRANSFERRED - DEMONSTRATION PURPOSES ONLY]\n");
            output.append("========================================================================================\n\n");
            output.append("Starting automated disbursement pipeline...\n\n");

            simBtn.setEnabled(false);
            statusLabel.setText("Starting payment simulation for " + all.size() + " employees...");

            new Thread(() -> {
                try {
                    PaymentSimulationService simService = new PaymentSimulationService();
                    simService.simulatePayout(all, new StandardPayrollCalculator(), new PaymentSimulationListener() {
                        @Override
                        public void onProgress(int processed, int total, DisbursementRecord record) {
                            EventQueue.invokeLater(() -> {
                                statusLabel.setText(String.format("Transferring %d/%d: %s -> A/C %s (Rs. %,.2f)",
                                        processed, total, record.employeeName(), record.bankAccount(), record.netAmount()));
                                if (total <= 50 || processed % 10 == 0 || processed == total) {
                                    output.append(String.format("  [SUCCESS] Emp #%-5d %-20s -> A/C %-16s | Net: Rs. %10.2f | %s\n",
                                            record.employeeId(),
                                            record.employeeName().length() > 20 ? record.employeeName().substring(0, 17) + "..." : record.employeeName(),
                                            record.bankAccount(),
                                            record.netAmount(),
                                            record.utr()));
                                }
                            });
                        }

                        @Override
                        public void onComplete(PaymentSimulationSummary summary) {
                            EventQueue.invokeLater(() -> {
                                output.append("\n" + summary.formatSummaryReport());
                                statusLabel.setText("Disbursement simulation completed: " + summary.totalEmployees() + " employees paid.");
                                simBtn.setEnabled(currentUser.getRole() == Role.ADMIN);
                                AlertDialog.show(PayrollFrame.this, "Payment Simulation Complete",
                                        "Bank Disbursement Simulation Finished!\n\n"
                                        + "Recipients Paid: " + summary.totalEmployees() + "\n"
                                        + "Total Disbursed: Rs. " + String.format("%,.2f", summary.totalDisbursed()) + "\n"
                                        + "Company Ending Balance: Rs. " + String.format("%,.2f", summary.companyEndingBalance()) + "\n"
                                        + "Saved to: data/payment_simulation_report.txt\n\n"
                                        + "(Simulation mode - no actual currency transferred)");
                            });
                        }

                        @Override
                        public void onError(Exception cause) {
                            EventQueue.invokeLater(() -> {
                                output.append("\nSimulation ERROR: " + cause.getMessage() + "\n");
                                statusLabel.setText("Payment simulation failed.");
                                simBtn.setEnabled(currentUser.getRole() == Role.ADMIN);
                                AlertDialog.show(PayrollFrame.this, "Simulation Error", cause.getMessage());
                            });
                        }
                    });
                } catch (Exception ex) {
                    EventQueue.invokeLater(() -> {
                        output.append("Simulation execution error: " + ex.getMessage() + "\n");
                        simBtn.setEnabled(currentUser.getRole() == Role.ADMIN);
                    });
                }
            }, "payment-simulation-thread").start();

        } catch (DataAccessException ex) {
            output.setText("Database error loading employees: " + ex.getMessage());
        }
    }

    // ========= File I/O Operations =========

    private void saveRecordsToFile() {
        try {
            List<Employee> all = employeeService.listAll();
            if (all.isEmpty()) {
                output.setText("No employee records in database to save.");
                return;
            }
            fileService.saveEmployees(all, DATA_FILE);
            File f = new File(DATA_FILE);
            StringBuilder sb = new StringBuilder();
            sb.append("=========================================================\n");
            sb.append("FILE I/O: PERSISTENT STORAGE (SAVE EMPLOYEES)\n");
            sb.append("=========================================================\n");
            sb.append("Successfully saved ").append(all.size()).append(" employee records to disk.\n");
            sb.append("File Path:     ").append(f.getPath()).append(" (").append(f.getAbsolutePath()).append(")\n");
            sb.append("File Size:     ").append(f.length()).append(" bytes\n");
            sb.append("Stream Type:   Character Stream (BufferedWriter / FileWriter)\n");
            sb.append("Format:        CSV (ID, Name, Type, Basic, Bonus/Allowance, Months, Bank Account)\n");
            sb.append("=========================================================\n");
            sb.append("Tip: Click 'Backup File' to create a byte-stream backup (FileInputStream/FileOutputStream).\n");
            output.setText(sb.toString());
            statusLabel.setText("Saved " + all.size() + " records to " + DATA_FILE);
        } catch (IOException ex) {
            output.setText("I/O Error saving records: " + ex.getMessage());
            AlertDialog.show(this, "Save Error", "Failed to save file: " + ex.getMessage());
        } catch (DataAccessException ex) {
            output.setText("Database Error loading records: " + ex.getMessage());
            AlertDialog.show(this, "Database Error", ex.getMessage());
        }
    }

    private void loadRecordsFromFile() {
        if (!ConfirmDialog.ask(this, "Load Records from File",
                "Load employees from '" + DATA_FILE + "' and insert into database?")) {
            return;
        }

        try {
            List<Employee> loaded = fileService.loadEmployees(DATA_FILE);
            if (loaded.isEmpty()) {
                output.setText("File '" + DATA_FILE + "' is empty or contains no records.");
                return;
            }

            long start = System.currentTimeMillis();
            employeeService.addBatch(loaded);
            long duration = System.currentTimeMillis() - start;
            int totalCount = employeeService.count();

            StringBuilder sb = new StringBuilder();
            sb.append("=========================================================\n");
            sb.append("FILE I/O: PERSISTENT STORAGE (LOAD EMPLOYEES)\n");
            sb.append("=========================================================\n");
            sb.append("Successfully loaded and imported ").append(loaded.size()).append(" records from disk.\n");
            sb.append("Source File:   ").append(DATA_FILE).append("\n");
            sb.append("Stream Type:   Character Stream (BufferedReader / FileReader)\n");
            sb.append("Import Time:   ").append(duration).append(" ms\n");
            sb.append("Total In DB:   ").append(totalCount).append(" employees\n");
            sb.append("=========================================================\n");
            output.setText(sb.toString());
            statusLabel.setText("Imported " + loaded.size() + " records from " + DATA_FILE);
        } catch (FileNotFoundException ex) {
            output.setText("File not found: " + DATA_FILE + "\nPlease click 'Save Records' first to create the data file.");
            AlertDialog.show(this, "File Not Found", "Data file does not exist: " + DATA_FILE + "\nPlease save records first.");
        } catch (IOException ex) {
            output.setText("I/O Parsing Error: " + ex.getMessage());
            AlertDialog.show(this, "Load Error", "Failed to parse records: " + ex.getMessage());
        } catch (DataAccessException ex) {
            output.setText("Database Error inserting records: " + ex.getMessage());
            AlertDialog.show(this, "Database Error", ex.getMessage());
        }
    }

    private void exportReportToFile() {
        try {
            List<Employee> all = employeeService.listAll();
            if (all.isEmpty()) {
                output.setText("No employee records found in database to export.");
                return;
            }

            PayrollCalculator calc = new StandardPayrollCalculator();
            fileService.generateReport(all, calc, REPORT_FILE);
            File f = new File(REPORT_FILE);

            StringBuilder sb = new StringBuilder();
            sb.append("=========================================================\n");
            sb.append("FILE I/O: PAYROLL REPORT EXPORTED\n");
            sb.append("=========================================================\n");
            sb.append("Successfully generated and exported batch report to disk.\n");
            sb.append("File Path:     ").append(f.getPath()).append(" (").append(f.getAbsolutePath()).append(")\n");
            sb.append("File Size:     ").append(f.length()).append(" bytes\n");
            sb.append("Features Used: Method Overloading + Character Streams\n");
            sb.append("=========================================================\n\n");
            sb.append(fileService.formatSummary(all, calc));

            output.setText(sb.toString());
            statusLabel.setText("Report exported to " + REPORT_FILE);
        } catch (IOException ex) {
            output.setText("I/O Error exporting report: " + ex.getMessage());
            AlertDialog.show(this, "Export Error", "Failed to export report: " + ex.getMessage());
        } catch (DataAccessException ex) {
            output.setText("Database Error: " + ex.getMessage());
            AlertDialog.show(this, "Database Error", ex.getMessage());
        }
    }

    private void backupDataFile() {
        try {
            File srcFile = new File(DATA_FILE);
            if (!srcFile.exists()) {
                output.setText("Cannot create backup: Source file '" + DATA_FILE + "' does not exist.\nPlease click 'Save Records' first.");
                AlertDialog.show(this, "Backup Failed", "Source file '" + DATA_FILE + "' does not exist.\nSave records first.");
                return;
            }

            fileService.copyFile(DATA_FILE, BACKUP_FILE);
            File destFile = new File(BACKUP_FILE);

            StringBuilder sb = new StringBuilder();
            sb.append("=========================================================\n");
            sb.append("FILE I/O: BYTE STREAM FILE BACKUP\n");
            sb.append("=========================================================\n");
            sb.append("Successfully copied data file byte-for-byte!\n");
            sb.append("Source:        ").append(DATA_FILE).append(" (").append(srcFile.length()).append(" bytes)\n");
            sb.append("Destination:   ").append(BACKUP_FILE).append(" (").append(destFile.length()).append(" bytes)\n");
            sb.append("Stream Type:   Byte Streams (FileInputStream / FileOutputStream)\n");
            sb.append("Exception Handling: Guaranteed stream closure in finally block\n");
            sb.append("=========================================================\n");
            output.setText(sb.toString());
            statusLabel.setText("Backup created at " + BACKUP_FILE);
        } catch (IOException ex) {
            output.setText("I/O Error creating backup: " + ex.getMessage());
            AlertDialog.show(this, "Backup Error", "Failed to backup file: " + ex.getMessage());
        }
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        salaryField.setText("");
        extraField.setText("");
        bankAccountField.setText("");
    }
}

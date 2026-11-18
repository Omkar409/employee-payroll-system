package payroll.ui;

import payroll.*;
import payroll.dao.DataAccessException;
import payroll.service.*;
import payroll.service.exception.ValidationException;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Main application dashboard (pure Java AWT).
 * Features: Add/Edit/Delete/Search employees, Calculate payroll, Run batch, Export report.
 * Respects Role-based Access Control: ADMIN vs HR.
 */
public class PayrollFrame extends Frame implements ActionListener, ItemListener {

    private final User currentUser;
    private final EmployeeService employeeService;
    private final PayrollBatchService batchService;

    // Form fields
    private final TextField idField      = new TextField(12);
    private final TextField nameField    = new TextField(18);
    private final Choice   typeChoice   = new Choice();
    private final TextField salaryField  = new TextField(12);
    private final TextField extraField   = new TextField(12);
    private final Label     extraLabel   = new Label("Allowance/Bonus:");

    // Output area
    private final TextArea output = new TextArea(14, 70);

    // Buttons
    private final Button addBtn      = new Button("Add");
    private final Button calcBtn     = new Button("Calculate");
    private final Button searchBtn   = new Button("Search ID");
    private final Button deleteBtn   = new Button("Delete");
    private final Button viewAllBtn  = new Button("View All");
    private final Button batchBtn    = new Button("Batch Run");
    private final Button seed1000Btn = new Button("Add 1000 People");
    private final Button cancelBtn   = new Button("Cancel Batch");
    private final Button clearBtn    = new Button("Clear");

    private Label statusLabel;

    public PayrollFrame(User currentUser, EmployeeService employeeService, PayrollBatchService batchService) {
        super("Employee Payroll System  |  User: " + currentUser.getUsername()
                + " (" + currentUser.getRole().name() + ")");
        this.currentUser = currentUser;
        this.employeeService = employeeService;
        this.batchService = batchService;

        buildUI();
        applyRoleRestrictions();

        setSize(820, 600);
        setLocationRelativeTo(null);
        setVisible(false);

        output.setText("Welcome, " + currentUser.getFullName() + "!\n"
                + "Role: " + currentUser.getRole().name() + "\n"
                + "=========================================================\n"
                + "Use the form above to manage employees and run payroll.\n");
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        setBackground(new Color(245, 247, 250));

        // --- Header ---
        Label title = new Label("SRM University AP - Employee Payroll Management System", Label.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(title, BorderLayout.NORTH);

        // --- Form (GridLayout 5x2) ---
        Panel form = new Panel(new GridLayout(5, 2, 6, 6));
        typeChoice.add("Permanent");
        typeChoice.add("Contract");
        typeChoice.addItemListener(this);

        form.add(new Label("Employee ID:")); form.add(idField);
        form.add(new Label("Name:"));        form.add(nameField);
        form.add(new Label("Category:"));   form.add(typeChoice);
        form.add(new Label("Basic Salary (Rs.):"));  form.add(salaryField);
        form.add(extraLabel);               form.add(extraField);
        extraField.setEnabled(false);

        // --- Buttons row 1 ---
        Panel row1 = new Panel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        for (Button b : new Button[]{addBtn, calcBtn, searchBtn, deleteBtn, viewAllBtn})
            row1.add(b);

        // --- Buttons row 2 ---
        Panel row2 = new Panel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        for (Button b : new Button[]{batchBtn, seed1000Btn, cancelBtn, clearBtn})
            row2.add(b);

        Panel btnContainer = new Panel(new GridLayout(2, 1, 4, 4));
        btnContainer.add(row1);
        btnContainer.add(row2);

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
        for (Button b : new Button[]{addBtn, calcBtn, searchBtn, deleteBtn, viewAllBtn, batchBtn, seed1000Btn, cancelBtn, clearBtn})
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
        seed1000Btn.setEnabled(isAdmin);
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
        if (src == addBtn)          addEmployee();
        else if (src == calcBtn)     calculatePayroll();
        else if (src == searchBtn)   searchEmployee();
        else if (src == deleteBtn)   deleteEmployee();
        else if (src == viewAllBtn)  viewAll();
        else if (src == batchBtn)    startBatch();
        else if (src == seed1000Btn) add1000Employees();
        else if (src == cancelBtn)   batchService.cancel();
        else if (src == clearBtn)    output.setText("");
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
            output.setText("SUCCESS: Employee added with DB ID: " + generated + "\n"
                    + "Name: " + name + "\nType: " + type + "\nBasic: Rs. " + String.format("%.2f", salary));
            clearForm();
        } catch (NumberFormatException ex) {
            output.setText("Input Error: Please enter valid numbers for ID, Salary, and Allowance/Bonus.");
        } catch (ValidationException | DataAccessException ex) {
            output.setText("Error: " + ex.getMessage());
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
            sb.append(String.format("%-6s | %-22s | %-10s | %12s\n", "ID", "NAME", "TYPE", "BASIC SALARY"));
            sb.append("-".repeat(60)).append("\n");
            for (Employee emp : all)
                sb.append(String.format("%-6d | %-22s | %-10s | %12.2f\n",
                        emp.getEmployeeId(), emp.getName(),
                        emp.getEmployeeType().name(), emp.getBasicSalary()));
            sb.append("-".repeat(60)).append("\n");
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

        for (int i = 1; i <= 1000; i++) {
            String fName = firstNames[rand.nextInt(firstNames.length)];
            String lName = lastNames[rand.nextInt(lastNames.length)];
            String fullName = fName + " " + lName + " #" + i;

            // Salaries between 25,000 and 150,000 rounded to 500
            double basicSalary = 25000 + (rand.nextInt(250) * 500);

            if (i % 2 == 1) {
                // Permanent Employee
                double bonus = 3000 + (rand.nextInt(35) * 500);
                list.add(new PermanentEmployee(0, fullName, basicSalary, bonus));
            } else {
                // Contract Employee
                double contractAllowance = 2000 + (rand.nextInt(20) * 500);
                int months = 6 + rand.nextInt(19); // 6 to 24 months
                list.add(new ContractEmployee(0, fullName, basicSalary, contractAllowance, months));
            }
        }
        return list;
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        salaryField.setText("");
        extraField.setText("");
    }
}

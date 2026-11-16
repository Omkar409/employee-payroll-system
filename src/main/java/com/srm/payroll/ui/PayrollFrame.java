package com.srm.payroll.ui;

import com.srm.payroll.dao.DataAccessException;
import com.srm.payroll.dao.impl.UserDAOImpl;
import com.srm.payroll.model.*;
import com.srm.payroll.service.AuthService;
import com.srm.payroll.service.EmployeeService;
import com.srm.payroll.service.PayrollBatchService;
import com.srm.payroll.service.PayrollProgressListener;
import com.srm.payroll.service.SessionContext;
import com.srm.payroll.service.exception.ValidationException;
import com.srm.payroll.util.InputValidator;
import com.srm.payroll.util.ReportExporter;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application window for Employee and Payroll Management.
 * Strictly adheres to Java AWT (no Swing).
 */
public class PayrollFrame extends Frame implements PayrollProgressListener {

    private final EmployeeService employeeService;
    private final PayrollBatchService batchService;
    private final User currentUser;

    // Form inputs
    private TextField txtId;
    private TextField txtName;
    private TextField txtBasicSalary;
    private TextField txtBonus;
    private TextField txtAllowance;
    private TextField txtMonths;
    private Choice choType;

    // CardLayout container for type-specific inputs
    private Panel typeFieldsPanel;
    private CardLayout typeCardLayout;

    // List and Output
    private java.awt.List employeeList;
    private TextArea txtOutput;
    private TextField txtSearch;

    // Labels
    private Label lblProgress;
    private Label lblUser;

    // Action buttons
    private Button btnAdd;
    private Button btnSearch;
    private Button btnUpdate;
    private Button btnDelete;
    private Button btnCalculate;
    private Button btnRunBatch;
    private Button btnCancelBatch;
    private Button btnExportCsv;
    private Button btnClearForm;
    private Button btnLogout;

    // Cache of loaded employees
    private final List<Employee> loadedEmployees = new ArrayList<>();
    private final List<PayrollResult> latestBatchResults = new ArrayList<>();
    private BatchSummary latestSummary;

    public PayrollFrame(User user, EmployeeService es, PayrollBatchService bs) {
        super("Employee Payroll Management System v2");
        this.currentUser = user;
        this.employeeService = es;
        this.batchService = bs;

        buildUI();
        registerListeners();
        applyRolePermissions();
        refreshEmployeeList();
    }

    private void buildUI() {
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        setBackground(SystemColor.control);

        // ------------------ NORTH: Header ------------------
        Panel headerPanel = new Panel(new BorderLayout(10, 10));
        headerPanel.setBackground(new Color(230, 235, 245));
        headerPanel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        Label lblTitle = new Label("  Employee Payroll Management System  ", Label.LEFT);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));

        Panel userPanel = new Panel(new FlowLayout(FlowLayout.RIGHT, 15, 6));
        lblUser = new Label("Logged in as: " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLogout = new Button(" Logout ");
        btnLogout.setFont(new Font("SansSerif", Font.PLAIN, 12));

        userPanel.add(lblUser);
        userPanel.add(btnLogout);

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(userPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // ------------------ WEST: Employee Form ------------------
        Panel westPanel = new Panel(new BorderLayout(10, 10));
        westPanel.setPreferredSize(new Dimension(340, 500));

        Label lblFormTitle = new Label("Employee Details", Label.CENTER);
        lblFormTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        westPanel.add(lblFormTitle, BorderLayout.NORTH);

        Panel formFields = new Panel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtId = new TextField(15);
        txtId.setEditable(false); // Managed automatically by system/DB
        txtName = new TextField(15);
        txtBasicSalary = new TextField(15);

        choType = new Choice();
        choType.add("PERMANENT");
        choType.add("CONTRACT");

        int r = 0;
        addFormGridRow(formFields, gbc, r++, "ID:", txtId);
        addFormGridRow(formFields, gbc, r++, "Name:", txtName);
        addFormGridRow(formFields, gbc, r++, "Basic Salary:", txtBasicSalary);
        addFormGridRow(formFields, gbc, r++, "Type:", choType);

        // CardLayout container for dynamic fields
        typeCardLayout = new CardLayout();
        typeFieldsPanel = new Panel(typeCardLayout);

        // Permanent Card
        Panel permCard = new Panel(new GridBagLayout());
        txtBonus = new TextField(15);
        addFormGridRow(permCard, gbc, 0, "Bonus:", txtBonus);
        typeFieldsPanel.add(permCard, "PERMANENT");

        // Contract Card
        Panel contractCard = new Panel(new GridBagLayout());
        txtAllowance = new TextField(15);
        txtMonths = new TextField(15);
        addFormGridRow(contractCard, gbc, 0, "Allowance:", txtAllowance);
        addFormGridRow(contractCard, gbc, 1, "Months:", txtMonths);
        typeFieldsPanel.add(contractCard, "CONTRACT");

        gbc.gridx = 0;
        gbc.gridy = r++;
        gbc.gridwidth = 2;
        formFields.add(typeFieldsPanel, gbc);

        westPanel.add(formFields, BorderLayout.CENTER);

        // CRUD Button panel on West
        Panel crudPanel = new Panel(new GridLayout(3, 2, 8, 8));
        btnAdd = new Button("Add Employee");
        btnUpdate = new Button("Update");
        btnDelete = new Button("Delete");
        btnCalculate = new Button("Calculate Salary");
        btnClearForm = new Button("Clear Form");
        btnSearch = new Button("Search By ID");

        crudPanel.add(btnAdd);
        crudPanel.add(btnUpdate);
        crudPanel.add(btnDelete);
        crudPanel.add(btnCalculate);
        crudPanel.add(btnClearForm);
        crudPanel.add(btnSearch);

        westPanel.add(crudPanel, BorderLayout.SOUTH);
        add(westPanel, BorderLayout.WEST);

        // ------------------ CENTER: List & Output Console ------------------
        Panel centerPanel = new Panel(new BorderLayout(8, 8));

        // Center-North: Search filter bar
        Panel searchBar = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchBar.add(new Label("Filter by Name / ID:"));
        txtSearch = new TextField(20);
        Button btnSearchFilter = new Button("Filter");
        Button btnResetFilter = new Button("Show All");
        searchBar.add(txtSearch);
        searchBar.add(btnSearchFilter);
        searchBar.add(btnResetFilter);
        centerPanel.add(searchBar, BorderLayout.NORTH);

        // Center split: Employee List (Top) and Output/Log (Bottom)
        Panel centerContent = new Panel(new GridLayout(2, 1, 8, 8));

        Panel listPanel = new Panel(new BorderLayout(4, 4));
        Label lblListTitle = new Label("Employee Directory (Click to select & edit):", Label.LEFT);
        lblListTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        listPanel.add(lblListTitle, BorderLayout.NORTH);

        employeeList = new java.awt.List(10, false);
        employeeList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        listPanel.add(employeeList, BorderLayout.CENTER);
        centerContent.add(listPanel);

        Panel outputPanel = new Panel(new BorderLayout(4, 4));
        Label lblOutputTitle = new Label("Payroll Calculation & Batch Output Console:", Label.LEFT);
        lblOutputTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        outputPanel.add(lblOutputTitle, BorderLayout.NORTH);

        txtOutput = new TextArea("", 10, 60, TextArea.SCROLLBARS_BOTH);
        txtOutput.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtOutput.setEditable(false);
        outputPanel.add(txtOutput, BorderLayout.CENTER);
        centerContent.add(outputPanel);

        centerPanel.add(centerContent, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // ------------------ SOUTH: Batch & Status Bar ------------------
        Panel southPanel = new Panel(new BorderLayout(10, 6));
        southPanel.setBackground(new Color(240, 240, 245));

        Panel batchControlPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 15, 6));
        btnRunBatch = new Button("   Run Batch Payroll   ");
        btnRunBatch.setFont(new Font("SansSerif", Font.BOLD, 12));

        btnCancelBatch = new Button(" Cancel Batch ");
        btnCancelBatch.setEnabled(false);

        btnExportCsv = new Button(" Export Results (CSV) ");

        batchControlPanel.add(btnRunBatch);
        batchControlPanel.add(btnCancelBatch);
        batchControlPanel.add(btnExportCsv);

        lblProgress = new Label("Status: Ready", Label.LEFT);
        lblProgress.setFont(new Font("SansSerif", Font.PLAIN, 12));

        southPanel.add(batchControlPanel, BorderLayout.WEST);
        southPanel.add(lblProgress, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        // Connect search filter buttons
        btnSearchFilter.addActionListener(e -> onFilterClicked());
        btnResetFilter.addActionListener(e -> {
            txtSearch.setText("");
            refreshEmployeeList();
        });
    }

    private void addFormGridRow(Panel panel, GridBagConstraints gbc, int row, String label, Component comp) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.35;
        Label l = new Label(label, Label.RIGHT);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(l, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        comp.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(comp, gbc);
    }

    private void registerListeners() {
        choType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                typeCardLayout.show(typeFieldsPanel, choType.getSelectedItem());
            }
        });

        employeeList.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                int index = employeeList.getSelectedIndex();
                if (index >= 0 && index < loadedEmployees.size()) {
                    populateForm(loadedEmployees.get(index));
                }
            }
        });

        btnAdd.addActionListener(e -> onAddEmployee());
        btnUpdate.addActionListener(e -> onUpdateEmployee());
        btnDelete.addActionListener(e -> onDeleteEmployee());
        btnCalculate.addActionListener(e -> onCalculateSalary());
        btnClearForm.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> onSearchById());
        btnRunBatch.addActionListener(e -> onRunBatch());
        btnCancelBatch.addActionListener(e -> onCancelBatch());
        btnExportCsv.addActionListener(e -> onExportCsv());
        btnLogout.addActionListener(e -> onLogout());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                batchService.shutdown();
                dispose();
                System.exit(0);
            }
        });
    }

    private void applyRolePermissions() {
        // T21: If HR logs in, Delete is disabled; service layer also enforces ADMIN only
        if (currentUser.getRole() == Role.HR) {
            btnDelete.setEnabled(false);
            btnDelete.setFocusable(false);
        } else {
            btnDelete.setEnabled(true);
        }
    }

    private Employee readForm() throws ValidationException {
        String name = txtName.getText();
        InputValidator.validateName(name);

        double basicSalary = InputValidator.parseNonNegativeMoney(txtBasicSalary.getText(), "Basic Salary");
        String typeStr = choType.getSelectedItem();

        int id = 0;
        if (!txtId.getText().trim().isEmpty()) {
            try {
                id = Integer.parseInt(txtId.getText().trim());
            } catch (NumberFormatException ignored) {}
        }

        if ("PERMANENT".equals(typeStr)) {
            double bonus = 0.0;
            if (!txtBonus.getText().trim().isEmpty()) {
                bonus = InputValidator.parseNonNegativeMoney(txtBonus.getText(), "Bonus");
            }
            return new PermanentEmployee(id, name.trim(), basicSalary, bonus);
        } else {
            double allowance = 0.0;
            if (!txtAllowance.getText().trim().isEmpty()) {
                allowance = InputValidator.parseNonNegativeMoney(txtAllowance.getText(), "Contract Allowance");
            }
            int months = InputValidator.parsePositiveInt(txtMonths.getText(), "Contract Months");
            return new ContractEmployee(id, name.trim(), basicSalary, allowance, months);
        }
    }

    private void populateForm(Employee e) {
        if (e == null) return;
        txtId.setText(String.valueOf(e.getEmployeeId()));
        txtName.setText(e.getName());
        txtBasicSalary.setText(String.format("%.2f", e.getBasicSalary()));

        if (e instanceof PermanentEmployee pe) {
            choType.select("PERMANENT");
            typeCardLayout.show(typeFieldsPanel, "PERMANENT");
            txtBonus.setText(String.format("%.2f", pe.getBonus()));
            txtAllowance.setText("");
            txtMonths.setText("");
        } else if (e instanceof ContractEmployee ce) {
            choType.select("CONTRACT");
            typeCardLayout.show(typeFieldsPanel, "CONTRACT");
            txtAllowance.setText(String.format("%.2f", ce.getContractAllowance()));
            txtMonths.setText(String.valueOf(ce.getContractMonths()));
            txtBonus.setText("");
        }
    }

    private void clearForm() {
        txtId.setText("");
        txtName.setText("");
        txtBasicSalary.setText("");
        txtBonus.setText("");
        txtAllowance.setText("");
        txtMonths.setText("");
        choType.select("PERMANENT");
        typeCardLayout.show(typeFieldsPanel, "PERMANENT");
        employeeList.deselect(employeeList.getSelectedIndex());
        txtName.requestFocus();
    }

    private void refreshEmployeeList() {
        new Thread(() -> {
            try {
                List<Employee> list = employeeService.listAll();
                EventQueue.invokeLater(() -> {
                    loadedEmployees.clear();
                    loadedEmployees.addAll(list);
                    employeeList.removeAll();
                    for (Employee emp : list) {
                        employeeList.add(formatListRow(emp));
                    }
                    lblProgress.setText("Employees loaded: " + list.size());
                });
            } catch (DataAccessException e) {
                EventQueue.invokeLater(() -> {
                    AlertDialog.show(this, "Database Error", "Failed to load employees: " + e.getUserMessage());
                });
            }
        }, "refresh-emp-worker").start();
    }

    private String formatListRow(Employee emp) {
        if (emp instanceof PermanentEmployee pe) {
            return String.format("ID: %-4d | %-20s | PERMANENT | Basic: %10.2f | Bonus: %10.2f",
                    pe.getEmployeeId(), pe.getName(), pe.getBasicSalary(), pe.getBonus());
        } else if (emp instanceof ContractEmployee ce) {
            return String.format("ID: %-4d | %-20s | CONTRACT  | Basic: %10.2f | Allow: %10.2f | Mo: %2d",
                    ce.getEmployeeId(), ce.getName(), ce.getBasicSalary(), ce.getContractAllowance(), ce.getContractMonths());
        }
        return emp.toString();
    }

    private void onAddEmployee() {
        try {
            Employee emp = readForm();
            new Thread(() -> {
                try {
                    int newId = employeeService.addEmployee(emp);
                    EventQueue.invokeLater(() -> {
                        AlertDialog.show(this, "Success", "Employee added successfully with ID: " + newId);
                        clearForm();
                        refreshEmployeeList();
                    });
                } catch (Exception ex) {
                    EventQueue.invokeLater(() -> {
                        AlertDialog.show(this, "Add Employee Failed", ex.getMessage());
                    });
                }
            }, "add-emp-worker").start();
        } catch (ValidationException e) {
            AlertDialog.show(this, "Validation Error", e.getMessage());
        }
    }

    private void onUpdateEmployee() {
        if (txtId.getText().trim().isEmpty()) {
            AlertDialog.show(this, "Update Error", "Please select an existing employee from the list to update.");
            return;
        }
        try {
            Employee emp = readForm();
            new Thread(() -> {
                try {
                    employeeService.updateEmployee(emp);
                    EventQueue.invokeLater(() -> {
                        AlertDialog.show(this, "Success", "Employee ID " + emp.getEmployeeId() + " updated successfully.");
                        refreshEmployeeList();
                    });
                } catch (Exception ex) {
                    EventQueue.invokeLater(() -> {
                        AlertDialog.show(this, "Update Failed", ex.getMessage());
                    });
                }
            }, "update-emp-worker").start();
        } catch (ValidationException e) {
            AlertDialog.show(this, "Validation Error", e.getMessage());
        }
    }

    private void onDeleteEmployee() {
        String idStr = txtId.getText().trim();
        if (idStr.isEmpty()) {
            AlertDialog.show(this, "Delete Error", "Please select an employee to delete.");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            AlertDialog.show(this, "Invalid ID", "Invalid employee ID selected.");
            return;
        }

        boolean confirm = ConfirmDialog.ask(this, "Confirm Deletion",
                "Are you sure you want to delete employee ID " + id + " (" + txtName.getText() + ")?\nThis action cannot be undone.");
        if (!confirm) {
            return;
        }

        new Thread(() -> {
            try {
                employeeService.deleteEmployee(id);
                EventQueue.invokeLater(() -> {
                    AlertDialog.show(this, "Deleted", "Employee ID " + id + " was deleted.");
                    clearForm();
                    refreshEmployeeList();
                });
            } catch (SecurityException ex) {
                EventQueue.invokeLater(() -> {
                    AlertDialog.show(this, "Access Denied", ex.getMessage());
                });
            } catch (DataAccessException ex) {
                EventQueue.invokeLater(() -> {
                    AlertDialog.show(this, "Delete Failed", ex.getUserMessage());
                });
            }
        }, "delete-emp-worker").start();
    }

    private void onCalculateSalary() {
        String idStr = txtId.getText().trim();
        if (idStr.isEmpty()) {
            // Calculate directly from form inputs if not yet saved to DB
            try {
                Employee tempEmp = readForm();
                double gross = tempEmp.calculateGrossSalary();
                double deductions = tempEmp.calculateDeductions();
                double net = tempEmp.calculateNetSalary();

                String out = String.format("""
                        ----------------------------------------------------
                        CALCULATED PAYROLL (UNSAVED / PREVIEW):
                        Employee:   %s (%s)
                        Basic:      $%,.2f
                        Gross:      $%,.2f
                        Deductions: $%,.2f
                        Net Salary: $%,.2f
                        ----------------------------------------------------
                        """, tempEmp.getName(), tempEmp.getEmployeeType(), tempEmp.getBasicSalary(), gross, deductions, net);
                txtOutput.setText(out);
            } catch (ValidationException e) {
                AlertDialog.show(this, "Calculation Error", e.getMessage());
            }
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            AlertDialog.show(this, "Invalid ID", "Employee ID must be a numeric value.");
            return;
        }

        new Thread(() -> {
            try {
                PayrollResult result = employeeService.calculatePayroll(id);
                EventQueue.invokeLater(() -> {
                    String out = String.format("""
                            ----------------------------------------------------
                            PAYROLL CALCULATION FOR ID: %d
                            Employee Name: %s
                            Gross Salary:  $%,.2f
                            Deductions:    $%,.2f
                            Net Salary:    $%,.2f
                            Calculated At: %s
                            ----------------------------------------------------
                            """,
                            result.getEmployeeId(),
                            result.getEmployeeName(),
                            result.getGrossSalary(),
                            result.getDeductions(),
                            result.getNetSalary(),
                            result.getProcessedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    txtOutput.setText(out);
                });
            } catch (DataAccessException ex) {
                EventQueue.invokeLater(() -> {
                    AlertDialog.show(this, "Error", ex.getUserMessage());
                });
            }
        }, "calc-worker").start();
    }

    private void onSearchById() {
        String input = txtSearch.getText().trim();
        if (input.isEmpty()) {
            input = txtId.getText().trim();
        }

        // T07: Search ID field: rejects non-numeric input cleanly
        int id;
        try {
            id = InputValidator.parsePositiveInt(input, "Search ID");
        } catch (ValidationException e) {
            AlertDialog.show(this, "Validation Error", e.getMessage());
            return;
        }

        final int searchId = id;
        new Thread(() -> {
            try {
                var opt = employeeService.findById(searchId);
                EventQueue.invokeLater(() -> {
                    if (opt.isPresent()) {
                        populateForm(opt.get());
                        txtOutput.setText("Found employee: " + opt.get() + "\n");
                    } else {
                        // T15: Employee not found; friendly dialog
                        AlertDialog.show(this, "Search Result", "Employee with ID " + searchId + " not found.");
                    }
                });
            } catch (DataAccessException ex) {
                EventQueue.invokeLater(() -> {
                    AlertDialog.show(this, "Database Error", ex.getUserMessage());
                });
            }
        }, "search-worker").start();
    }

    private void onFilterClicked() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            refreshEmployeeList();
            return;
        }

        new Thread(() -> {
            try {
                List<Employee> matches = employeeService.searchByName(keyword);
                EventQueue.invokeLater(() -> {
                    loadedEmployees.clear();
                    loadedEmployees.addAll(matches);
                    employeeList.removeAll();
                    for (Employee emp : matches) {
                        employeeList.add(formatListRow(emp));
                    }
                    lblProgress.setText("Search matches found: " + matches.size());
                });
            } catch (DataAccessException e) {
                EventQueue.invokeLater(() -> {
                    AlertDialog.show(this, "Search Error", e.getUserMessage());
                });
            }
        }, "filter-worker").start();
    }

    private void onRunBatch() {
        // T24: Second click guard
        btnRunBatch.setEnabled(false);
        btnCancelBatch.setEnabled(true);
        lblProgress.setText("Starting payroll batch...");
        latestBatchResults.clear();
        txtOutput.setText("=== Starting Payroll Batch Processing ===\n");

        try {
            batchService.start(this);
        } catch (IllegalStateException ex) {
            btnRunBatch.setEnabled(true);
            btnCancelBatch.setEnabled(false);
            AlertDialog.show(this, "Batch In Progress", ex.getMessage());
        }
    }

    private void onCancelBatch() {
        btnCancelBatch.setEnabled(false);
        lblProgress.setText("Cancelling payroll batch...");
        batchService.cancel();
    }

    private void onExportCsv() {
        if (latestBatchResults.isEmpty()) {
            AlertDialog.show(this, "Export", "No batch results to export. Run a payroll batch first.");
            return;
        }

        FileDialog dialog = new FileDialog(this, "Save Payroll Results CSV", FileDialog.SAVE);
        dialog.setFile("payroll_results_" + System.currentTimeMillis() + ".csv");
        dialog.setVisible(true);

        String dir = dialog.getDirectory();
        String file = dialog.getFile();
        if (dir != null && file != null) {
            File targetFile = new File(dir, file);
            try {
                ReportExporter.exportResultsToCsv(targetFile, latestBatchResults);
                AlertDialog.show(this, "Export Successful", "Results exported to:\n" + targetFile.getAbsolutePath());
            } catch (Exception ex) {
                AlertDialog.show(this, "Export Failed", "Error writing file: " + ex.getMessage());
            }
        }
    }

    private void onLogout() {
        SessionContext.clear();
        new LoginFrame(new AuthService(new UserDAOImpl())).setVisible(true);
        dispose();
    }

    // ------------------ PayrollProgressListener Callbacks ------------------

    @Override
    public void onProgress(int processed, int total, PayrollResult latest) {
        EventQueue.invokeLater(() -> {
            lblProgress.setText(String.format("Processed %d / %d employees...", processed, total));
            latestBatchResults.add(latest);
            // Append line to console (limit length if huge)
            if (processed <= 500 || processed % 100 == 0 || processed == total) {
                txtOutput.append(String.format("[%04d/%04d] %s -> Net: $%,.2f\n",
                        processed, total, latest.getEmployeeName(), latest.getNetSalary()));
            }
        });
    }

    @Override
    public void onComplete(BatchSummary summary) {
        EventQueue.invokeLater(() -> {
            this.latestSummary = summary;
            btnRunBatch.setEnabled(true);
            btnCancelBatch.setEnabled(false);
            lblProgress.setText(String.format("Completed: %d total, %d succeeded, %d failed",
                    summary.total(), summary.succeeded(), summary.failed()));

            String report = String.format("""
                    ====================================================
                    BATCH PAYROLL EXECUTION COMPLETED
                    ====================================================
                    Total Processed:  %d
                    Succeeded:        %d
                    Failed:           %d
                    Total Gross:      $%,.2f
                    Total Net:        $%,.2f
                    Duration:         %d ms
                    ====================================================
                    """,
                    summary.total(), summary.succeeded(), summary.failed(),
                    summary.totalGross(), summary.totalNet(), summary.durationMillis());

            txtOutput.append(report);
            AlertDialog.show(this, "Batch Run Succeeded",
                    String.format("Processed %d employees in %d ms.\nTotal Net Salary: $%,.2f",
                            summary.total(), summary.durationMillis(), summary.totalNet()));
        });
    }

    @Override
    public void onCancelled(int processed, int total) {
        EventQueue.invokeLater(() -> {
            btnRunBatch.setEnabled(true);
            btnCancelBatch.setEnabled(false);
            lblProgress.setText("Batch cancelled (" + processed + "/" + total + " processed).");
            txtOutput.append("\n[BATCH CANCELLED BY USER]\n");
            AlertDialog.show(this, "Batch Cancelled",
                    "Payroll batch was cancelled after processing " + processed + " records.");
        });
    }

    @Override
    public void onError(Exception cause) {
        EventQueue.invokeLater(() -> {
            btnRunBatch.setEnabled(true);
            btnCancelBatch.setEnabled(false);
            lblProgress.setText("Batch failed with error: " + cause.getMessage());
            txtOutput.append("\n[BATCH FAILED]: " + cause.getMessage() + "\n");
            AlertDialog.show(this, "Batch Execution Error",
                    "An error occurred while running the batch:\n" + cause.getMessage());
        });
    }
}


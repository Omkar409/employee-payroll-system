package payroll.ui;

import payroll.*;
import payroll.dao.DataAccessException;
import payroll.service.*;
import payroll.service.exception.ValidationException;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Modern Enterprise Employee Payroll Dashboard (Pure Java Swing + Graphics2D).
 * Features:
 * - SRM University AP official branding (Deep Navy #0F294A, Academic Gold #C59B27).
 * - Full-width custom painted header banner with drawn vector crest and user pill badge.
 * - Form card with rounded border, generous padding, and styled input fields (including IFSC Code).
 * - 15 custom color-coded rounded ModernButtons (Primary, Positive, Destructive, Utility).
 * - Styled JTable (32px row height, navy header, alternating rows, row selection to form).
 * - Console output area for payslips, batch logs, settlement statements.
 * - Modern footer status bar with colored status indicator dot and monospace font.
 */
public class PayrollFrame extends JFrame implements ActionListener, ItemListener {

    private static final String DATA_FILE = "data/employees.txt";
    private static final String BACKUP_FILE = "data/employees_backup.txt";
    private static final String REPORT_FILE = "data/payroll_report.txt";

    private final User currentUser;
    private final EmployeeService employeeService;
    private final PayrollBatchService batchService;
    private final PayrollFileService fileService = new PayrollFileService();

    // Form input fields
    private final JTextField idField          = new JTextField(10);
    private final JTextField nameField        = new JTextField(15);
    private final JComboBox<String> typeChoice = new JComboBox<>(new String[]{"Permanent", "Contract"});
    private final JTextField salaryField      = new JTextField(10);
    private final JTextField extraField       = new JTextField(10);
    private final JLabel     extraLabel       = new JLabel("Allowance/Bonus:");
    private final JTextField bankAccountField = new JTextField(15);
    private final JTextField ifscField        = new JTextField(11);

    // Modern Styled JTable
    private DefaultTableModel tableModel;
    private JTable employeeTable;

    // Output console
    private final JTextArea output = new JTextArea(7, 70);

    // Color-Coded Custom Rounded Buttons
    // Primary Actions (Deep Navy #0F294A / White)
    private final ModernButton addBtn        = new ModernButton("Add", ModernButton.ButtonStyle.PRIMARY);
    private final ModernButton calcBtn       = new ModernButton("Calculate", ModernButton.ButtonStyle.PRIMARY);
    private final ModernButton batchBtn      = new ModernButton("Batch Run", ModernButton.ButtonStyle.PRIMARY);

    // Positive / Simulation Actions (Slate Blue #2563EB / White)
    private final ModernButton updateBtn     = new ModernButton("Update Salary", ModernButton.ButtonStyle.POSITIVE);
    private final ModernButton simBtn        = new ModernButton("Payment Simulation", ModernButton.ButtonStyle.POSITIVE);

    // Destructive Actions (Crimson Red #DC2626 / White)
    private final ModernButton deleteBtn     = new ModernButton("Delete", ModernButton.ButtonStyle.DESTRUCTIVE);

    // Utility Actions (Clean Light Gray #E2E8F0 / Dark Navy)
    private final ModernButton searchBtn     = new ModernButton("Search ID", ModernButton.ButtonStyle.UTILITY);
    private final ModernButton saveFileBtn   = new ModernButton("Save Records", ModernButton.ButtonStyle.UTILITY);
    private final ModernButton exportRepBtn  = new ModernButton("Export Report", ModernButton.ButtonStyle.UTILITY);
    private final ModernButton clearBtn      = new ModernButton("Clear", ModernButton.ButtonStyle.UTILITY);

    // Status bar components
    private JLabel statusText;
    private StatusDotPanel statusDot;

    public PayrollFrame(User currentUser, EmployeeService employeeService, PayrollBatchService batchService) {
        super("SRM University AP - Employee Payroll Management System");
        this.currentUser = currentUser;
        this.employeeService = employeeService;
        this.batchService = batchService;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        buildUI();
        applyRoleRestrictions();

        setSize(1140, 820);
        setMinimumSize(new Dimension(980, 700));
        setLocationRelativeTo(null);

        // Load initial records into the modern JTable
        refreshTableData();

        logWelcomeMessage();
    }

    private void buildUI() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(0, 0));
        contentPane.setBackground(UITheme.BG_OFFWHITE);

        // =========================================================================
        // 1. TOP HEADER BANNER (Full-width Navy Blue #0F294A with Drawn Emblem & Pill)
        // =========================================================================
        JPanel headerPanel = new HeaderBannerPanel();
        contentPane.add(headerPanel, BorderLayout.NORTH);

        // Main content wrapper
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(UITheme.BG_OFFWHITE);
        centerPanel.setBorder(new EmptyBorder(10, 14, 10, 14));

        // =========================================================================
        // 2. INPUT FORM CARD (White Rounded Card with Generous Padding)
        // =========================================================================
        JPanel formCard = new RoundedCardPanel(12);
        formCard.setLayout(new BorderLayout(0, 8));
        formCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Form Fields (Grid layout for responsive inputs)
        JPanel gridFields = new JPanel(new GridBagLayout());
        gridFields.setBackground(UITheme.CARD_WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        styleTextField(idField);
        styleTextField(nameField);
        styleTextField(salaryField);
        styleTextField(extraField);
        styleTextField(bankAccountField);
        styleTextField(ifscField);

        typeChoice.setFont(UITheme.FONT_REGULAR_13);
        typeChoice.setBackground(Color.WHITE);
        typeChoice.addItemListener(this);
        extraField.setEnabled(false);

        // Row 0
        gbc.gridy = 0;
        gbc.gridx = 0; gbc.weightx = 0.1; gridFields.add(createFieldLabel("Employee ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.23; gridFields.add(idField, gbc);
        gbc.gridx = 2; gbc.weightx = 0.1; gridFields.add(createFieldLabel("Full Name:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.23; gridFields.add(nameField, gbc);
        gbc.gridx = 4; gbc.weightx = 0.1; gridFields.add(createFieldLabel("Category:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.24; gridFields.add(typeChoice, gbc);

        // Row 1
        gbc.gridy = 1;
        gbc.gridx = 0; gbc.weightx = 0.1; gridFields.add(createFieldLabel("Basic Salary (Rs.):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.23; gridFields.add(salaryField, gbc);
        gbc.gridx = 2; gbc.weightx = 0.1; gridFields.add(extraLabel, gbc);
        gbc.gridx = 3; gbc.weightx = 0.23; gridFields.add(extraField, gbc);
        gbc.gridx = 4; gbc.weightx = 0.1; gridFields.add(createFieldLabel("Bank Account No:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.24; gridFields.add(bankAccountField, gbc);

        // Row 2: IFSC Code (Requested by user below Bank Account No)
        gbc.gridy = 2;
        gbc.gridx = 0; gbc.weightx = 0.1; gridFields.add(createFieldLabel("Bank IFSC Code:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.23; gridFields.add(ifscField, gbc);
        gbc.gridx = 2; gbc.gridwidth = 4; gbc.weightx = 0.67;
        JLabel hintLabel = new JLabel("Click on any row in the table below to load an employee. Leave account/IFSC blank to auto-generate.");
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLabel.setForeground(UITheme.TEXT_MUTED);
        gridFields.add(hintLabel, gbc);
        gbc.gridwidth = 1;

        formCard.add(gridFields, BorderLayout.CENTER);

        // =========================================================================
        // 3. COLOR-CODED BUTTON TOOLBARS (10 Buttons Grouped into 2 Clean Rows)
        // =========================================================================
        JPanel buttonsContainer = new JPanel(new GridLayout(2, 1, 4, 6));
        buttonsContainer.setBackground(UITheme.CARD_WHITE);

        // Button Row 1: Direct Employee Record Operations (5 Buttons)
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        row1.setBackground(UITheme.CARD_WHITE);
        row1.add(addBtn);
        row1.add(updateBtn);
        row1.add(calcBtn);
        row1.add(searchBtn);
        row1.add(deleteBtn);

        // Button Row 2: Batch Processing, Simulation, File I/O & Reset (5 Buttons)
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        row2.setBackground(UITheme.CARD_WHITE);
        row2.add(batchBtn);
        row2.add(simBtn);
        row2.add(saveFileBtn);
        row2.add(exportRepBtn);
        row2.add(clearBtn);

        buttonsContainer.add(row1);
        buttonsContainer.add(row2);
        formCard.add(buttonsContainer, BorderLayout.SOUTH);

        centerPanel.add(formCard, BorderLayout.NORTH);

        // =========================================================================
        // 4. CENTRAL MODERN JTABLE & AUDIT CONSOLE (Split View)
        // =========================================================================
        // Table Model Setup
        String[] columns = {
            "ID", "Employee Name", "Category", "Bank Account", "IFSC Code",
            "Basic Salary (Rs.)", "Bonus / Allowance", "Gross (Rs.)", "Deductions (Rs.)", "Net Salary (Rs.)"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        employeeTable = new JTable(tableModel);
        employeeTable.setRowHeight(32);
        employeeTable.setFont(UITheme.FONT_REGULAR_13);
        employeeTable.setShowVerticalLines(false);
        employeeTable.setShowHorizontalLines(true);
        employeeTable.setGridColor(new Color(0xE2, 0xE8, 0xF0));
        employeeTable.setSelectionBackground(UITheme.TABLE_SELECTION);
        employeeTable.setSelectionForeground(UITheme.NAVY_PRIMARY);
        employeeTable.setFillsViewportHeight(true);

        // Header Styling
        JTableHeader tableHeader = employeeTable.getTableHeader();
        tableHeader.setPreferredSize(new Dimension(0, 34));
        tableHeader.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setBackground(UITheme.NAVY_PRIMARY);
                l.setForeground(Color.WHITE);
                l.setFont(UITheme.FONT_BOLD_13);
                l.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });

        // Alternating row striping cell renderer
        employeeTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (isSelected) {
                    c.setBackground(UITheme.TABLE_SELECTION);
                    c.setForeground(UITheme.NAVY_PRIMARY);
                    c.setFont(UITheme.FONT_BOLD_13);
                } else {
                    c.setBackground(row % 2 == 0 ? UITheme.CARD_WHITE : UITheme.TABLE_STRIPE);
                    c.setForeground(UITheme.TEXT_DARK);
                    c.setFont(UITheme.FONT_REGULAR_13);
                }
                return c;
            }
        });

        // Table Selection Listener: Clicking a row loads it into form
        employeeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && employeeTable.getSelectedRow() != -1) {
                int selectedRow = employeeTable.getSelectedRow();
                loadTableRowIntoForm(selectedRow);
            }
        });

        JScrollPane tableScroll = new JScrollPane(employeeTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_SUBTLE, 1));
        tableScroll.getViewport().setBackground(Color.WHITE);

        // Output Console Setup
        output.setEditable(false);
        output.setFont(UITheme.FONT_MONO_12);
        output.setBackground(Color.WHITE);
        output.setForeground(UITheme.TEXT_DARK);
        output.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane consoleScroll = new JScrollPane(output);
        consoleScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_SUBTLE, 1),
                " System Audit & Calculation Output ",
                0, 0, UITheme.FONT_BOLD_13, UITheme.NAVY_PRIMARY));

        // Vertical Split: Table (60%) + Console (40%)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, consoleScroll);
        splitPane.setResizeWeight(0.62);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);

        centerPanel.add(splitPane, BorderLayout.CENTER);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        // =========================================================================
        // 5. MODERN STATUS BAR (Footer with Indicator Dot & Monospace Font)
        // =========================================================================
        JPanel footerBar = new JPanel(new BorderLayout());
        footerBar.setPreferredSize(new Dimension(0, 28));
        footerBar.setBackground(Color.WHITE);
        footerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)));

        JPanel leftStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        leftStatus.setBackground(Color.WHITE);

        statusDot = new StatusDotPanel(UITheme.STATUS_GREEN);
        statusText = new JLabel("System Ready. Connected to MySQL database.");
        statusText.setFont(UITheme.FONT_MONO_11);
        statusText.setForeground(UITheme.TEXT_MUTED);

        leftStatus.add(statusDot);
        leftStatus.add(statusText);

        JLabel rightInfo = new JLabel("SRM AP • Enterprise Payroll v2.4 • PBKDF2 Secure");
        rightInfo.setFont(UITheme.FONT_MONO_11);
        rightInfo.setForeground(UITheme.TEXT_MUTED);

        footerBar.add(leftStatus, BorderLayout.WEST);
        footerBar.add(rightInfo, BorderLayout.EAST);

        contentPane.add(footerBar, BorderLayout.SOUTH);

        // =========================================================================
        // 6. ACTION LISTENERS
        // =========================================================================
        ModernButton[] allButtons = {
            addBtn, updateBtn, calcBtn, searchBtn, deleteBtn,
            batchBtn, simBtn, saveFileBtn, exportRepBtn, clearBtn
        };
        for (ModernButton b : allButtons) {
            b.addActionListener(this);
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                batchService.shutdown();
                SessionContext.clear();
                dispose();
                System.exit(0);
            }
        });
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(UITheme.FONT_REGULAR_13);
        tf.setBackground(Color.WHITE);
        tf.setForeground(UITheme.TEXT_DARK);
        tf.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_SUBTLE, 1, true),
                new EmptyBorder(5, 8, 5, 8)
        ));
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setFont(UITheme.FONT_REGULAR_13);
        label.setForeground(UITheme.TEXT_DARK);
        return label;
    }

    private void applyRoleRestrictions() {
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        deleteBtn.setEnabled(isAdmin);
        batchBtn.setEnabled(isAdmin);
        simBtn.setEnabled(isAdmin);
    }

    private void updateStatus(String message, Color dotColor) {
        SwingUtilities.invokeLater(() -> {
            statusText.setText(message);
            statusDot.setColor(dotColor);
            statusDot.repaint();
        });
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        String selected = (String) typeChoice.getSelectedItem();
        if ("Contract".equalsIgnoreCase(selected)) {
            extraLabel.setText("Allowance (Rs.):");
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
        else if (src == clearBtn)       clearForm();
        else if (src == batchBtn)       startBatch();
        else if (src == simBtn)         startPaymentSimulation();
        else if (src == saveFileBtn)    saveRecordsToFile();
        else if (src == exportRepBtn)   exportReportToFile();
    }

    // =========================================================================
    // CRUD & DATABASE OPERATIONS
    // =========================================================================

    private void addEmployee() {
        try {
            int id = Integer.parseInt(idField.getText().trim());
            String name = nameField.getText().trim();
            if (name.isEmpty()) { output.setText("Validation Error: Employee Name is required."); return; }
            double salary = Double.parseDouble(salaryField.getText().trim());
            String type = (String) typeChoice.getSelectedItem();

            Employee emp;
            if ("Contract".equalsIgnoreCase(type)) {
                double allowance = extraField.getText().trim().isEmpty() ? 0 : Double.parseDouble(extraField.getText().trim());
                emp = new ContractEmployee(id, name, salary, allowance, 12);
            } else {
                double bonus = extraField.getText().trim().isEmpty() ? 0 : Double.parseDouble(extraField.getText().trim());
                emp = new PermanentEmployee(id, name, salary, bonus);
            }
            emp.setEmployeeId(0); // auto assigned by DB
            int generatedId = employeeService.addEmployee(emp);

            String bankAcc = bankAccountField.getText().trim();
            if (bankAcc.isEmpty()) bankAcc = BankAccountStore.generateDefaultAccount(generatedId);
            emp.setBankAccount(bankAcc);
            BankAccountStore.setAccount(generatedId, bankAcc);

            String ifsc = ifscField.getText().trim();
            if (ifsc.isEmpty()) ifsc = BankAccountStore.generateDefaultIfsc(bankAcc);
            emp.setIfscCode(ifsc);
            BankAccountStore.setIfsc(generatedId, ifsc);

            output.setText("SUCCESS: Added employee to database!\n"
                    + "Generated ID:   " + generatedId + "\n"
                    + "Name:           " + name + "\n"
                    + "Category:       " + type + "\n"
                    + "Basic Salary:   Rs. " + String.format("%,.2f", salary) + "\n"
                    + "Bank Account:   " + bankAcc + "\n"
                    + "IFSC Code:      " + ifsc + "\n");

            updateStatus("Employee added successfully (DB ID: " + generatedId + ")", UITheme.STATUS_GREEN);
            refreshTableData();
            clearForm();
        } catch (NumberFormatException ex) {
            output.setText("Input Error: Valid numbers required for ID, Salary, and Allowance/Bonus.");
            updateStatus("Input Error: numeric parsing failed", UITheme.STATUS_RED);
        } catch (ValidationException | DataAccessException ex) {
            output.setText("Database Error: " + ex.getMessage());
            updateStatus("Error: " + ex.getMessage(), UITheme.STATUS_RED);
        }
    }

    private void updateEmployee() {
        try {
            String idStr = idField.getText().trim();
            if (idStr.isEmpty()) {
                output.setText("Error: Enter or select an Employee ID to update.");
                return;
            }
            int id = Integer.parseInt(idStr);
            String name = nameField.getText().trim();
            if (name.isEmpty()) { output.setText("Error: Name cannot be empty."); return; }
            double salary = Double.parseDouble(salaryField.getText().trim());
            String type = (String) typeChoice.getSelectedItem();

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

            String ifsc = ifscField.getText().trim();
            if (!ifsc.isEmpty()) {
                emp.setIfscCode(ifsc);
                BankAccountStore.setIfsc(id, ifsc);
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
            sb.append(String.format("IFSC Code:       %s\n", emp.getIfscCode()));
            sb.append("---------------------------------------------------------\n");
            sb.append(String.format("Recalculated Net:Rs. %,.2f\n", r.getNetSalary()));
            sb.append("=========================================================\n");

            output.setText(sb.toString());
            updateStatus("Employee ID " + id + " updated successfully.", UITheme.STATUS_GREEN);
            refreshTableData();
        } catch (NumberFormatException ex) {
            output.setText("Input Error: Valid numbers required for ID, Salary, and Allowance/Bonus.");
            updateStatus("Update failed: invalid number input", UITheme.STATUS_RED);
        } catch (ValidationException | DataAccessException ex) {
            output.setText("Update Error: " + ex.getMessage());
            updateStatus("Update Error: " + ex.getMessage(), UITheme.STATUS_RED);
            AlertDialog.show(this, "Update Error", ex.getMessage());
        }
    }

    private void calculatePayroll() {
        try {
            String idStr = idField.getText().trim();
            if (idStr.isEmpty()) {
                output.setText("Select an employee or enter Employee ID to calculate payroll.");
                return;
            }
            int id = Integer.parseInt(idStr);
            PayrollResult r = employeeService.calculatePayroll(id);
            output.setText(formatPayrollResult(r));
            updateStatus("Payroll calculated for Employee ID: " + id, UITheme.STATUS_GREEN);
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
                    populateForm(emp);
                    highlightRowInTable(emp.getEmployeeId());
                    output.setText("Found: " + emp);
                    updateStatus("Found employee ID: " + id, UITheme.STATUS_GREEN);
                }, () -> {
                    output.setText("No employee found with ID: " + idStr);
                    updateStatus("Employee ID " + idStr + " not found.", UITheme.STATUS_AMBER);
                });
            } else {
                String name = nameField.getText().trim();
                if (!name.isEmpty()) {
                    List<Employee> results = employeeService.searchByName(name);
                    if (results.isEmpty()) {
                        output.setText("No employees found matching: " + name);
                        updateStatus("No matches for: " + name, UITheme.STATUS_AMBER);
                        return;
                    }
                    displayEmployeesInTable(results);
                    populateForm(results.get(0));
                    output.setText("Found " + results.size() + " matching employee(s). First match loaded into form.");
                    updateStatus("Found " + results.size() + " search results.", UITheme.STATUS_GREEN);
                } else {
                    output.setText("Enter an Employee ID or Name to search.");
                }
            }
        } catch (NumberFormatException e) {
            output.setText("Error: Employee ID must be a number.");
        } catch (DataAccessException e) {
            output.setText("Search Error: " + e.getMessage());
        }
    }

    private void deleteEmployee() {
        try {
            String idStr = idField.getText().trim();
            if (idStr.isEmpty()) { output.setText("Enter or select Employee ID to delete."); return; }
            int id = Integer.parseInt(idStr);
            boolean confirm = ConfirmDialog.ask(this, "Confirm Delete",
                    "Delete employee with ID " + id + "?\nThis action cannot be undone.");
            if (!confirm) { output.setText("Delete cancelled."); return; }

            employeeService.deleteEmployee(id);
            BankAccountStore.removeAccount(id);
            output.setText("SUCCESS: Employee ID " + id + " deleted from database.");
            updateStatus("Employee ID " + id + " deleted.", UITheme.STATUS_GREEN);
            clearForm();
            refreshTableData();
        } catch (NumberFormatException e) {
            output.setText("Error: Employee ID must be a number.");
        } catch (SecurityException e) {
            output.setText("Access Denied: " + e.getMessage());
        } catch (DataAccessException e) {
            output.setText("Delete Error: " + e.getMessage());
        }
    }

    // =========================================================================
    // JTABLE POPULATION & SYNC
    // =========================================================================

    private void refreshTableData() {
        try {
            List<Employee> all = employeeService.listAll();
            displayEmployeesInTable(all);
            updateStatus("Displaying " + all.size() + " employee records from database.", UITheme.STATUS_GREEN);
        } catch (DataAccessException e) {
            output.setText("Database Connection Error loading employees: " + e.getMessage());
            updateStatus("Database error loading table", UITheme.STATUS_RED);
        }
    }

    private void displayEmployeesInTable(List<Employee> list) {
        tableModel.setRowCount(0);
        StandardPayrollCalculator calc = new StandardPayrollCalculator();

        for (Employee emp : list) {
            PayrollResult r = calc.calculate(emp);
            String extraStr;
            if (emp instanceof PermanentEmployee pe) {
                extraStr = String.format("Bonus: %,.2f", pe.getBonus());
            } else if (emp instanceof ContractEmployee ce) {
                extraStr = String.format("Allow: %,.2f", ce.getContractAllowance());
            } else {
                extraStr = "0.00";
            }

            tableModel.addRow(new Object[]{
                emp.getEmployeeId(),
                emp.getName(),
                emp.getEmployeeType().name(),
                emp.getBankAccount(),
                emp.getIfscCode(),
                String.format("%,.2f", emp.getBasicSalary()),
                extraStr,
                String.format("%,.2f", r.getGrossSalary()),
                String.format("%,.2f", r.getDeductions()),
                String.format("%,.2f", r.getNetSalary())
            });
        }
    }

    private void loadTableRowIntoForm(int row) {
        try {
            idField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
            nameField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
            String category = String.valueOf(tableModel.getValueAt(row, 2));
            typeChoice.setSelectedItem("CONTRACT".equalsIgnoreCase(category) ? "Contract" : "Permanent");
            bankAccountField.setText(String.valueOf(tableModel.getValueAt(row, 3)));
            ifscField.setText(String.valueOf(tableModel.getValueAt(row, 4)));

            String basicStr = String.valueOf(tableModel.getValueAt(row, 5)).replace(",", "");
            salaryField.setText(basicStr);

            String extraStr = String.valueOf(tableModel.getValueAt(row, 6));
            if (extraStr.contains(":")) {
                extraStr = extraStr.substring(extraStr.indexOf(':') + 1).trim().replace(",", "");
            }
            extraField.setEnabled(true);
            extraField.setText(extraStr);
        } catch (Exception ignored) {}
    }

    private void populateForm(Employee emp) {
        idField.setText(String.valueOf(emp.getEmployeeId()));
        nameField.setText(emp.getName());
        salaryField.setText(String.format("%.2f", emp.getBasicSalary()));
        bankAccountField.setText(emp.getBankAccount());
        ifscField.setText(emp.getIfscCode());

        if (emp instanceof ContractEmployee ce) {
            typeChoice.setSelectedItem("Contract");
            extraField.setEnabled(true);
            extraField.setText(String.format("%.2f", ce.getContractAllowance()));
        } else if (emp instanceof PermanentEmployee pe) {
            typeChoice.setSelectedItem("Permanent");
            extraField.setEnabled(true);
            extraField.setText(String.format("%.2f", pe.getBonus()));
        }
    }

    private void highlightRowInTable(int employeeId) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((int) tableModel.getValueAt(i, 0) == employeeId) {
                employeeTable.setRowSelectionInterval(i, i);
                employeeTable.scrollRectToVisible(employeeTable.getCellRect(i, 0, true));
                break;
            }
        }
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        salaryField.setText("");
        extraField.setText("");
        bankAccountField.setText("");
        ifscField.setText("");
        employeeTable.clearSelection();
        updateStatus("Form fields cleared.", UITheme.STATUS_GREEN);
    }

    // =========================================================================
    // MULTITHREADED BATCH PAYROLL EXECUTION
    // =========================================================================

    private void startBatch() {
        if (batchService.isRunning()) {
            output.setText("A batch run is already in progress. Please wait for completion.");
            return;
        }
        output.setText("Starting multithreaded batch payroll run on background thread (payroll-batch)...\n");
        batchBtn.setEnabled(false);
        updateStatus("Batch payroll run in progress...", UITheme.STATUS_AMBER);

        batchService.start(new PayrollProgressListener() {
            @Override
            public void onProgress(int processed, int total, PayrollResult latest) {
                SwingUtilities.invokeLater(() -> {
                    updateStatus(String.format("Batch processing %d/%d | %s", processed, total, latest.toString()),
                            UITheme.STATUS_AMBER);
                    output.append("  " + latest + "\n");
                });
            }

            @Override
            public void onComplete(BatchSummary summary) {
                SwingUtilities.invokeLater(() -> {
                    output.append("\n=== MULTITHREADED BATCH COMPLETE ===\n");
                    output.append("Processed: " + summary.total() + " employees | Net Payout: Rs. "
                            + String.format("%,.2f", summary.totalNet())
                            + " | Duration: " + summary.durationMillis() + " ms\n");
                    updateStatus("Batch complete. Run ID: " + batchService.getLastRunId(), UITheme.STATUS_GREEN);
                    batchBtn.setEnabled(currentUser.getRole() == Role.ADMIN);
                    refreshTableData();
                });
            }

            @Override
            public void onCancelled(int processed, int total) {
                SwingUtilities.invokeLater(() -> {
                    output.append("\nBatch cancelled after " + processed + "/" + total + " records.\n");
                    updateStatus("Batch run cancelled by user.", UITheme.STATUS_RED);
                    batchBtn.setEnabled(currentUser.getRole() == Role.ADMIN);
                });
            }

            @Override
            public void onError(Exception cause) {
                SwingUtilities.invokeLater(() -> {
                    output.append("\nBatch Execution ERROR: " + cause.getMessage() + "\n");
                    updateStatus("Batch execution error: " + cause.getMessage(), UITheme.STATUS_RED);
                    batchBtn.setEnabled(currentUser.getRole() == Role.ADMIN);
                    AlertDialog.show(PayrollFrame.this, "Batch Error", cause.getMessage());
                });
            }
        });
    }

    // =========================================================================
    // AUTOMATED BANK PAYMENT SIMULATION ENGINE
    // =========================================================================

    private void startPaymentSimulation() {
        if (batchService.isRunning()) {
            output.setText("A batch process is currently running. Please wait for it to complete.");
            return;
        }

        try {
            List<Employee> all = employeeService.listAll();
            if (all.isEmpty()) {
                output.setText("No employees found in database. Add employees or click 'Add 1000 People' first.");
                return;
            }

            boolean confirm = ConfirmDialog.ask(this, "Bank Salary Disbursement Simulation",
                    "Simulate bulk direct bank deposits for " + all.size() + " employees\n"
                    + "from Company Treasury Account (SRM-CORP-TREASURY-001)?\n\n"
                    + "[Note: Simulation mode - no actual currency transferred]");
            if (!confirm) {
                output.setText("Payment simulation cancelled.");
                return;
            }

            output.setText("========================================================================================\n");
            output.append("INITIALIZING AUTOMATED CLEARING HOUSE (ACH / NEFT) BANKING GATEWAY SIMULATION...\n");
            output.append("DEBIT ACCOUNT: State Bank of India | SRM-CORP-TREASURY-001\n");
            output.append("[NOTE: SIMULATION MODE - NO ACTUAL CURRENCY TRANSFERRED - DEMO PURPOSES ONLY]\n");
            output.append("========================================================================================\n\n");

            simBtn.setEnabled(false);
            updateStatus("Running Payment Simulation for " + all.size() + " employees...", UITheme.STATUS_AMBER);

            new Thread(() -> {
                try {
                    PaymentSimulationService simService = new PaymentSimulationService();
                    simService.simulatePayout(all, new StandardPayrollCalculator(), new PaymentSimulationListener() {
                        @Override
                        public void onProgress(int processed, int total, DisbursementRecord record) {
                            SwingUtilities.invokeLater(() -> {
                                updateStatus(String.format("Disbursing %d/%d: %s -> A/C %s (%s)",
                                        processed, total, record.employeeName(), record.bankAccount(), record.ifscCode()),
                                        UITheme.STATUS_AMBER);
                                if (total <= 50 || processed % 10 == 0 || processed == total) {
                                    output.append(String.format("  [PAID] #%-5d %-18s -> A/C %-14s | %-11s | Net: Rs. %9.2f | %s\n",
                                            record.employeeId(),
                                            record.employeeName().length() > 18 ? record.employeeName().substring(0, 15) + "..." : record.employeeName(),
                                            record.bankAccount(),
                                            record.ifscCode(),
                                            record.netAmount(),
                                            record.utr()));
                                }
                            });
                        }

                        @Override
                        public void onComplete(PaymentSimulationSummary summary) {
                            SwingUtilities.invokeLater(() -> {
                                output.append("\n" + summary.formatSummaryReport());
                                updateStatus("Disbursement completed: " + summary.totalEmployees() + " employees settled.",
                                        UITheme.STATUS_GREEN);
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
                            SwingUtilities.invokeLater(() -> {
                                output.append("\nSimulation ERROR: " + cause.getMessage() + "\n");
                                updateStatus("Payment simulation failed: " + cause.getMessage(), UITheme.STATUS_RED);
                                simBtn.setEnabled(currentUser.getRole() == Role.ADMIN);
                                AlertDialog.show(PayrollFrame.this, "Simulation Error", cause.getMessage());
                            });
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        output.append("Simulation error: " + ex.getMessage() + "\n");
                        simBtn.setEnabled(currentUser.getRole() == Role.ADMIN);
                    });
                }
            }, "payment-simulation-thread").start();

        } catch (DataAccessException ex) {
            output.setText("Database error loading employees: " + ex.getMessage());
        }
    }

    // =========================================================================
    // BULK 1000 EMPLOYEES SEEDING
    // =========================================================================

    private void add1000Employees() {
        if (!ConfirmDialog.ask(this, "Confirm Bulk Addition",
                "Are you sure you want to generate and insert 1,000 employees into MySQL?")) {
            return;
        }

        output.setText("Generating 1,000 employees with diverse credentials, bank accounts, and IFSC codes...\n");
        updateStatus("Inserting 1,000 employees into MySQL in batch...", UITheme.STATUS_AMBER);

        new Thread(() -> {
            try {
                long start = System.currentTimeMillis();
                List<Employee> batch = generate1000Employees();
                employeeService.addBatch(batch);
                long duration = System.currentTimeMillis() - start;
                int totalCount = employeeService.count();

                SwingUtilities.invokeLater(() -> {
                    output.append("=========================================================\n");
                    output.append("SUCCESS: Added 1,000 employees in " + duration + " ms!\n");
                    output.append("500 Permanent Employees (with bonuses & IFSC codes)\n");
                    output.append("500 Contract Employees (with allowances & terms)\n");
                    output.append("Total employees in MySQL database now: " + totalCount + "\n");
                    output.append("=========================================================\n");
                    output.append("Tip: Click 'Batch Run' or 'Payment Simulation' to disburse salaries!\n");
                    updateStatus("1,000 employees inserted successfully in " + duration + " ms.", UITheme.STATUS_GREEN);
                    refreshTableData();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    output.append("ERROR inserting 1,000 employees: " + ex.getMessage() + "\n");
                    updateStatus("Bulk insertion failed.", UITheme.STATUS_RED);
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
        String[] bankPrefixes = {"SBIN", "HDFC", "ICIC", "PUNB", "BARB", "UBIN"};

        List<Employee> list = new ArrayList<>(1000);
        Random rand = new Random();

        for (int i = 1; i <= 1000; i++) {
            String fName = firstNames[rand.nextInt(firstNames.length)];
            String lName = lastNames[rand.nextInt(lastNames.length)];
            String fullName = fName + " " + lName + " #" + i;

            double basicSalary = 25000 + (rand.nextInt(250) * 500);

            Employee emp;
            if (i % 2 == 1) {
                double bonus = 3000 + (rand.nextInt(35) * 500);
                emp = new PermanentEmployee(0, fullName, basicSalary, bonus);
            } else {
                double contractAllowance = 2000 + (rand.nextInt(20) * 500);
                int months = 6 + rand.nextInt(19);
                emp = new ContractEmployee(0, fullName, basicSalary, contractAllowance, months);
            }

            String prefix = bankPrefixes[rand.nextInt(bankPrefixes.length)];
            String bankAccount = prefix + String.format("%010d", 1000000 + i);
            emp.setBankAccount(bankAccount);
            emp.setIfscCode(BankAccountStore.generateDefaultIfsc(bankAccount));
            list.add(emp);
        }
        return list;
    }

    // =========================================================================
    // FILE I/O OPERATIONS (Character Streams, Method Overloading, Byte Streams)
    // =========================================================================

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
            sb.append("Format:        CSV (ID, Name, Type, Basic, Bonus, Months, Bank A/C, IFSC)\n");
            sb.append("=========================================================\n");
            output.setText(sb.toString());
            updateStatus("Saved " + all.size() + " records to " + DATA_FILE, UITheme.STATUS_GREEN);
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
            updateStatus("Imported " + loaded.size() + " records from " + DATA_FILE, UITheme.STATUS_GREEN);
            refreshTableData();
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
            updateStatus("Report exported to " + REPORT_FILE, UITheme.STATUS_GREEN);
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
            updateStatus("Backup created at " + BACKUP_FILE, UITheme.STATUS_GREEN);
        } catch (IOException ex) {
            output.setText("I/O Error creating backup: " + ex.getMessage());
            AlertDialog.show(this, "Backup Error", "Failed to backup file: " + ex.getMessage());
        }
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

    private void logWelcomeMessage() {
        output.setText("Welcome to SRM University AP - Employee Payroll Management System\n"
                + "Logged in as: " + currentUser.getFullName() + " | Role: " + currentUser.getRole().name() + "\n"
                + "========================================================================================\n"
                + "• Select any employee row from the directory table above to view or edit details.\n"
                + "• Use the form cards to Add, Update Salary, Search, or Calculate individual payroll.\n"
                + "• Click 'Payment Simulation' to simulate bulk ACH/NEFT banking disbursements.\n"
                + "• Click 'Batch Run' to execute multithreaded database transactions.\n"
                + "• File I/O operations (Save, Load, Export Report, Backup) are available below.\n"
                + "========================================================================================\n");
    }

    // =========================================================================
    // CUSTOM JAVA2D PAINTED COMPONENTS
    // =========================================================================

    /**
     * Top header banner painted with university deep navy, vector crest, and user pill badge.
     */
    private class HeaderBannerPanel extends JPanel {
        HeaderBannerPanel() {
            setPreferredSize(new Dimension(0, 58));
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(0, 16, 0, 16));

            // Right User Pill Badge
            JPanel userBadge = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0x18, 0x3A, 0x64));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                    g2.setColor(UITheme.GOLD_ACCENT);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));

                    // Status dot
                    g2.setColor(UITheme.GOLD_ACCENT);
                    g2.fillOval(10, (getHeight() - 8) / 2, 8, 8);
                    g2.dispose();
                }
            };
            userBadge.setOpaque(false);
            userBadge.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 5));
            userBadge.setBorder(new EmptyBorder(2, 18, 2, 10));

            JLabel userLabel = new JLabel("User: " + currentUser.getUsername() + " (" + currentUser.getRole().name() + ")");
            userLabel.setFont(UITheme.FONT_BUTTON);
            userLabel.setForeground(Color.WHITE);
            userBadge.add(userLabel);

            JPanel rightWrapper = new JPanel(new GridBagLayout());
            rightWrapper.setOpaque(false);
            rightWrapper.add(userBadge);

            add(rightWrapper, BorderLayout.EAST);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Navy Gradient
            GradientPaint gp = new GradientPaint(0, 0, UITheme.NAVY_PRIMARY, w, 0, UITheme.NAVY_DARK);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // Gold bottom accent line (2px)
            g2.setColor(UITheme.GOLD_ACCENT);
            g2.fillRect(0, h - 2, w, 2);

            // Vector Mini Emblem at x=18, y=28
            int cx = 32;
            int cy = h / 2;
            g2.setColor(UITheme.GOLD_ACCENT);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawOval(cx - 15, cy - 15, 30, 30);
            g2.fillOval(cx - 3, cy - 3, 6, 6);

            // Header Typography
            g2.setColor(UITheme.GOLD_ACCENT);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString("SRM UNIVERSITY AP", 56, 23);

            g2.setColor(Color.WHITE);
            g2.setFont(UITheme.FONT_HEADER);
            g2.drawString("Employee Payroll Management System", 56, 44);

            g2.dispose();
        }
    }

    /**
     * Card container panel with smooth rounded corners and subtle border.
     */
    private static class RoundedCardPanel extends JPanel {
        private final int radius;

        RoundedCardPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(UITheme.CARD_WHITE);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));

            g2.setColor(UITheme.BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radius, radius));

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Small colored circle status indicator dot.
     */
    private static class StatusDotPanel extends JPanel {
        private Color dotColor;

        StatusDotPanel(Color color) {
            this.dotColor = color;
            setPreferredSize(new Dimension(10, 10));
            setOpaque(false);
        }

        void setColor(Color c) {
            this.dotColor = c;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(dotColor);
            g2.fillOval(0, 0, 9, 9);
            g2.dispose();
        }
    }
}

package payroll.ui;

import payroll.Role;
import payroll.service.AuthService;
import payroll.service.exception.DuplicateUserException;
import payroll.service.exception.ValidationException;
import payroll.util.InputValidator;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

/**
 * User registration window (pure Java AWT).
 */
public class RegisterFrame extends Frame {

    private final AuthService authService;
    private final Frame parent;

    private TextField txtUsername;
    private TextField txtPassword;
    private TextField txtConfirmPassword;
    private TextField txtFullName;
    private TextField txtEmail;
    private Choice choRole;
    private Button btnRegister;
    private Button btnCancel;
    private Label lblStatus;

    public RegisterFrame(AuthService authService, Frame parent) {
        super("Employee Payroll System - Register User");
        this.authService = authService;
        this.parent = parent;
        buildUI();
        registerListeners();
    }

    private void buildUI() {
        setSize(480, 420);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(15, 15));
        setBackground(SystemColor.control);

        Label lblTitle = new Label("Register New Account", Label.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(lblTitle, BorderLayout.NORTH);

        Panel formPanel = new Panel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 12, 6, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUsername        = new TextField(22);
        txtFullName        = new TextField(22);
        txtEmail           = new TextField(22);
        txtPassword        = new TextField(22); txtPassword.setEchoChar('*');
        txtConfirmPassword = new TextField(22); txtConfirmPassword.setEchoChar('*');
        choRole = new Choice();
        choRole.add("HR");
        choRole.add("ADMIN");

        int row = 0;
        addFormRow(formPanel, gbc, row++, "Username:", txtUsername);
        addFormRow(formPanel, gbc, row++, "Full Name:", txtFullName);
        addFormRow(formPanel, gbc, row++, "Email:", txtEmail);
        addFormRow(formPanel, gbc, row++, "Role:", choRole);
        addFormRow(formPanel, gbc, row++, "Password:", txtPassword);
        addFormRow(formPanel, gbc, row++, "Confirm Password:", txtConfirmPassword);

        lblStatus = new Label(" ", Label.CENTER);
        lblStatus.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        formPanel.add(lblStatus, gbc);

        add(formPanel, BorderLayout.CENTER);

        Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnRegister = new Button("   Register   ");
        btnRegister.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCancel = new Button("   Cancel   ");
        btnCancel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        buttonPanel.add(btnRegister);
        buttonPanel.add(btnCancel);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addFormRow(Panel panel, GridBagConstraints gbc, int row, String labelText, Component field) {
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        Label label = new Label(labelText, Label.RIGHT);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(label, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(field, gbc);
    }

    private void registerListeners() {
        btnRegister.addActionListener(e -> onRegisterClicked());
        btnCancel.addActionListener(e -> dispose());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
    }

    private void onRegisterClicked() {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String email    = txtEmail.getText().trim();
        char[] password = txtPassword.getText().toCharArray();
        char[] confirm  = txtConfirmPassword.getText().toCharArray();
        String roleStr  = choRole.getSelectedItem();

        if (!Arrays.equals(password, confirm)) {
            lblStatus.setText("Passwords do not match.");
            AlertDialog.show(this, "Validation Error", "Passwords do not match. Please re-enter.");
            clearPasswords(password, confirm);
            return;
        }

        btnRegister.setEnabled(false);
        lblStatus.setText("Registering...");
        lblStatus.setForeground(Color.BLUE);

        char[] passwordCopy = Arrays.copyOf(password, password.length);
        Arrays.fill(password, '\0');
        Arrays.fill(confirm, '\0');

        new Thread(() -> {
            try {
                Role role = Role.valueOf(roleStr);
                authService.register(username, passwordCopy, fullName, email, role);
                EventQueue.invokeLater(() -> {
                    AlertDialog.show(this, "Registration Successful",
                            "Account '" + username + "' created successfully!\nYou may now log in.");
                    dispose();
                });
            } catch (ValidationException ex) {
                EventQueue.invokeLater(() -> {
                    lblStatus.setText(ex.getMessage());
                    lblStatus.setForeground(Color.RED);
                    btnRegister.setEnabled(true);
                    AlertDialog.show(RegisterFrame.this, "Validation Error", ex.getMessage());
                });
            } catch (DuplicateUserException ex) {
                EventQueue.invokeLater(() -> {
                    lblStatus.setText(ex.getMessage());
                    lblStatus.setForeground(Color.RED);
                    btnRegister.setEnabled(true);
                    AlertDialog.show(RegisterFrame.this, "Duplicate Account", ex.getMessage());
                });
            } catch (Exception ex) {
                EventQueue.invokeLater(() -> {
                    lblStatus.setText("Registration failed");
                    lblStatus.setForeground(Color.RED);
                    btnRegister.setEnabled(true);
                    AlertDialog.show(RegisterFrame.this, "Error", ex.getMessage());
                });
            } finally {
                Arrays.fill(passwordCopy, '\0');
            }
        }, "register-worker").start();
    }

    private void clearPasswords(char[]... arrays) {
        for (char[] arr : arrays) Arrays.fill(arr, '\0');
        txtPassword.setText("");
        txtConfirmPassword.setText("");
    }
}

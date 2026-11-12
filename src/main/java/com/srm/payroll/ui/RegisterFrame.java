package com.srm.payroll.ui;

import com.srm.payroll.model.Role;
import com.srm.payroll.service.AuthService;
import com.srm.payroll.service.exception.DuplicateUserException;
import com.srm.payroll.service.exception.ValidationException;
import com.srm.payroll.util.InputValidator;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Registration window built strictly using Java AWT.
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

        // Header Title
        Label lblTitle = new Label("Register New Account", Label.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(lblTitle, BorderLayout.NORTH);

        // Form Fields (GridBagLayout)
        Panel formPanel = new Panel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 12, 6, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUsername = new TextField(22);
        txtPassword = new TextField(22);
        txtPassword.setEchoChar('*');
        txtConfirmPassword = new TextField(22);
        txtConfirmPassword.setEchoChar('*');
        txtFullName = new TextField(22);
        txtEmail = new TextField(22);

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
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        formPanel.add(lblStatus, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Action Buttons
        Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnRegister = new Button("   Register   ");
        btnCancel = new Button("   Cancel   ");
        btnRegister.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCancel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        buttonPanel.add(btnRegister);
        buttonPanel.add(btnCancel);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addFormRow(Panel panel, GridBagConstraints gbc, int row, String labelText, Component field) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        Label label = new Label(labelText, Label.RIGHT);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(field, gbc);
    }

    private void registerListeners() {
        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRegisterClicked();
            }
        });

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    private List<String> validateForm() {
        List<String> errors = new ArrayList<>();
        String username = txtUsername.getText();
        try {
            InputValidator.validateUsername(username);
        } catch (ValidationException ex) {
            errors.add(ex.getMessage());
        }

        try {
            InputValidator.validateNonBlank(txtFullName.getText(), "Full Name");
        } catch (ValidationException ex) {
            errors.add(ex.getMessage());
        }

        try {
            InputValidator.validateEmail(txtEmail.getText());
        } catch (ValidationException ex) {
            errors.add(ex.getMessage());
        }

        char[] pwd = txtPassword.getText().toCharArray();
        char[] confirmPwd = txtConfirmPassword.getText().toCharArray();

        if (pwd.length == 0) {
            errors.add("Password cannot be empty.");
        } else if (!Arrays.equals(pwd, confirmPwd)) {
            errors.add("Passwords do not match.");
        } else {
            try {
                InputValidator.validateStrongPassword(pwd);
            } catch (ValidationException ex) {
                errors.add(ex.getMessage());
            }
        }

        return errors;
    }

    private void onRegisterClicked() {
        lblStatus.setText(" ");
        List<String> errors = validateForm();
        if (!errors.isEmpty()) {
            AlertDialog.show(this, "Validation Error", String.join("\n", errors));
            return;
        }

        String username = txtUsername.getText().trim();
        char[] password = txtPassword.getText().toCharArray();
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        Role role = Role.valueOf(choRole.getSelectedItem());

        btnRegister.setEnabled(false);
        lblStatus.setText("Registering user...");
        lblStatus.setForeground(Color.BLUE);

        // Run on worker thread to avoid blocking EDT
        new Thread(() -> {
            try {
                authService.register(username, password, fullName, email, role);
                EventQueue.invokeLater(() -> {
                    AlertDialog.show(this, "Registration Successful",
                            "Account for '" + username + "' (" + role + ") registered successfully!");
                    showSuccessAndClose();
                });
            } catch (DuplicateUserException ex) {
                EventQueue.invokeLater(() -> {
                    btnRegister.setEnabled(true);
                    lblStatus.setText("Registration failed: Duplicate user");
                    lblStatus.setForeground(Color.RED);
                    AlertDialog.show(this, "Registration Failed", ex.getMessage());
                });
            } catch (Exception ex) {
                EventQueue.invokeLater(() -> {
                    btnRegister.setEnabled(true);
                    lblStatus.setText("Error during registration");
                    lblStatus.setForeground(Color.RED);
                    AlertDialog.show(this, "Registration Error", ex.getMessage());
                });
            } finally {
                Arrays.fill(password, '\0');
            }
        }, "register-worker").start();
    }

    private void showSuccessAndClose() {
        dispose();
    }
}

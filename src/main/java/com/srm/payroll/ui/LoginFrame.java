package com.srm.payroll.ui;

import com.srm.payroll.dao.impl.EmployeeDAOImpl;
import com.srm.payroll.dao.impl.PayrollResultDAOImpl;
import com.srm.payroll.model.User;
import com.srm.payroll.service.*;
import com.srm.payroll.service.exception.AuthenticationException;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

/**
 * Authentication login window built strictly with Java AWT.
 */
public class LoginFrame extends Frame {

    private final AuthService authService;

    private TextField txtUsername;
    private TextField txtPassword;
    private Button btnLogin;
    private Button btnRegister;
    private Label lblStatus;

    public LoginFrame(AuthService authService) {
        super("Employee Payroll System - Login");
        this.authService = authService;

        buildUI();
        registerListeners();
    }

    private void buildUI() {
        setSize(420, 290);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        setBackground(SystemColor.control);

        // Header Title
        Label lblTitle = new Label("Payroll Management System", Label.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        add(lblTitle, BorderLayout.NORTH);

        // Center Form Panel (GridBagLayout)
        Panel formPanel = new Panel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUsername = new TextField(20);
        txtUsername.setFont(new Font("SansSerif", Font.PLAIN, 13));

        txtPassword = new TextField(20);
        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtPassword.setEchoChar('*');

        // Username row
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        Label lblUsername = new Label("Username:", Label.RIGHT);
        lblUsername.setFont(new Font("SansSerif", Font.PLAIN, 13));
        formPanel.add(lblUsername, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formPanel.add(txtUsername, gbc);

        // Password row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        Label lblPassword = new Label("Password:", Label.RIGHT);
        lblPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
        formPanel.add(lblPassword, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formPanel.add(txtPassword, gbc);

        // Status Label row
        lblStatus = new Label(" ", Label.CENTER);
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblStatus.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        formPanel.add(lblStatus, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Bottom Button Panel
        Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnLogin = new Button("   Login   ");
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 13));

        btnRegister = new Button("   Register   ");
        btnRegister.setFont(new Font("SansSerif", Font.PLAIN, 13));

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnRegister);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void registerListeners() {
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onLoginClicked();
            }
        });

        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRegisterClicked();
            }
        });

        // Wire Enter key in password field to trigger login
        txtPassword.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onLoginClicked();
            }
        });

        txtUsername.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtPassword.requestFocus();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });
    }

    private void onLoginClicked() {
        String username = txtUsername.getText().trim();
        char[] password = txtPassword.getText().toCharArray();

        if (username.isEmpty() || password.length == 0) {
            lblStatus.setText("Please enter username and password.");
            lblStatus.setForeground(Color.RED);
            AlertDialog.show(this, "Login Failed", "Username and password cannot be empty.");
            return;
        }

        lblStatus.setText("Authenticating...");
        lblStatus.setForeground(Color.BLUE);
        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);

        // Worker thread for database authentication
        new Thread(() -> {
            try {
                User user = authService.login(username, password);
                EventQueue.invokeLater(() -> {
                    SessionContext.setCurrentUser(user);
                    openMainApplication(user);
                });
            } catch (AuthenticationException ex) {
                EventQueue.invokeLater(() -> {
                    lblStatus.setText(ex.getMessage());
                    lblStatus.setForeground(Color.RED);
                    btnLogin.setEnabled(true);
                    btnRegister.setEnabled(true);
                    AlertDialog.show(LoginFrame.this, "Authentication Error", ex.getMessage());
                    clearPassword();
                });
            } catch (Exception ex) {
                EventQueue.invokeLater(() -> {
                    lblStatus.setText("Database connection error");
                    lblStatus.setForeground(Color.RED);
                    btnLogin.setEnabled(true);
                    btnRegister.setEnabled(true);
                    AlertDialog.show(LoginFrame.this, "Connection Failure",
                            "Unable to communicate with the authentication service:\n" + ex.getMessage());
                    clearPassword();
                });
            } finally {
                Arrays.fill(password, '\0');
            }
        }, "auth-worker").start();
    }

    private void onRegisterClicked() {
        RegisterFrame registerFrame = new RegisterFrame(authService, this);
        registerFrame.setVisible(true);
    }

    private void openMainApplication(User user) {
        EmployeeDAOImpl employeeDAO = new EmployeeDAOImpl();
        PayrollResultDAOImpl resultDAO = new PayrollResultDAOImpl();
        StandardPayrollCalculator calculator = new StandardPayrollCalculator();

        EmployeeService employeeService = new EmployeeService(employeeDAO, calculator);
        PayrollBatchService batchService = new PayrollBatchService(employeeDAO, resultDAO, calculator);

        PayrollFrame payrollFrame = new PayrollFrame(user, employeeService, batchService);
        payrollFrame.setVisible(true);
        dispose();
    }

    private void clearPassword() {
        txtPassword.setText("");
        txtPassword.requestFocus();
    }

    public void clearForm() {
        txtUsername.setText("");
        txtPassword.setText("");
        lblStatus.setText(" ");
        txtUsername.requestFocus();
    }
}

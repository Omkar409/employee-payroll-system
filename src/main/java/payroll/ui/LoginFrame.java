package payroll.ui;

import payroll.*;
import payroll.dao.impl.EmployeeDAOImpl;
import payroll.dao.impl.PayrollResultDAOImpl;
import payroll.service.*;
import payroll.service.exception.AuthenticationException;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

/**
 * Login window (pure Java AWT).
 * Authenticates the user against MySQL via AuthService.
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

        Label lblTitle = new Label("Payroll Management System", Label.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        add(lblTitle, BorderLayout.NORTH);

        Panel formPanel = new Panel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUsername = new TextField(20);
        txtUsername.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtPassword = new TextField(20);
        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtPassword.setEchoChar('*');

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        Label lblU = new Label("Username:", Label.RIGHT);
        lblU.setFont(new Font("SansSerif", Font.PLAIN, 13));
        formPanel.add(lblU, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        Label lblP = new Label("Password:", Label.RIGHT);
        lblP.setFont(new Font("SansSerif", Font.PLAIN, 13));
        formPanel.add(lblP, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(txtPassword, gbc);

        lblStatus = new Label(" ", Label.CENTER);
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblStatus.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(lblStatus, gbc);

        add(formPanel, BorderLayout.CENTER);

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
        btnLogin.addActionListener(e -> onLoginClicked());
        btnRegister.addActionListener(e -> onRegisterClicked());
        txtPassword.addActionListener(e -> onLoginClicked());
        txtUsername.addActionListener(e -> txtPassword.requestFocus());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); System.exit(0); }
        });
    }

    private void onLoginClicked() {
        String username = txtUsername.getText().trim();
        char[] password = txtPassword.getText().toCharArray();

        if (username.isEmpty() || password.length == 0) {
            lblStatus.setText("Please enter username and password.");
            AlertDialog.show(this, "Login Failed", "Username and password cannot be empty.");
            return;
        }

        lblStatus.setText("Authenticating...");
        lblStatus.setForeground(Color.BLUE);
        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);

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
                            "Unable to connect to the database:\n" + ex.getMessage());
                    clearPassword();
                });
            } finally {
                Arrays.fill(password, '\0');
            }
        }, "auth-worker").start();
    }

    private void onRegisterClicked() {
        new RegisterFrame(authService, this).setVisible(true);
    }

    private void openMainApplication(User user) {
        EmployeeDAOImpl employeeDAO = new EmployeeDAOImpl();
        PayrollResultDAOImpl resultDAO = new PayrollResultDAOImpl();
        StandardPayrollCalculator calculator = new StandardPayrollCalculator();
        EmployeeService employeeService = new EmployeeService(employeeDAO, calculator);
        PayrollBatchService batchService = new PayrollBatchService(employeeDAO, resultDAO, calculator);
        new PayrollFrame(user, employeeService, batchService).setVisible(true);
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

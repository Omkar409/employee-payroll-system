package com.srm.payroll;

import com.srm.payroll.config.DatabaseConnection;
import com.srm.payroll.dao.impl.UserDAOImpl;
import com.srm.payroll.service.AuthService;
import com.srm.payroll.ui.AlertDialog;
import com.srm.payroll.ui.LoginFrame;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application Entry Point.
 * Initializes core services and launches LoginFrame on the AWT Event Dispatch Thread (EDT).
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        LOGGER.info("Starting Employee Payroll Management System v2...");

        // Pre-flight database connectivity check
        boolean dbConnected = false;
        try {
            dbConnected = DatabaseConnection.getInstance().testConnection();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Startup DB connection check failed: " + e.getMessage());
        }

        final boolean isDbAvailable = dbConnected;

        EventQueue.invokeLater(() -> {
            try {
                UserDAOImpl userDAO = new UserDAOImpl();
                AuthService authService = new AuthService(userDAO);

                LoginFrame loginFrame = new LoginFrame(authService);
                loginFrame.setVisible(true);

                if (!isDbAvailable) {
                    AlertDialog.show(loginFrame, "Database Connection Warning",
                            "Could not establish initial connection to MySQL database.\n" +
                            "Please check that MySQL Server is running and credentials in db.properties are valid.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fatal error launching UI", e);
                AlertDialog.show(null, "Startup Error", "Failed to start application:\n" + e.getMessage());
                System.exit(1);
            }
        });
    }
}


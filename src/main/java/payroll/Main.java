package payroll;

import payroll.config.DatabaseConnection;
import payroll.dao.impl.UserDAOImpl;
import payroll.service.AuthService;
import payroll.ui.AlertDialog;
import payroll.ui.LoginFrame;
import payroll.ui.SplashScreenWindow;

import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application entry point for the Employee Payroll Management System.
 * Displays a 2.5-second modern startup splash screen with vector crest,
 * tests MySQL connectivity, and launches the login window on the EDT.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        LOGGER.info("Starting Employee Payroll Management System...");

        // Launch smooth 2.5-second splash screen with animated progress bar
        SplashScreenWindow.showSplash(() -> {
            boolean dbConnected = false;
            try {
                dbConnected = DatabaseConnection.getInstance().testConnection();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Startup DB connection check failed: " + e.getMessage());
            }

            final boolean isDbAvailable = dbConnected;

            try {
                UserDAOImpl userDAO = new UserDAOImpl();
                AuthService authService = new AuthService(userDAO);

                LoginFrame loginFrame = new LoginFrame(authService);
                loginFrame.setVisible(true);

                if (!isDbAvailable) {
                    AlertDialog.show(loginFrame, "Database Connection Warning",
                            "Could not establish initial connection to MySQL database.\n" +
                            "Please verify MySQL service is running and db.properties has valid credentials.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fatal error launching UI", e);
                AlertDialog.show(null, "Startup Error", "Failed to start application:\n" + e.getMessage());
                System.exit(1);
            }
        });
    }
}

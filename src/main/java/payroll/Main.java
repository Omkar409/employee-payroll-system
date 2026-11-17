package payroll;

import java.awt.EventQueue;

/**
 * Main application entry point for the Employee Payroll Management System.
 * Demonstrates:
 * - Application entry point (main method)
 * - EventQueue.invokeLater() scheduling the GUI on AWT's Event Dispatch Thread (EDT)
 */
public class Main {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new PayrollFrame());
    }
}

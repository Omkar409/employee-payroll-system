package com.srm.payroll.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe singleton factory for obtaining JDBC connections.
 * Loads configuration from db.properties on the classpath.
 */
public final class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String PROPERTIES_FILE = "db.properties";

    private final String url;
    private final String username;
    private final String password;

    private DatabaseConnection() {
        Properties props;
        try {
            props = loadProperties();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load database properties: " + e.getMessage(), e);
            throw new IllegalStateException("Could not load " + PROPERTIES_FILE + ". Ensure it is present in classpath.", e);
        }

        this.url = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");

        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL Driver not found in classpath", e);
            throw new RuntimeException("MySQL JDBC Driver missing. Check lib configuration.", e);
        }
    }

    private static class Holder {
        private static final DatabaseConnection INSTANCE = new DatabaseConnection();
    }

    public static DatabaseConnection getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Returns a new JDBC connection per call.
     * Callers must close the connection using try-with-resources.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Tests the database connection by executing SELECT 1.
     * Returns true if connection succeeds, false otherwise.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.execute("SELECT 1");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Connection test failed: " + e.getMessage());
            return false;
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new IOException("Cannot find " + PROPERTIES_FILE + " in classpath resources.");
            }
            properties.load(in);
        }
        return properties;
    }
}

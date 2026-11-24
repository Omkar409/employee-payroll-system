package payroll.service;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe persistent store for employee bank account numbers.
 * Backed by data/bank_accounts.properties.
 */
public class BankAccountStore {

    private static final String FILE_PATH = "data/bank_accounts.properties";
    private static final Map<Integer, String> ACCOUNTS = new ConcurrentHashMap<>();

    static {
        load();
    }

    public static synchronized void load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;
        try (InputStream in = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(in);
            for (String key : props.stringPropertyNames()) {
                try {
                    int id = Integer.parseInt(key.trim());
                    ACCOUNTS.put(id, props.getProperty(key).trim());
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    public static synchronized void save() {
        File file = new File(FILE_PATH);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Properties props = new Properties();
        for (Map.Entry<Integer, String> entry : ACCOUNTS.entrySet()) {
            props.setProperty(String.valueOf(entry.getKey()), entry.getValue());
        }
        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, "Employee Bank Accounts");
        } catch (IOException ignored) {}
    }

    public static String getAccount(int employeeId) {
        String existing = ACCOUNTS.get(employeeId);
        if (existing != null && !existing.trim().isEmpty()) {
            return existing;
        }
        return generateDefaultAccount(employeeId);
    }

    public static void setAccount(int employeeId, String account) {
        if (employeeId <= 0 || account == null || account.trim().isEmpty()) return;
        ACCOUNTS.put(employeeId, account.trim());
        save();
    }

    public static void removeAccount(int employeeId) {
        if (ACCOUNTS.remove(employeeId) != null) {
            save();
        }
    }

    public static void clear() {
        ACCOUNTS.clear();
        save();
    }

    public static String generateDefaultAccount(int employeeId) {
        return String.format("SBIN%010d", Math.max(1, employeeId));
    }
}

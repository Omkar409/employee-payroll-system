package payroll.service;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe persistent store for employee bank account numbers and IFSC codes.
 * Backed by data/bank_accounts.properties.
 */
public class BankAccountStore {

    private static final String FILE_PATH = "data/bank_accounts.properties";
    private static final Map<Integer, String> ACCOUNTS = new ConcurrentHashMap<>();
    private static final Map<Integer, String> IFSC_CODES = new ConcurrentHashMap<>();

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
                String val = props.getProperty(key).trim();
                if (key.endsWith(".ifsc")) {
                    try {
                        int id = Integer.parseInt(key.substring(0, key.indexOf('.')).trim());
                        IFSC_CODES.put(id, val);
                    } catch (NumberFormatException ignored) {}
                } else if (key.endsWith(".account")) {
                    try {
                        int id = Integer.parseInt(key.substring(0, key.indexOf('.')).trim());
                        ACCOUNTS.put(id, val);
                    } catch (NumberFormatException ignored) {}
                } else {
                    // Legacy plain ID format
                    try {
                        int id = Integer.parseInt(key.trim());
                        ACCOUNTS.put(id, val);
                    } catch (NumberFormatException ignored) {}
                }
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
            props.setProperty(entry.getKey() + ".account", entry.getValue());
        }
        for (Map.Entry<Integer, String> entry : IFSC_CODES.entrySet()) {
            props.setProperty(entry.getKey() + ".ifsc", entry.getValue());
        }
        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, "Employee Bank Accounts and IFSC Codes");
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

    public static String getIfsc(int employeeId) {
        String existing = IFSC_CODES.get(employeeId);
        if (existing != null && !existing.trim().isEmpty()) {
            return existing;
        }
        String acc = getAccount(employeeId);
        return generateDefaultIfsc(acc);
    }

    public static void setIfsc(int employeeId, String ifsc) {
        if (employeeId <= 0 || ifsc == null || ifsc.trim().isEmpty()) return;
        IFSC_CODES.put(employeeId, ifsc.trim().toUpperCase());
        save();
    }

    public static void removeAccount(int employeeId) {
        boolean changed = (ACCOUNTS.remove(employeeId) != null);
        changed |= (IFSC_CODES.remove(employeeId) != null);
        if (changed) {
            save();
        }
    }

    public static void clear() {
        ACCOUNTS.clear();
        IFSC_CODES.clear();
        save();
    }

    public static String generateDefaultAccount(int employeeId) {
        return String.format("SBIN%010d", Math.max(1, employeeId));
    }

    public static String generateDefaultIfsc(String bankAccount) {
        if (bankAccount != null) {
            String upper = bankAccount.toUpperCase();
            if (upper.startsWith("HDFC")) return "HDFC0001234";
            if (upper.startsWith("ICIC")) return "ICIC0000001";
            if (upper.startsWith("PUNB")) return "PUNB0123456";
            if (upper.startsWith("BARB")) return "BARB0AMARAV";
            if (upper.startsWith("UBIN")) return "UBIN0532104";
            if (upper.startsWith("AXIS") || upper.startsWith("UTIB")) return "UTIB0000123";
        }
        return "SBIN0021045"; // Default SRM University AP Campus Branch IFSC
    }
}

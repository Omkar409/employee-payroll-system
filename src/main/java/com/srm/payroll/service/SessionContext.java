package com.srm.payroll.service;

import com.srm.payroll.model.Role;
import com.srm.payroll.model.User;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe static session context holding the currently authenticated user.
 */
public final class SessionContext {

    private static final AtomicReference<User> CURRENT_USER = new AtomicReference<>(null);

    private SessionContext() {
        // Utility class
    }

    public static void setCurrentUser(User u) {
        CURRENT_USER.set(u);
    }

    public static User getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static boolean hasRole(Role r) {
        User u = CURRENT_USER.get();
        return u != null && u.getRole() == r;
    }

    public static void clear() {
        CURRENT_USER.set(null);
    }
}

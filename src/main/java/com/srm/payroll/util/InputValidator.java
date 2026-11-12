package com.srm.payroll.util;

import com.srm.payroll.service.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Validates user input across UI forms and services to prevent invalid data
 * entry and sanitize inputs before domain processing.
 */
public final class InputValidator {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final double MAX_MONEY_AMOUNT = 999_999_999.99; // Within MySQL DECIMAL(12,2)

    private InputValidator() {
        // Utility class
    }

    public static String validateNonBlank(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty.");
        }
        return value.trim();
    }

    public static void validateName(String name) throws ValidationException {
        String trimmed = validateNonBlank(name, "Employee Name");
        if (trimmed.length() > 100) {
            throw new ValidationException("Employee Name cannot exceed 100 characters.");
        }
    }

    public static void validateUsername(String username) throws ValidationException {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty.");
        }
        String trimmed = username.trim();
        if (trimmed.length() < 4) {
            throw new ValidationException("Username must be at least 4 characters long.");
        }
        if (trimmed.length() > 50) {
            throw new ValidationException("Username cannot exceed 50 characters.");
        }
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Username can only contain alphanumeric characters and underscores.");
        }
    }

    public static void validateEmail(String email) throws ValidationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email cannot be empty.");
        }
        String trimmed = email.trim();
        if (trimmed.length() > 100) {
            throw new ValidationException("Email cannot exceed 100 characters.");
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Please enter a valid email address (e.g. name@example.com).");
        }
    }

    public static void validateStrongPassword(char[] password) throws ValidationException {
        if (password == null || password.length == 0) {
            throw new ValidationException("Password cannot be empty.");
        }
        if (password.length < 6) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }
        boolean hasDigit = false;
        boolean hasLetter = false;
        for (char c : password) {
            if (Character.isDigit(c)) hasDigit = true;
            if (Character.isLetter(c)) hasLetter = true;
        }
        if (!hasDigit || !hasLetter) {
            throw new ValidationException("Password must contain at least one letter and one number.");
        }
    }

    public static int parsePositiveInt(String str, String fieldName) throws ValidationException {
        String trimmed = validateNonBlank(str, fieldName);
        try {
            int val = Integer.parseInt(trimmed);
            if (val <= 0) {
                throw new ValidationException(fieldName + " must be a positive integer greater than zero.");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid integer number.");
        }
    }

    public static double parseNonNegativeMoney(String str, String fieldName) throws ValidationException {
        String trimmed = validateNonBlank(str, fieldName);
        try {
            double val = Double.parseDouble(trimmed);
            if (Double.isNaN(val) || Double.isInfinite(val)) {
                throw new ValidationException(fieldName + " must be a valid numeric amount.");
            }
            if (val < 0) {
                throw new ValidationException(fieldName + " cannot be negative.");
            }
            if (val > MAX_MONEY_AMOUNT) {
                throw new ValidationException(fieldName + " exceeds maximum allowed salary limit.");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid decimal amount.");
        }
    }
}

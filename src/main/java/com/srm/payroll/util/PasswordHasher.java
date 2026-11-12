package com.srm.payroll.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Password hashing utility using PBKDF2WithHmacSHA256.
 * Adheres to OWASP recommendations with a 16-byte salt, 600,000 iterations,
 * and 256-bit output key length. Constant-time verification prevents timing attacks.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 600_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Utility class
    }

    /**
     * Generates a cryptographically secure random 16-byte salt encoded in Base64.
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes the given password using PBKDF2WithHmacSHA256 and Base64 salt.
     */
    public static String hash(char[] password, String saltBase64) {
        if (password == null || saltBase64 == null) {
            throw new IllegalArgumentException("Password and salt must not be null.");
        }
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Error hashing password with algorithm " + ALGORITHM, e);
        } finally {
            ((PBEKeySpec) spec).clearPassword();
        }
    }

    /**
     * Verifies whether the provided password matches the expected Base64 hash using constant-time comparison.
     */
    public static boolean verify(char[] password, String saltBase64, String expectedHashBase64) {
        if (password == null || saltBase64 == null || expectedHashBase64 == null) {
            return false;
        }
        String computedHashBase64 = hash(password, saltBase64);
        byte[] expectedBytes = expectedHashBase64.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] computedBytes = computedHashBase64.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, computedBytes);
    }

    public static void main(String[] args) {
        String saltAdmin = generateSalt();
        String hashAdmin = hash("Admin@1234".toCharArray(), saltAdmin);
        String saltHr = generateSalt();
        String hashHr = hash("HrUser@1234".toCharArray(), saltHr);

        System.out.println("ADMIN_SALT=" + saltAdmin);
        System.out.println("ADMIN_HASH=" + hashAdmin);
        System.out.println("HR_SALT=" + saltHr);
        System.out.println("HR_HASH=" + hashHr);
    }
}

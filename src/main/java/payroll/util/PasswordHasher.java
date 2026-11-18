package payroll.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Password hashing utility using PBKDF2WithHmacSHA256 (600,000 iterations).
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 600_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(char[] password, String saltBase64) {
        if (password == null || saltBase64 == null)
            throw new IllegalArgumentException("Password and salt must not be null.");
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Error hashing password.", e);
        } finally {
            ((PBEKeySpec) spec).clearPassword();
        }
    }

    public static boolean verify(char[] password, String saltBase64, String expectedHashBase64) {
        if (password == null || saltBase64 == null || expectedHashBase64 == null) return false;
        String computed = hash(password, saltBase64);
        return MessageDigest.isEqual(
                expectedHashBase64.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                computed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}

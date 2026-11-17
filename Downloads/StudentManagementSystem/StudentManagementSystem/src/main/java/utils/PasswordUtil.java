package utils;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt password hashing helpers. Plain-text passwords are never stored. */
public final class PasswordUtil {

    /** Work factor. 12 is a good balance of security and login speed (~250 ms). */
    private static final int LOG_ROUNDS = 12;

    private PasswordUtil() { }

    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    /** Safely verifies a password against a stored BCrypt hash. */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException malformedHash) {
            return false;
        }
    }

    /**
     * Utility entry point: prints a BCrypt hash for a password so you can put it
     * into the SQL script.
     *
     * <pre>java -cp "target/classes:libs/*" utils.PasswordUtil myNewPassword</pre>
     */
    public static void main(String[] args) {
        String pwd = args.length > 0 ? args[0] : "admin123";
        String hash = hash(pwd);
        System.out.println("Password : " + pwd);
        System.out.println("BCrypt   : " + hash);
        System.out.println("Verify   : " + verify(pwd, hash));
    }
}

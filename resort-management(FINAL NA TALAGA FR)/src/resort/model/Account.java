package resort.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public abstract class Account implements Serializable {
    private static final long serialVersionUID = 8726981512789219048L;
    private static final String PASSWORD_PREFIX = "PBKDF2$";
    private static final int MIN_PASSWORD_ITERATIONS = 120000;
    private static final int PASSWORD_ITERATIONS = 600000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private String username;
    private String password;
    private String staffId;
    private boolean passwordChangeRequired;
    private boolean administrator;
    private boolean administratorRoleInitialized;

    public Account(String username, char[] password) {
        this(username, password, false);
    }

    protected Account(String username, boolean administrator) {
        this.username = validateUsername(username);
        this.password = "";
        setAdministratorRole(administrator);
    }

    protected Account(String username, char[] password, boolean administrator) {
        this.username = validateUsername(username);
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        this.password = hashPassword(password);
        setAdministratorRole(administrator);
    }

    public String getUsername() {
        return username;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void requirePasswordChange() {
        passwordChangeRequired = true;
    }

    void changePassword(char[] newPassword) {
        if (newPassword == null || newPassword.length < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        password = hashPassword(newPassword);
        passwordChangeRequired = false;
    }

    String getStoredPassword() {
        return password;
    }

    void restorePasswordState(String storedPassword, boolean changeRequired) {
        password = storedPassword;
        passwordChangeRequired = changeRequired;
    }

    public boolean checkPassword(String attempt) {
        char[] characters = attempt == null ? null : attempt.toCharArray();
        try {
            return checkPassword(characters);
        } finally {
            if (characters != null) {
                Arrays.fill(characters, '\0');
            }
        }
    }

    public boolean checkPassword(char[] attempt) {
        if (attempt == null || password == null) {
            return false;
        }
        if (!isPasswordHash(password)) {
            if (!matches(password, attempt)) {
                return false;
            }
            password = hashPassword(attempt);
            return true;
        }

        try {
            String[] parts = password.split("\\$", -1);
            if (parts.length != 4) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < MIN_PASSWORD_ITERATIONS
                    || iterations > PASSWORD_ITERATIONS) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derivePassword(attempt, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean isPasswordHash(String value) {
        if (value == null || !value.startsWith(PASSWORD_PREFIX)) {
            return false;
        }
        String[] parts = value.split("\\$", -1);
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            return iterations >= MIN_PASSWORD_ITERATIONS
                    && iterations <= PASSWORD_ITERATIONS
                    && Base64.getDecoder().decode(parts[2]).length == SALT_BYTES
                    && Base64.getDecoder().decode(parts[3]).length == HASH_BITS / 8;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String validateUsername(String username) {
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        return cleanUsername;
    }

    private static boolean matches(String expected, char[] attempt) {
        if (expected.length() != attempt.length) {
            return false;
        }
        int difference = 0;
        for (int i = 0; i < attempt.length; i++) {
            difference |= expected.charAt(i) ^ attempt[i];
        }
        return difference == 0;
    }

    private static String hashPassword(char[] characters) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derivePassword(characters, salt, PASSWORD_ITERATIONS);
        return PASSWORD_PREFIX + PASSWORD_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private static byte[] derivePassword(char[] characters, byte[] salt, int iterations) {
        char[] copy = characters.clone();
        PBEKeySpec specification = new PBEKeySpec(copy, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification).getEncoded();
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Password hashing is unavailable.", ex);
        } finally {
            specification.clearPassword();
            Arrays.fill(copy, '\0');
        }
    }

    boolean hasLegacyPassword() {
        return password != null && !isPasswordHash(password);
    }

    final void setAdministratorRole(boolean administrator) {
        this.administrator = administrator;
        administratorRoleInitialized = true;
    }

    public boolean isAdmin() {
        return administrator;
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        if (!administratorRoleInitialized) {
            administrator = this instanceof Admin;
            administratorRoleInitialized = true;
        }
    }
}

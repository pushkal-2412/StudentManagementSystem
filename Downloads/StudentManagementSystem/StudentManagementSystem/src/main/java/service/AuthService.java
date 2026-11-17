package service;

import dao.AdminDAO;
import database.DBConnection;
import model.Admin;
import utils.PasswordUtil;
import utils.SessionManager;

import java.sql.SQLException;
import java.util.Optional;

/** Authentication, session and password change logic. */
public class AuthService {

    private final AdminDAO adminDAO = new AdminDAO();

    /** Thrown for any credential problem; the message is safe to show to the user. */
    public static class AuthException extends Exception {
        public AuthException(String message) { super(message); }
    }

    /**
     * Verifies credentials against the BCrypt hash stored in the admins table and
     * opens a session on success.
     */
    public Admin login(String username, String password) throws AuthException {
        if (username == null || username.trim().isEmpty()) {
            throw new AuthException("Username cannot be empty.");
        }
        if (password == null || password.isEmpty()) {
            throw new AuthException("Password cannot be empty.");
        }

        Optional<Admin> found;
        try {
            found = adminDAO.findByUsername(username.trim());
        } catch (SQLException e) {
            throw new AuthException("Cannot reach the database.\n\n"
                + "Details: " + e.getMessage()
                + "\n\nCheck that MySQL is running and that application.properties "
                + "has the correct URL, user and password.");
        }

        if (found.isEmpty()) {
            throw new AuthException("Invalid username or password.");
        }
        Admin admin = found.get();
        if (!PasswordUtil.verify(password, admin.getPasswordHash())) {
            throw new AuthException("Invalid username or password.");
        }
        SessionManager.getInstance().login(admin);
        return admin;
    }

    public void logout() {
        SessionManager.getInstance().logout();
    }

    /** Changes the password of the logged-in administrator. */
    public void changePassword(String oldPassword, String newPassword, String confirmPassword)
            throws AuthException {
        SessionManager session = SessionManager.getInstance();
        if (!session.isAuthenticated()) {
            throw new AuthException("No active session. Please log in again.");
        }
        try {
            Admin admin = adminDAO.findByUsername(session.getUsername())
                    .orElseThrow(() -> new AuthException("Account no longer exists."));
            if (!PasswordUtil.verify(oldPassword, admin.getPasswordHash())) {
                throw new AuthException("The current password is incorrect.");
            }
            if (newPassword == null || newPassword.length() < 6) {
                throw new AuthException("The new password must be at least 6 characters.");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new AuthException("The new password and confirmation do not match.");
            }
            if (PasswordUtil.verify(newPassword, admin.getPasswordHash())) {
                throw new AuthException("The new password must be different from the old one.");
            }
            adminDAO.updatePasswordHash(admin.getUsername(), PasswordUtil.hash(newPassword));
            admin.setPasswordHash(PasswordUtil.hash(newPassword));
            session.login(admin);
        } catch (SQLException e) {
            throw new AuthException("Could not update the password: " + e.getMessage());
        }
    }

    /** Used by the start-up screen to report connectivity clearly. */
    public static String testDatabase() {
        try {
            DBConnection.testConnection();
            return null;
        } catch (SQLException e) {
            return e.getMessage();
        }
    }
}

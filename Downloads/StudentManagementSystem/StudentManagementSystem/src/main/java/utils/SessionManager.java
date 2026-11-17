package utils;

import model.Admin;
import java.time.LocalDateTime;

/**
 * Holds the authenticated administrator for the running application (session).
 * Every administrative frame asks this object before performing an action.
 */
public final class SessionManager {

    private static SessionManager instance;

    private Admin currentAdmin;
    private LocalDateTime loginTime;

    private SessionManager() { }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(Admin admin) {
        this.currentAdmin = admin;
        this.loginTime = LocalDateTime.now();
    }

    /** Ends the session. Called by logout and on window close. */
    public void logout() {
        this.currentAdmin = null;
        this.loginTime = null;
    }

    public boolean isAuthenticated() {
        return currentAdmin != null;
    }

    public Admin getCurrentAdmin() {
        if (currentAdmin == null) {
            throw new IllegalStateException("No active session. Please log in.");
        }
        return currentAdmin;
    }

    public String getUsername() {
        return isAuthenticated() ? currentAdmin.getUsername() : "-";
    }

    public String getRole() {
        return isAuthenticated() ? currentAdmin.getRole() : "-";
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    /** Role based access control used by controllers. */
    public boolean hasRole(String role) {
        return isAuthenticated() && role.equalsIgnoreCase(currentAdmin.getRole());
    }

    public boolean isAdmin() { return hasRole("ADMIN"); }
}

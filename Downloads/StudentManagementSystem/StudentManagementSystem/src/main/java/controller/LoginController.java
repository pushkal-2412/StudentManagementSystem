package controller;

import model.Admin;
import service.AuthService;

/** Controller for the authentication screen. */
public class LoginController {

    private final AuthService authService = new AuthService();

    public Admin login(String username, char[] password) throws AuthService.AuthException {
        String pwd = password == null ? "" : new String(password);
        return authService.login(username, pwd);
    }

    public void logout() { authService.logout(); }

    public void changePassword(String oldPwd, String newPwd, String confirm)
            throws AuthService.AuthException {
        authService.changePassword(oldPwd, newPwd, confirm);
    }

    /** @return null when the database is reachable, otherwise the error text. */
    public String checkDatabase() { return AuthService.testDatabase(); }
}

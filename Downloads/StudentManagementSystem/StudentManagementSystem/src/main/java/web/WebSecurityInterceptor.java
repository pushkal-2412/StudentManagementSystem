package web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class WebSecurityInterceptor implements HandlerInterceptor {

    static final String ADMIN_ID = "smsAdminId";
    static final String CSRF_TOKEN = "smsCsrfToken";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || "/api/session".equals(path)) {
            return true;
        }

        if (!"GET".equals(request.getMethod()) && !hasValidCsrfToken(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid request token.");
            return false;
        }
        if (!"/api/login".equals(path)
                && request.getSession(false) != null
                && request.getSession(false).getAttribute(ADMIN_ID) != null) {
            return true;
        }
        if ("/api/login".equals(path)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"Please sign in to continue.\"}");
        return false;
    }

    private boolean hasValidCsrfToken(HttpServletRequest request) {
        if (request.getSession(false) == null) {
            return false;
        }
        Object expected = request.getSession(false).getAttribute(CSRF_TOKEN);
        String supplied = request.getHeader("X-CSRF-TOKEN");
        return expected instanceof String token && supplied != null
                && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8));
    }
}

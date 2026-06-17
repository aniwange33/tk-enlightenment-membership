package com.tertech.tkenlightment.membership.auth.jwt;

import com.tertech.tkenlightment.membership.auth.AuthPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * While the authenticated principal still carries {@code mustChangePassword}, blocks every request
 * except {@code POST /api/auth/change-password} with 403.
 */
class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final String CHANGE_PASSWORD_PATH = "/api/auth/change-password";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Only gate the REST API; the SPA shell must always load so a first-login
        // user can reach the change-password screen client-side.
        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal
                && principal.mustChangePassword()
                && !isChangePasswordRequest(request)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(
                    "{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,"
                            + "\"detail\":\"Password change required\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isChangePasswordRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && CHANGE_PASSWORD_PATH.equals(request.getRequestURI());
    }
}

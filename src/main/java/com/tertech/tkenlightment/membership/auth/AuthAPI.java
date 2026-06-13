package com.tertech.tkenlightment.membership.auth;

import com.tertech.tkenlightment.membership.auth.rest.dtos.LoginResponse;
import org.springframework.stereotype.Component;

/** Public entry point into the auth module. */
@Component
public class AuthAPI {

    private final AuthService authService;

    AuthAPI(AuthService authService) {
        this.authService = authService;
    }

    public LoginResponse login(String email, String password) {
        return toResponse(authService.login(email, password));
    }

    public LoginResponse changePassword(String accountId, String currentPassword, String newPassword) {
        return toResponse(authService.changePassword(accountId, currentPassword, newPassword));
    }

    private LoginResponse toResponse(TokenResult result) {
        return new LoginResponse(result.token(), result.mustChangePassword());
    }
}

package com.tertech.tkenlightment.membership.auth.rest.controllers;

import com.tertech.tkenlightment.membership.auth.AuthAPI;
import com.tertech.tkenlightment.membership.auth.AuthPrincipal;
import com.tertech.tkenlightment.membership.auth.rest.dtos.ChangePasswordRequest;
import com.tertech.tkenlightment.membership.auth.rest.dtos.ForgotPasswordRequest;
import com.tertech.tkenlightment.membership.auth.rest.dtos.LoginRequest;
import com.tertech.tkenlightment.membership.auth.rest.dtos.LoginResponse;
import com.tertech.tkenlightment.membership.auth.rest.dtos.MessageResponse;
import com.tertech.tkenlightment.membership.auth.rest.dtos.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthAPI authAPI;

    AuthController(AuthAPI authAPI) {
        this.authAPI = authAPI;
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authAPI.login(request.email(), request.password());
    }

    @PostMapping("/change-password")
    LoginResponse changePassword(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        return authAPI.changePassword(
                principal.accountId(), request.currentPassword(), request.newPassword());
    }

    @PostMapping("/forgot-password")
    MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authAPI.requestPasswordReset(request.email());
        return new MessageResponse("Reset link sent");
    }

    @PostMapping("/reset-password")
    MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authAPI.resetPassword(request.token(), request.newPassword());
        return new MessageResponse("Password updated");
    }
}

package com.tertech.tkenlightment.membership.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.tertech.tkenlightment.membership.BaseIT;
import com.tertech.tkenlightment.membership.auth.rest.dtos.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class AuthFlowTests extends BaseIT {

    @Test
    void shouldRejectBadCredentialsWithGeneric401() {
        MvcTestResult result = mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "admin@taraku-enlightenment.org", "password": "wrong-password" }
                        """)
                .exchange();

        assertThat(result).hasStatus(401);
    }

    @Test
    void shouldRejectUnknownEmailWithGeneric401() {
        MvcTestResult result = mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "nobody@example.com", "password": "whatever123" }
                        """)
                .exchange();

        assertThat(result).hasStatus(401);
    }

    @Test
    void shouldBlockAllEndpointsExceptChangePasswordWhileMustChange() {
        String token = tokens.adminMustChangePassword();

        MvcTestResult blocked = mvc.get()
                .uri("/api/members")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .exchange();

        assertThat(blocked).hasStatus(403);
        assertThat(blocked).bodyText().contains("Password change required");
    }

    @Test
    void seededAdminLoginThenChangePasswordUnlocksAccess() {
        // Login as the seeded admin (forced to change on first login).
        MvcTestResult login = mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "admin@taraku-enlightenment.org", "password": "AdminTest!2026" }
                        """)
                .exchange();

        assertThat(login).hasStatusOk();
        LoginResponse loginBody = readBody(login, LoginResponse.class);
        assertThat(loginBody.token()).isNotBlank();

        // Before changing, the must-change gate blocks normal endpoints.
        if (loginBody.mustChangePassword()) {
            MvcTestResult gated = mvc.get()
                    .uri("/api/members")
                    .header(HttpHeaders.AUTHORIZATION, bearer(loginBody.token()))
                    .exchange();
            assertThat(gated).hasStatus(403);
        }

        // Change password -> fresh token without the flag.
        MvcTestResult changed = mvc.post()
                .uri("/api/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, bearer(loginBody.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "currentPassword": "AdminTest!2026", "newPassword": "NewAdminPass!1" }
                        """)
                .exchange();

        assertThat(changed).hasStatusOk();
        LoginResponse changedBody = readBody(changed, LoginResponse.class);
        assertThat(changedBody.mustChangePassword()).isFalse();

        // The fresh token now reaches admin endpoints.
        MvcTestResult admin = mvc.get()
                .uri("/api/members")
                .header(HttpHeaders.AUTHORIZATION, bearer(changedBody.token()))
                .exchange();
        assertThat(admin).hasStatusOk();

        // Restore the seeded credential so other tests' logins keep working.
        MvcTestResult restore = mvc.post()
                .uri("/api/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, bearer(changedBody.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "currentPassword": "NewAdminPass!1", "newPassword": "AdminTest!2026" }
                        """)
                .exchange();
        assertThat(restore).hasStatusOk();
    }
}

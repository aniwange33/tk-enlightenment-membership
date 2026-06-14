package com.tertech.tkenlightment.membership.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.tertech.tkenlightment.membership.BaseIT;
import com.tertech.tkenlightment.membership.auth.rest.dtos.LoginResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** End-to-end forgot-password -> emailed token -> reset -> login, plus the error paths. */
class PasswordResetFlowTests extends BaseIT {

    private static final Pattern TOKEN = Pattern.compile("token=([0-9a-f]+)");

    @Autowired
    UserAccountRepository accountRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void forgotThenResetLetsMemberLogInWithNewPassword() {
        String email = "reset.flow@example.com";
        seedMember(email, "member-reset-1", "Original123", true);

        MvcTestResult forgot = forgotPassword(email);
        assertThat(forgot).hasStatusOk();

        String rawToken = awaitResetTokenEmailedTo(email);

        MvcTestResult reset = resetPassword(rawToken, "BrandNew123");
        assertThat(reset).hasStatusOk();

        // New password works...
        MvcTestResult login = login(email, "BrandNew123");
        assertThat(login).hasStatusOk();
        LoginResponse body = readBody(login, LoginResponse.class);
        assertThat(body.token()).isNotBlank();
        // ...reset cleared the forced-change flag.
        assertThat(body.mustChangePassword()).isFalse();

        // Old password no longer works.
        assertThat(login(email, "Original123")).hasStatus(401);
    }

    @Test
    void resetTokenIsSingleUse() {
        String email = "reset.singleuse@example.com";
        seedMember(email, "member-reset-2", "Original123", true);

        assertThat(forgotPassword(email)).hasStatusOk();
        String rawToken = awaitResetTokenEmailedTo(email);

        assertThat(resetPassword(rawToken, "FirstReset12")).hasStatusOk();
        // Second use of the same token is rejected.
        assertThat(resetPassword(rawToken, "SecondReset12")).hasStatus(400);
    }

    @Test
    void forgotPasswordForUnknownEmailReturns404() {
        assertThat(forgotPassword("nobody.here@example.com")).hasStatus(404);
    }

    @Test
    void forgotPasswordForDisabledAccountReturns403() {
        String email = "reset.disabled@example.com";
        seedMember(email, "member-reset-3", "Original123", false);

        assertThat(forgotPassword(email)).hasStatus(403);
    }

    @Test
    void resetWithInvalidTokenReturns400() {
        assertThat(resetPassword("deadbeef", "Whatever123")).hasStatus(400);
    }

    // ---- helpers ----

    private void seedMember(String email, String memberId, String rawPassword, boolean enabled) {
        var account = UserAccountEntity.createMember(email, memberId, passwordEncoder.encode(rawPassword));
        account.setEnabledForStatus(enabled);
        accountRepository.save(account);
    }

    private MvcTestResult forgotPassword(String email) {
        return mvc.post()
                .uri("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"" + email + "\" }")
                .exchange();
    }

    private MvcTestResult resetPassword(String token, String newPassword) {
        return mvc.post()
                .uri("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"token\": \"" + token + "\", \"newPassword\": \"" + newPassword + "\" }")
                .exchange();
    }

    private MvcTestResult login(String email, String password) {
        return mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"" + email + "\", \"password\": \"" + password + "\" }")
                .exchange();
    }

    private String awaitResetTokenEmailedTo(String email) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            verify(mailSender, atLeastOnce()).send(captor.capture());
            assertThat(extractToken(captor, email)).isNotNull();
        });
        return extractToken(captor, email);
    }

    private String extractToken(ArgumentCaptor<SimpleMailMessage> captor, String email) {
        for (SimpleMailMessage message : captor.getAllValues()) {
            String[] to = message.getTo();
            if (to == null || to.length == 0 || !email.equals(to[0]) || message.getText() == null) {
                continue;
            }
            Matcher matcher = TOKEN.matcher(message.getText());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}

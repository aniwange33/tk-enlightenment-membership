package com.tertech.tkenlightment.membership.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tertech.tkenlightment.membership.auth.domain.events.PasswordResetRequestedEvent;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-14T10:00:00Z");

    @Mock
    UserAccountRepository accountRepository;

    @Mock
    PasswordResetTokenRepository tokenRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    SpringEventPublisher eventPublisher;

    @Captor
    ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor;

    @Captor
    ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor;

    private PasswordResetService service() {
        return new PasswordResetService(
                accountRepository,
                tokenRepository,
                passwordEncoder,
                eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private UserAccountEntity enabledAccount() {
        return UserAccountEntity.createMember("user@example.com", "member-1", "hash");
    }

    private static String sha256Hex(String raw) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- requestReset ----

    @Test
    void requestRejectsUnknownEmailWith404() {
        when(accountRepository.findByEmail("nobody@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service().requestReset("nobody@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void requestRejectsDisabledAccountWith403() {
        UserAccountEntity disabled = enabledAccount();
        disabled.setEnabledForStatus(false);
        when(accountRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(disabled));

        assertThatThrownBy(() -> service().requestReset("user@example.com"))
                .isInstanceOf(DisabledException.class);
        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void requestStoresHashedTokenAndPublishesRawTokenAndSupersedesPrior() {
        UserAccountEntity account = enabledAccount();
        when(accountRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(account));

        PasswordResetTokenEntity prior =
                PasswordResetTokenEntity.create(account.getId(), "oldhash", NOW.plus(Duration.ofHours(5)));
        when(tokenRepository.findByUserAccountIdAndUsedAtIsNull(account.getId()))
                .thenReturn(List.of(prior));

        service().requestReset("user@example.com");

        verify(tokenRepository).save(tokenCaptor.capture());
        verify(eventPublisher).publish(eventCaptor.capture());

        String rawToken = eventCaptor.getValue().rawToken();
        PasswordResetTokenEntity saved = tokenCaptor.getValue();

        // Only the hash is stored; the raw token is never persisted.
        assertThat(rawToken).isNotBlank();
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken).isEqualTo(sha256Hex(rawToken));
        assertThat(saved.getUserAccountId()).isEqualTo(account.getId());
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        // Prior unused token is superseded.
        assertThat(prior.getUsedAt()).isEqualTo(NOW);
        assertThat(eventCaptor.getValue().email()).isEqualTo("user@example.com");
    }

    // ---- resetPassword ----

    @Test
    void resetRejectsUnknownTokenWith400() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service().resetPassword("whatever", "NewPass12"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resetRejectsExpiredToken() {
        String raw = "rawtoken";
        PasswordResetTokenEntity expired = PasswordResetTokenEntity.create(
                "member-1", sha256Hex(raw), NOW.minus(Duration.ofMinutes(1)));
        when(tokenRepository.findByTokenHash(sha256Hex(raw))).thenReturn(java.util.Optional.of(expired));

        assertThatThrownBy(() -> service().resetPassword(raw, "NewPass12"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void resetRejectsAlreadyUsedToken() {
        String raw = "rawtoken";
        PasswordResetTokenEntity used = PasswordResetTokenEntity.create(
                "member-1", sha256Hex(raw), NOW.plus(Duration.ofHours(1)));
        used.markUsed(NOW.minus(Duration.ofMinutes(5)));
        when(tokenRepository.findByTokenHash(sha256Hex(raw))).thenReturn(java.util.Optional.of(used));

        assertThatThrownBy(() -> service().resetPassword(raw, "NewPass12"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resetRejectsDisabledAccountWith403() {
        String raw = "rawtoken";
        PasswordResetTokenEntity token = PasswordResetTokenEntity.create(
                "member-1", sha256Hex(raw), NOW.plus(Duration.ofHours(1)));
        when(tokenRepository.findByTokenHash(sha256Hex(raw))).thenReturn(java.util.Optional.of(token));
        UserAccountEntity disabled = enabledAccount();
        disabled.setEnabledForStatus(false);
        when(accountRepository.findById("member-1")).thenReturn(java.util.Optional.of(disabled));

        assertThatThrownBy(() -> service().resetPassword(raw, "NewPass12"))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void resetUpdatesPasswordClearsMustChangeAndConsumesToken() {
        String raw = "rawtoken";
        PasswordResetTokenEntity token = PasswordResetTokenEntity.create(
                "member-1", sha256Hex(raw), NOW.plus(Duration.ofHours(1)));
        when(tokenRepository.findByTokenHash(sha256Hex(raw))).thenReturn(java.util.Optional.of(token));
        UserAccountEntity account = enabledAccount();
        assertThat(account.isMustChangePassword()).isTrue();
        when(accountRepository.findById("member-1")).thenReturn(java.util.Optional.of(account));
        when(passwordEncoder.encode("NewPass12")).thenReturn("ENCODED");

        service().resetPassword(raw, "NewPass12");

        assertThat(account.getPasswordHash()).isEqualTo("ENCODED");
        assertThat(account.isMustChangePassword()).isFalse();
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        verify(accountRepository).save(account);
        verify(tokenRepository).save(token);
    }
}

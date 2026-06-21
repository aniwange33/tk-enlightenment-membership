package com.tertech.tkenlightment.membership.auth;

import com.tertech.tkenlightment.membership.auth.domain.events.PasswordResetRequestedEvent;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.ResourceNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Forgot-password / reset flow. Issues single-use, 24h, DB-backed tokens (only the hash is stored)
 * and consumes them to set a new password. A successful reset also clears the forced first-login
 * change; existing JWTs stay valid until natural expiry (stateless model, no revocation).
 */
@Service
@Transactional
class PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofHours(24);
    private static final int TOKEN_BYTES = 32;
    private static final String INVALID_TOKEN = "Invalid or expired reset token";
    private static final HexFormat HEX = HexFormat.of();

    private final UserAccountRepository accountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SpringEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    PasswordResetService(
            UserAccountRepository accountRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            SpringEventPublisher eventPublisher,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Issues a reset token for the account owning {@code email} and emits {@link
     * PasswordResetRequestedEvent}. Surfaces account state directly: 404 for an unknown email, 403
     * for a disabled/terminated account (a deliberate, documented enumeration trade-off).
     */
    void requestReset(String email) {
        UserAccountEntity account = accountRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for that email"));

        if (!account.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        // Supersede any outstanding tokens so only the newest emailed link works.
        Instant now = clock.instant();
        tokenRepository.findByUserAccountIdAndUsedAtIsNull(account.getId())
                .forEach(token -> token.markUsed(now));

        String rawToken = generateRawToken();
        tokenRepository.save(
                PasswordResetTokenEntity.create(account.getId(), hash(rawToken), now.plus(TOKEN_TTL)));

        eventPublisher.publish(new PasswordResetRequestedEvent(account.getEmail(), rawToken));
        meterRegistry.counter("password_reset.requested").increment();
    }

    /**
     * Consumes a valid token and sets the new password. Invalid/expired/used tokens are rejected
     * with a generic 400; a since-disabled account yields 403.
     */
    void resetPassword(String rawToken, String newPassword) {
        Instant now = clock.instant();
        PasswordResetTokenEntity token = tokenRepository
                .findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_TOKEN));

        if (!token.isUsableAt(now)) {
            throw new IllegalArgumentException(INVALID_TOKEN);
        }

        UserAccountEntity account = accountRepository
                .findById(token.getUserAccountId())
                .orElseThrow(() -> new IllegalArgumentException(INVALID_TOKEN));
        if (!account.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        account.changePassword(passwordEncoder.encode(newPassword));
        token.markUsed(now);
        accountRepository.save(account);
        tokenRepository.save(token);
        meterRegistry.counter("password_reset.completed").increment();
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

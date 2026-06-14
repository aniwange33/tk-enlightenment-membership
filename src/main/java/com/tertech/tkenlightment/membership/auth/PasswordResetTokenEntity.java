package com.tertech.tkenlightment.membership.auth;

import com.tertech.tkenlightment.membership.shared.domain.models.BaseEntity;
import com.tertech.tkenlightment.membership.shared.domain.models.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A single-use, time-bounded password-reset grant. Only the SHA-256 {@code tokenHash} is stored; the
 * raw token lives solely in the emailed link, so a leaked row cannot be replayed.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@NoArgsConstructor
class PasswordResetTokenEntity extends BaseEntity {

    @Id
    private String id;

    @Column(name = "user_account_id", nullable = false)
    private String userAccountId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    static PasswordResetTokenEntity create(String userAccountId, String tokenHash, Instant expiresAt) {
        var token = new PasswordResetTokenEntity();
        token.id = IdGenerator.newId();
        token.userAccountId = userAccountId;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        return token;
    }

    /** A token is usable until it is consumed or its 24h window elapses. */
    boolean isUsableAt(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    void markUsed(Instant now) {
        this.usedAt = now;
    }
}

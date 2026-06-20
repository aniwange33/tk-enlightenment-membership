package com.tertech.tkenlightment.membership.auth;

import com.tertech.tkenlightment.membership.auth.domain.events.AccountCreatedEvent;
import com.tertech.tkenlightment.membership.auth.domain.models.Role;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns account provisioning and the sync of login-ability with member status. */
@Service
@Transactional
class AccountService {

    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final UserAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SpringEventPublisher eventPublisher;

    AccountService(
            UserAccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            SpringEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Provisions a MEMBER account for a newly registered member, then emits {@link
     * AccountCreatedEvent} so the welcome email carries the real temporary password. Idempotent on
     * memberId (tolerates event redelivery) and reconciles a leftover disabled account that still
     * owns the email after a re-registration.
     */
    void provisionForMember(String memberId, String email, String fullName, String membershipNumber) {
        if (accountRepository.findByMemberId(memberId).isPresent()) {
            return; // already provisioned — redelivered event
        }

        String tempPassword = generateTempPassword();
        String hash = passwordEncoder.encode(tempPassword);

        Optional<UserAccountEntity> existingByEmail = accountRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            UserAccountEntity account = existingByEmail.get();
            if (account.isEnabled()) {
                // Email already owned by a live account — should not happen (member emails are
                // unique). Do not clobber an active login.
                return;
            }
            account.reissueForMember(memberId, hash);
            accountRepository.save(account);
        } else {
            accountRepository.save(UserAccountEntity.createMember(email, memberId, hash));
        }

        eventPublisher.publish(
                new AccountCreatedEvent(email, fullName, membershipNumber, tempPassword));
    }

    /** Mirrors login-ability onto member standing: enabled only while the member is ACTIVE. */
    void syncEnabledForStatus(String memberId, boolean active) {
        accountRepository
                .findByMemberId(memberId)
                .ifPresent(account -> {
                    account.setEnabledForStatus(active);
                    accountRepository.save(account);
                });
    }

    /** Seeds exactly one ADMIN if none exists. Idempotent; no-op when an admin is already present. */
    void seedAdminIfAbsent(String email, String rawPassword) {
        if (accountRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        accountRepository.save(
                UserAccountEntity.createAdmin(email, passwordEncoder.encode(rawPassword)));
    }

    private String generateTempPassword() {
        RandomGenerator rng = RandomGenerator.getDefault();
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(rng.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }
}

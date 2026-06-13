package com.tertech.tkenlightment.membership.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tertech.tkenlightment.membership.BaseIT;
import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.ChangeStatusCmd;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import com.tertech.tkenlightment.membership.member.domain.services.RegisterMemberCmd;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Covers the account lifecycle the spec calls out but that the HTTP tests cannot reach (member temp
 * passwords are random/email-only): disabled-login refusal, status→enabled sync, provisioning
 * idempotency, and re-registration reconciliation. Service-level checks use the package-private
 * beans directly; one async test proves the event wiring end-to-end.
 */
class AccountLifecycleTests extends BaseIT {

    @Autowired
    AccountService accountService;

    @Autowired
    AuthService authService;

    @Autowired
    UserAccountRepository accountRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    MemberAPI memberAPI;

    @Test
    void disabledAccountCannotLogInButReEnablingRestoresAccess() {
        String memberId = "member-disabled-1";
        String email = "disabled.login@example.com";
        seedMemberAccount(email, memberId, "Secret123");

        // Enabled: login succeeds.
        assertThat(authService.login(email, "Secret123").token()).isNotBlank();

        // Member leaves ACTIVE -> account disabled -> login refused even with the right password.
        accountService.syncEnabledForStatus(memberId, false);
        assertThrows(DisabledException.class, () -> authService.login(email, "Secret123"));

        // Back to ACTIVE -> login restored.
        accountService.syncEnabledForStatus(memberId, true);
        assertThat(authService.login(email, "Secret123").token()).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        String memberId = "member-badpass-1";
        String email = "badpass.login@example.com";
        seedMemberAccount(email, memberId, "Secret123");

        assertThrows(BadCredentialsException.class, () -> authService.login(email, "WrongPass1"));
    }

    @Test
    void provisioningIsIdempotentOnMemberId() {
        String memberId = "member-idem-1";
        String email = "idempotent.provision@example.com";

        accountService.provisionForMember(memberId, email, "Idem User", "TEC-2026-900");
        accountService.provisionForMember(memberId, email, "Idem User", "TEC-2026-900");

        assertThat(accountRepository.findByMemberId(memberId)).isPresent();
        assertThat(accountRepository.findAll().stream()
                        .filter(a -> memberId.equals(a.getMemberId()))
                        .count())
                .isEqualTo(1);
    }

    @Test
    void reRegistrationReissuesLeftoverDisabledAccount() {
        String email = "reregister@example.com";
        String oldMemberId = "member-terminated-1";
        String newMemberId = "member-reregistered-1";

        // Leftover disabled account from a terminated member still owns the email.
        var leftover = UserAccountEntity.createMember(email, oldMemberId, passwordEncoder.encode("Old12345"));
        leftover.setEnabledForStatus(false);
        accountRepository.save(leftover);

        // New registration with the same email reuses and reactivates that account.
        accountService.provisionForMember(newMemberId, email, "Re Registered", "TEC-2026-901");

        var account = accountRepository.findByEmail(email).orElseThrow();
        assertThat(account.getMemberId()).isEqualTo(newMemberId);
        assertThat(account.isEnabled()).isTrue();
        assertThat(account.isMustChangePassword()).isTrue();
        assertThat(accountRepository.findByMemberId(oldMemberId)).isEmpty();
    }

    @Test
    void changePasswordRequiresCorrectCurrentAndClearsMustChange() {
        String memberId = "member-changepw-1";
        String email = "changepw@example.com";
        var account = seedMemberAccount(email, memberId, "Initial123");
        assertThat(account.isMustChangePassword()).isTrue();

        assertThrows(
                BadCredentialsException.class,
                () -> authService.changePassword(account.getId(), "WrongCurrent1", "Updated123"));

        var result = authService.changePassword(account.getId(), "Initial123", "Updated123");
        assertThat(result.mustChangePassword()).isFalse();

        // New password works; old one no longer does.
        assertThat(authService.login(email, "Updated123").token()).isNotBlank();
        assertThrows(BadCredentialsException.class, () -> authService.login(email, "Initial123"));
    }

    @Test
    void registrationProvisionsAccountAndStatusChangeDisablesIt() {
        MemberResult member = memberAPI.registerMember(new RegisterMemberCmd(
                "Async",
                "Member",
                LocalDate.of(1990, 1, 15),
                "async.lifecycle@example.com",
                null,
                null));
        String memberId = member.id();

        // The auth listener provisions an enabled account asynchronously after commit.
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var account = accountRepository.findByMemberId(memberId);
            assertThat(account).isPresent();
            assertThat(account.get().isEnabled()).isTrue();
        });

        // Suspending the member disables login via MemberStatusChangedEvent.
        memberAPI.changeStatus(new ChangeStatusCmd(memberId, MemberStatus.SUSPENDED));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(accountRepository.findByMemberId(memberId).orElseThrow().isEnabled())
                        .isFalse());
    }

    private UserAccountEntity seedMemberAccount(String email, String memberId, String rawPassword) {
        return accountRepository.save(
                UserAccountEntity.createMember(email, memberId, passwordEncoder.encode(rawPassword)));
    }
}

package com.tertech.tkenlightment.membership.auth;

import com.tertech.tkenlightment.membership.auth.domain.models.Role;
import com.tertech.tkenlightment.membership.shared.domain.models.BaseEntity;
import com.tertech.tkenlightment.membership.shared.domain.models.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_accounts")
@Getter
@Setter
@NoArgsConstructor
class UserAccountEntity extends BaseEntity {

    @Id
    private String id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "member_id")
    private String memberId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    static UserAccountEntity createMember(String email, String memberId, String passwordHash) {
        var account = new UserAccountEntity();
        account.id = IdGenerator.newId();
        account.email = email;
        account.memberId = memberId;
        account.passwordHash = passwordHash;
        account.role = Role.MEMBER;
        account.enabled = true;
        account.mustChangePassword = true;
        return account;
    }

    static UserAccountEntity createAdmin(String email, String passwordHash) {
        var account = new UserAccountEntity();
        account.id = IdGenerator.newId();
        account.email = email;
        account.passwordHash = passwordHash;
        account.role = Role.ADMIN;
        account.enabled = true;
        account.mustChangePassword = true;
        return account;
    }

    /** Re-attach a previously disabled (terminated-member) account to a freshly registered member. */
    void reissueForMember(String memberId, String passwordHash) {
        this.memberId = memberId;
        this.passwordHash = passwordHash;
        this.role = Role.MEMBER;
        this.enabled = true;
        this.mustChangePassword = true;
    }

    void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.mustChangePassword = false;
    }

    void setEnabledForStatus(boolean enabled) {
        this.enabled = enabled;
    }
}

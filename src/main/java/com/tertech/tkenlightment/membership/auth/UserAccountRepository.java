package com.tertech.tkenlightment.membership.auth;

import com.tertech.tkenlightment.membership.auth.domain.models.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserAccountRepository extends JpaRepository<UserAccountEntity, String> {

    Optional<UserAccountEntity> findByEmail(String email);

    Optional<UserAccountEntity> findByMemberId(String memberId);

    boolean existsByRole(Role role);
}

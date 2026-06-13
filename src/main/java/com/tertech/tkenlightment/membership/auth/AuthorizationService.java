package com.tertech.tkenlightment.membership.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Authorization helper referenced from {@code @PreAuthorize} SpEL as {@code @authz} on member/dues
 * controllers. Resolved by bean name at runtime, so it introduces no cross-module import.
 */
@Component("authz")
public class AuthorizationService {

    /** True when the current user is an admin or the owner of the given member record. */
    public boolean canAccessMember(String memberId) {
        AuthPrincipal principal = currentPrincipal();
        if (principal == null) {
            return false;
        }
        return switch (principal.role()) {
            case ADMIN -> true;
            case MEMBER -> memberId != null && memberId.equals(principal.memberId());
        };
    }

    private AuthPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return principal;
        }
        return null;
    }
}

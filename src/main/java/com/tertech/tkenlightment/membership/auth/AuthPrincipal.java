package com.tertech.tkenlightment.membership.auth;

import com.tertech.tkenlightment.membership.auth.domain.models.Role;

/**
 * Authenticated identity carried in the security context, built from JWT claims. {@code memberId} is
 * {@code null} for admins.
 */
public record AuthPrincipal(
        String accountId,
        String email,
        Role role,
        String memberId,
        boolean mustChangePassword) {}

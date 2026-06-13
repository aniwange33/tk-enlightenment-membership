package com.tertech.tkenlightment.membership;

import com.tertech.tkenlightment.membership.auth.AuthPrincipal;
import com.tertech.tkenlightment.membership.auth.domain.models.Role;
import com.tertech.tkenlightment.membership.auth.jwt.JwtService;

/**
 * Mints real signed JWTs for integration tests, exercising the production filter chain without
 * needing a persisted account (the filter validates signature only, never the DB).
 */
public class TestTokens {

    private final JwtService jwtService;

    public TestTokens(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String admin() {
        return jwtService.generateToken(
                new AuthPrincipal("admin-account", "admin@taraku-enlightenment.org", Role.ADMIN, null, false));
    }

    public String adminMustChangePassword() {
        return jwtService.generateToken(
                new AuthPrincipal("admin-account", "admin@taraku-enlightenment.org", Role.ADMIN, null, true));
    }

    public String member(String memberId) {
        return jwtService.generateToken(
                new AuthPrincipal("member-account-" + memberId, memberId + "@example.com", Role.MEMBER, memberId, false));
    }

    public String memberMustChangePassword(String memberId) {
        return jwtService.generateToken(
                new AuthPrincipal("member-account-" + memberId, memberId + "@example.com", Role.MEMBER, memberId, true));
    }
}

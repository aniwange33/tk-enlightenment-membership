package com.tertech.tkenlightment.membership.auth.jwt;

import com.tertech.tkenlightment.membership.auth.AuthPrincipal;
import com.tertech.tkenlightment.membership.auth.domain.models.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Issues and validates stateless HS256 JWTs. No DB access — signature + expiry only. */
@Component
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;
    private final Clock clock;

    JwtService(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.ttl:PT8H}") Duration ttl,
            Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
        this.clock = clock;
    }

    public String generateToken(AuthPrincipal principal) {
        var now = clock.instant();
        return Jwts.builder()
                .subject(principal.accountId())
                .claim("email", principal.email())
                .claim("role", principal.role().name())
                .claim("memberId", principal.memberId())
                .claim("mustChangePassword", principal.mustChangePassword())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Validates signature and expiry, returning the principal. Throws {@link io.jsonwebtoken.JwtException} on failure. */
    public AuthPrincipal parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token);
        Claims claims = jws.getPayload();
        return new AuthPrincipal(
                claims.getSubject(),
                claims.get("email", String.class),
                Role.valueOf(claims.get("role", String.class)),
                claims.get("memberId", String.class),
                Boolean.TRUE.equals(claims.get("mustChangePassword", Boolean.class)));
    }
}

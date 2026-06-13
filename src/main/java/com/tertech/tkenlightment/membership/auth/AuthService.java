package com.tertech.tkenlightment.membership.auth;

import com.tertech.tkenlightment.membership.auth.jwt.JwtService;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login and password-change — issues stateless tokens; never reveals account existence. */
@Service
@Transactional
class AuthService {

    private final UserAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    AuthService(
            UserAccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    TokenResult login(String email, String rawPassword) {
        UserAccountEntity account = accountRepository
                .findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!account.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        return new TokenResult(jwtService.generateToken(toPrincipal(account)), account.isMustChangePassword());
    }

    TokenResult changePassword(String accountId, String currentPassword, String newPassword) {
        UserAccountEntity account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        account.changePassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
        return new TokenResult(jwtService.generateToken(toPrincipal(account)), account.isMustChangePassword());
    }

    private AuthPrincipal toPrincipal(UserAccountEntity account) {
        return new AuthPrincipal(
                account.getId(),
                account.getEmail(),
                account.getRole(),
                account.getMemberId(),
                account.isMustChangePassword());
    }
}

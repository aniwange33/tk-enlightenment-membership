package com.tertech.tkenlightment.membership.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Seeds the first ADMIN from configuration on startup when no admin exists yet. */
@Component
class AdminSeeder implements ApplicationRunner {

    private final AccountService accountService;
    private final String adminEmail;
    private final String adminPassword;

    AdminSeeder(
            AccountService accountService,
            @Value("${auth.admin.email:}") String adminEmail,
            @Value("${auth.admin.password:}") String adminPassword) {
        this.accountService = accountService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (StringUtils.hasText(adminEmail) && StringUtils.hasText(adminPassword)) {
            accountService.seedAdminIfAbsent(adminEmail, adminPassword);
        }
    }
}

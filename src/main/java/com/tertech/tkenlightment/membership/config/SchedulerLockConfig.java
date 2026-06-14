package com.tertech.tkenlightment.membership.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Enables ShedLock so the {@code @Scheduled} dues jobs run on exactly one instance per fire under a
 * multi-instance deployment. Uses the existing PostgreSQL datasource as the lock store and the
 * database clock ({@code usingDbTime}) so node clock skew cannot release a lock early.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT1H")
class SchedulerLockConfig {

    @Bean
    LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
    }
}

package com.tertech.tkenlightment.membership.dues;

import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class DuesScheduler {

    private static final Logger log = LoggerFactory.getLogger(DuesScheduler.class);

    private final DuesService duesService;
    private final Clock clock;

    DuesScheduler(DuesService duesService, Clock clock) {
        this.duesService = duesService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 0 1 4 *")
    void sendDuesReminders() {
        int year = LocalDate.now(clock).getYear();
        log.info("Running scheduled dues reminders for year {}", year);
        duesService.sendDuesReminders(year);
    }

    @Scheduled(cron = "0 0 0 1 5 *")
    void inactivateUnpaidMembers() {
        int year = LocalDate.now(clock).getYear();
        log.info("Running scheduled dues inactivation for year {}", year);
        duesService.inactivateUnpaidMembers(year);
    }
}

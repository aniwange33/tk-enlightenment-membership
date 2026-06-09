package com.tertech.tkenlightment.membership.dues;

import com.tertech.tkenlightment.membership.member.domain.events.MemberRegisteredEvent;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class MemberRegisteredListener {

    private static final Logger log = LoggerFactory.getLogger(MemberRegisteredListener.class);

    private final DuesService duesService;
    private final Clock clock;

    MemberRegisteredListener(DuesService duesService, Clock clock) {
        this.duesService = duesService;
        this.clock = clock;
    }

    @ApplicationModuleListener
    void onMemberRegistered(MemberRegisteredEvent event) {
        int currentYear = LocalDate.now(clock).getYear();
        log.info("Creating dues record for new member {} for year {}", event.memberId(), currentYear);
        duesService.createDuesRecord(event.memberId(), currentYear);
    }
}

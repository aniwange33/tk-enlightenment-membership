package com.tertech.tkenlightment.membership.notification;

import com.tertech.tkenlightment.membership.dues.domain.events.DuesPaidEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.MemberAutoInactivatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class DuesEventNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(DuesEventNotificationListener.class);

    @ApplicationModuleListener
    void onDuesPaid(DuesPaidEvent event) {
        log.info(
                "[EMAIL STUB] Dues payment confirmation to {} ({}) for year {}",
                event.fullName(),
                event.email(),
                event.year());
    }

    @ApplicationModuleListener
    void onMemberAutoInactivated(MemberAutoInactivatedEvent event) {
        log.info(
                "[EMAIL STUB] Auto-inactivation notice to {} ({}) for unpaid dues year {}",
                event.fullName(),
                event.email(),
                event.year());
    }
}

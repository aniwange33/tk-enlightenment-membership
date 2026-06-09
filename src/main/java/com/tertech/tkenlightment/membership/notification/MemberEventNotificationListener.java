package com.tertech.tkenlightment.membership.notification;

import com.tertech.tkenlightment.membership.member.domain.events.MemberRegisteredEvent;
import com.tertech.tkenlightment.membership.member.domain.events.MemberStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class MemberEventNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(MemberEventNotificationListener.class);

    @ApplicationModuleListener
    void onMemberRegistered(MemberRegisteredEvent event) {
        log.info(
                "[EMAIL STUB] Welcome email to {} ({}) — membership number: {}",
                event.fullName(),
                event.email(),
                event.membershipNumber());
    }

    @ApplicationModuleListener
    void onMemberStatusChanged(MemberStatusChangedEvent event) {
        log.info(
                "[EMAIL STUB] Status change email to {} ({}) — {} → {}",
                event.fullName(),
                event.email(),
                event.oldStatus(),
                event.newStatus());
    }
}

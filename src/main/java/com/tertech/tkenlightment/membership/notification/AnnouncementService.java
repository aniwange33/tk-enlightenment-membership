package com.tertech.tkenlightment.membership.notification;

import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import com.tertech.tkenlightment.membership.notification.rest.dtos.RecipientGroup;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves an announcement's audience and hands the fan-out off to an async listener via {@link
 * AnnouncementRequestedEvent}. Returns the number of recipients targeted; actual delivery is
 * best-effort and happens after the request commits.
 */
@Service
@Transactional
class AnnouncementService {

    private final MemberAPI memberAPI;
    private final SpringEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    AnnouncementService(MemberAPI memberAPI, SpringEventPublisher eventPublisher, MeterRegistry meterRegistry) {
        this.memberAPI = memberAPI;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
    }

    int sendAnnouncement(String subject, String body, RecipientGroup group) {
        List<String> recipients = resolveRecipients(group).stream()
                .map(MemberResult::email)
                .toList();

        if (!recipients.isEmpty()) {
            eventPublisher.publish(new AnnouncementRequestedEvent(subject, body, recipients));
        }
        meterRegistry.counter("announcements.sent", "group", group.name()).increment();
        meterRegistry.counter("announcements.recipients", "group", group.name()).increment(recipients.size());
        return recipients.size();
    }

    private List<MemberResult> resolveRecipients(RecipientGroup group) {
        return switch (group) {
            case ACTIVE_MEMBERS -> memberAPI.findActiveMembers();
                // "All members" excludes ex-members who have left the club (TERMINATED).
            case ALL_MEMBERS -> memberAPI.findNonTerminatedMembers();
        };
    }
}

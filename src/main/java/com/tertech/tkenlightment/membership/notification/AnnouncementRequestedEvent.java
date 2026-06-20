package com.tertech.tkenlightment.membership.notification;

import com.tertech.tkenlightment.membership.shared.domain.events.DomainEvent;
import java.util.List;

/**
 * Published when an admin sends an announcement. Carries the resolved recipient list so the email
 * fan-out happens asynchronously, after the request has returned.
 */
record AnnouncementRequestedEvent(String subject, String body, List<String> recipientEmails)
        implements DomainEvent {}

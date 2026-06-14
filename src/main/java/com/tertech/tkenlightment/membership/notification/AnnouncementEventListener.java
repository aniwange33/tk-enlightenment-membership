package com.tertech.tkenlightment.membership.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Fans an announcement out to every recipient, best-effort: a failed send is logged, not fatal. */
@Component
class AnnouncementEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementEventListener.class);

    private final EmailService emailService;

    AnnouncementEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @ApplicationModuleListener
    void onAnnouncementRequested(AnnouncementRequestedEvent event) {
        String body = brandedBody(event.body());
        for (String email : event.recipientEmails()) {
            try {
                emailService.send(email, event.subject(), body);
            } catch (Exception e) {
                log.error("Failed to send announcement to {}", email, e);
            }
        }
    }

    /** Wraps the admin's message in the club's greeting and sign-off, matching the other emails. */
    private String brandedBody(String message) {
        return """
                Dear Member,

                %s

                Warm regards,
                Taraku Enlightenment Club"""
                .formatted(message);
    }
}

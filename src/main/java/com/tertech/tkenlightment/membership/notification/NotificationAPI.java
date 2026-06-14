package com.tertech.tkenlightment.membership.notification;

import com.tertech.tkenlightment.membership.notification.rest.dtos.RecipientGroup;
import org.springframework.stereotype.Component;

/** Public entry point into the notification module. */
@Component
public class NotificationAPI {

    private final AnnouncementService announcementService;

    NotificationAPI(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /** Sends an announcement to the given audience; returns the number of recipients targeted. */
    public int sendAnnouncement(String subject, String body, RecipientGroup recipientGroup) {



        return announcementService.sendAnnouncement(subject, body, recipientGroup);
    }
}

package com.tertech.tkenlightment.membership.notification.rest.controllers;

import com.tertech.tkenlightment.membership.notification.NotificationAPI;
import com.tertech.tkenlightment.membership.notification.rest.dtos.AnnouncementRequest;
import com.tertech.tkenlightment.membership.notification.rest.dtos.AnnouncementResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/announcements")
class AnnouncementController {

    private final NotificationAPI notificationAPI;

    AnnouncementController(NotificationAPI notificationAPI) {
        this.notificationAPI = notificationAPI;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    AnnouncementResponse send(@Valid @RequestBody AnnouncementRequest request) {
        int recipientCount =
                notificationAPI.sendAnnouncement(request.subject(), request.body(), request.recipientGroup());
        return new AnnouncementResponse(recipientCount);
    }
}

package com.tertech.tkenlightment.membership.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notification")
public record NotificationProperties(String from, String resetLinkBaseUrl) {

    public NotificationProperties {
        if (from == null || from.isBlank()) {
            from = "noreply@taraku-enlightenment.org";
        }
        if (resetLinkBaseUrl == null || resetLinkBaseUrl.isBlank()) {
            resetLinkBaseUrl = "http://localhost:8080/reset-password";
        }
    }
}

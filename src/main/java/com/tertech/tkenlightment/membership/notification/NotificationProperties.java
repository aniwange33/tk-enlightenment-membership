package com.tertech.tkenlightment.membership.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notification")
public record NotificationProperties(String from) {

    public NotificationProperties {
        if (from == null || from.isBlank()) {
            from = "noreply@taraku-enlightenment.org";
        }
    }
}

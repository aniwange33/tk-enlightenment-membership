package com.tertech.tkenlightment.membership.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notification")
public record NotificationProperties(String from, String appBaseUrl) {

    public NotificationProperties {
        if (from == null || from.isBlank()) {
            from = "noreply@taraku-enlightenment.org";
        }
        if (appBaseUrl == null || appBaseUrl.isBlank()) {
            appBaseUrl = "http://localhost:8080";
        }
        if (appBaseUrl.endsWith("/")) {
            appBaseUrl = appBaseUrl.substring(0, appBaseUrl.length() - 1);
        }
    }

    /** Where members sign in (the SPA login route). */
    public String loginUrl() {
        return appBaseUrl + "/login";
    }

    /** The password-reset SPA route carrying the one-time token. */
    public String resetUrl(String token) {
        return appBaseUrl + "/reset?token=" + token;
    }
}

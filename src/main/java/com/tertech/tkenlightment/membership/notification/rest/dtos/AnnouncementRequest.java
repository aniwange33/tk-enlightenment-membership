package com.tertech.tkenlightment.membership.notification.rest.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnnouncementRequest(
        @NotBlank String subject,
        @NotBlank String body,
        @NotNull RecipientGroup recipientGroup) {}

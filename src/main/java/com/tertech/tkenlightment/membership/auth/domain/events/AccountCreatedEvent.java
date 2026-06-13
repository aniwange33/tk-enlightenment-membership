package com.tertech.tkenlightment.membership.auth.domain.events;

import com.tertech.tkenlightment.membership.shared.domain.events.DomainEvent;

/**
 * Published when an account is provisioned for a newly registered member. Carries the one-time
 * temporary password so the notification module can deliver real working credentials in the welcome
 * email.
 */
public record AccountCreatedEvent(
        String email,
        String fullName,
        String membershipNumber,
        String tempPassword) implements DomainEvent {}

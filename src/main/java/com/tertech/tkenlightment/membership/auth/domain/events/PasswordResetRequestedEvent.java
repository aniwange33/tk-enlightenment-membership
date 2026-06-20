package com.tertech.tkenlightment.membership.auth.domain.events;

import com.tertech.tkenlightment.membership.shared.domain.events.DomainEvent;

/**
 * Published when a user requests a password reset. Carries the one-time raw token so the
 * notification module can build a working reset link; the token is never persisted in raw form.
 */
public record PasswordResetRequestedEvent(String email, String rawToken) implements DomainEvent {}

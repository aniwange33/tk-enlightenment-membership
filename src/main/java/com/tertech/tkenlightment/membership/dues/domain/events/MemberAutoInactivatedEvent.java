package com.tertech.tkenlightment.membership.dues.domain.events;

import com.tertech.tkenlightment.membership.shared.domain.events.DomainEvent;

public record MemberAutoInactivatedEvent(
        String memberId,
        String email,
        String fullName,
        int year) implements DomainEvent {}

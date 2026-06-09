package com.tertech.tkenlightment.membership.member.domain.events;

import com.tertech.tkenlightment.membership.shared.domain.events.DomainEvent;

public record MemberStatusChangedEvent(
        String memberId,
        String email,
        String fullName,
        String oldStatus,
        String newStatus) implements DomainEvent {}

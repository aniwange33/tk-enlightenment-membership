package com.tertech.tkenlightment.membership.member.domain.events;

import com.tertech.tkenlightment.membership.shared.domain.events.DomainEvent;

public record MemberProfileUpdatedEvent(
        String memberId,
        String email) implements DomainEvent {}

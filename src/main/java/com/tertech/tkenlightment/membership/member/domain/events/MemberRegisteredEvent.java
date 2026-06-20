package com.tertech.tkenlightment.membership.member.domain.events;

import com.tertech.tkenlightment.membership.shared.domain.events.DomainEvent;

public record MemberRegisteredEvent(
        String memberId,
        String membershipNumber,
        String email,
        String fullName) implements DomainEvent {}

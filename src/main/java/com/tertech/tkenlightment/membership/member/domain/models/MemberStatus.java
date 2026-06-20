package com.tertech.tkenlightment.membership.member.domain.models;

import java.util.Set;

public enum MemberStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    TERMINATED;

    private static final Set<MemberStatus> ACTIVE_TRANSITIONS = Set.of(INACTIVE, SUSPENDED, TERMINATED);
    private static final Set<MemberStatus> INACTIVE_TRANSITIONS = Set.of(ACTIVE, SUSPENDED, TERMINATED);
    private static final Set<MemberStatus> SUSPENDED_TRANSITIONS = Set.of(ACTIVE, TERMINATED);

    public boolean canTransitionTo(MemberStatus target) {
        return switch (this) {
            case ACTIVE -> ACTIVE_TRANSITIONS.contains(target);
            case INACTIVE -> INACTIVE_TRANSITIONS.contains(target);
            case SUSPENDED -> SUSPENDED_TRANSITIONS.contains(target);
            case TERMINATED -> false;
        };
    }
}

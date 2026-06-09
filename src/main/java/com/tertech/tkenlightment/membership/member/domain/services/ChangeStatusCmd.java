package com.tertech.tkenlightment.membership.member.domain.services;

import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;

public record ChangeStatusCmd(
        String memberId,
        MemberStatus newStatus) {}

package com.tertech.tkenlightment.membership.member.domain.services;

public record UpdateMemberProfileCmd(
        String memberId,
        String phone,
        String address) {}

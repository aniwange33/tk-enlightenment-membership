package com.tertech.tkenlightment.membership.member.rest.dtos;

public record UpdateMemberProfileRequest(
        String phone,
        String address) {}

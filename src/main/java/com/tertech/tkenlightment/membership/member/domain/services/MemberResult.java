package com.tertech.tkenlightment.membership.member.domain.services;

import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import java.time.LocalDate;

public record MemberResult(
        String id,
        String membershipNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        String address,
        LocalDate joinDate,
        MemberStatus status) {}

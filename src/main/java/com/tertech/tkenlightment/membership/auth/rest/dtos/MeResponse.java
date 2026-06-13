package com.tertech.tkenlightment.membership.auth.rest.dtos;

import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import java.time.LocalDate;

public record MeResponse(
        String id,
        String membershipNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        String address,
        LocalDate joinDate,
        MemberStatus status) {

    public static MeResponse from(MemberResult result) {
        return new MeResponse(
                result.id(),
                result.membershipNumber(),
                result.firstName(),
                result.lastName(),
                result.dateOfBirth(),
                result.email(),
                result.phone(),
                result.address(),
                result.joinDate(),
                result.status());
    }
}

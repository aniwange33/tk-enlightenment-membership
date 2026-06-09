package com.tertech.tkenlightment.membership.member;

import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import org.springframework.stereotype.Component;

@Component
class MemberMapper {

    MemberResult toResult(MemberEntity entity) {
        return new MemberResult(
                entity.getId().value(),
                entity.getMembershipNumber(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getDateOfBirth(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getJoinDate(),
                entity.getStatus());
    }
}

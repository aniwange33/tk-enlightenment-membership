package com.tertech.tkenlightment.membership.member.rest.dtos;

import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull(message = "Status is required") MemberStatus status) {}

package com.tertech.tkenlightment.membership.dues.rest.dtos;

import java.time.LocalDate;

public record DuesRecordResponse(
        String id,
        String memberId,
        int year,
        boolean paid,
        LocalDate paidDate) {}

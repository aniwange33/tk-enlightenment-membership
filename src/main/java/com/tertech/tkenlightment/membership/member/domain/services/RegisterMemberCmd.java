package com.tertech.tkenlightment.membership.member.domain.services;

import java.time.LocalDate;

public record RegisterMemberCmd(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        String address) {}

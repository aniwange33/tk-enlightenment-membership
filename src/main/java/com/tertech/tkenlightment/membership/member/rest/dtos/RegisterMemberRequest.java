package com.tertech.tkenlightment.membership.member.rest.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RegisterMemberRequest(
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Last name is required") String lastName,
        @NotNull(message = "Date of birth is required") LocalDate dateOfBirth,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        String phone,
        String address) {}

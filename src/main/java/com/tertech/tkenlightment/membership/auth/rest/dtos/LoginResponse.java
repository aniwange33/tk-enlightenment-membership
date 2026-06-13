package com.tertech.tkenlightment.membership.auth.rest.dtos;

public record LoginResponse(String token, boolean mustChangePassword) {}

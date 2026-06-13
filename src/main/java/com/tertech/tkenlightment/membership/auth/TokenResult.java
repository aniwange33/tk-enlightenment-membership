package com.tertech.tkenlightment.membership.auth;

/** Internal carrier for a freshly issued token and the current must-change flag. */
record TokenResult(String token, boolean mustChangePassword) {}

package com.tertech.tkenlightment.membership.shared.domain.models;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {}

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}

package com.tertech.tkenlightment.membership.member.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record MemberId(
        @Column(name = "id") String value) implements Serializable {

    public MemberId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MemberId must not be blank");
        }
    }
}

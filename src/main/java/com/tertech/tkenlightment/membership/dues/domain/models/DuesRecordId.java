package com.tertech.tkenlightment.membership.dues.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record DuesRecordId(
        @Column(name = "id") String value) implements Serializable {

    public DuesRecordId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DuesRecordId must not be blank");
        }
    }
}

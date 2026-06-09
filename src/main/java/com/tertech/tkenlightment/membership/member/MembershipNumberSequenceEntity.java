package com.tertech.tkenlightment.membership.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "membership_number_sequence")
@Getter
@Setter
@NoArgsConstructor
class MembershipNumberSequenceEntity {

    @Id
    @Column(name = "year")
    private Integer year;

    @Column(name = "next_value", nullable = false)
    private Integer nextValue;

    MembershipNumberSequenceEntity(Integer year) {
        this.year = year;
        this.nextValue = 1;
    }

    String generateAndIncrement() {
        String number = String.format("TEC-%d-%03d", year, nextValue);
        nextValue++;
        return number;
    }
}

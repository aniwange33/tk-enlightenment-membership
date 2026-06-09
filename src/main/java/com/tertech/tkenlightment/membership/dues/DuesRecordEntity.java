package com.tertech.tkenlightment.membership.dues;

import com.tertech.tkenlightment.membership.dues.domain.models.DuesRecordId;
import com.tertech.tkenlightment.membership.shared.domain.models.BaseEntity;
import com.tertech.tkenlightment.membership.shared.domain.models.IdGenerator;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dues_records")
@Getter
@Setter
@NoArgsConstructor
class DuesRecordEntity extends BaseEntity {

    @EmbeddedId
    private DuesRecordId id;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "paid", nullable = false)
    private Boolean paid;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    static DuesRecordEntity create(String memberId, int year) {
        var record = new DuesRecordEntity();
        record.setId(new DuesRecordId(IdGenerator.newId()));
        record.setMemberId(memberId);
        record.setYear(year);
        record.setPaid(false);
        return record;
    }

    void markPaid(LocalDate paidDate) {
        this.paid = true;
        this.paidDate = paidDate;
    }
}

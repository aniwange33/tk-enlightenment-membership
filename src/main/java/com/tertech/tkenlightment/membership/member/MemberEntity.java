package com.tertech.tkenlightment.membership.member;

import com.tertech.tkenlightment.membership.member.domain.models.MemberId;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.DomainException;
import com.tertech.tkenlightment.membership.shared.domain.models.BaseEntity;
import com.tertech.tkenlightment.membership.shared.domain.models.IdGenerator;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
class MemberEntity extends BaseEntity {

    @EmbeddedId
    private MemberId id;

    @Column(name = "membership_number", nullable = false, unique = true)
    private String membershipNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MemberStatus status;

    static MemberEntity create(
            String membershipNumber,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String email,
            String phone,
            String address,
            LocalDate joinDate) {
        var member = new MemberEntity();
        member.setId(new MemberId(IdGenerator.newId()));
        member.setMembershipNumber(membershipNumber);
        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setDateOfBirth(dateOfBirth);
        member.setEmail(email);
        member.setPhone(phone);
        member.setAddress(address);
        member.setJoinDate(joinDate);
        member.setStatus(MemberStatus.ACTIVE);
        return member;
    }

    void changeStatus(MemberStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new DomainException(
                    "Cannot transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    void updateProfile(String phone, String address) {
        this.phone = phone;
        this.address = address;
    }

    String fullName() {
        return firstName + " " + lastName;
    }
}

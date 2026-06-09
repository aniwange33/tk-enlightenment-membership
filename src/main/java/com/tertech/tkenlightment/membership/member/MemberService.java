package com.tertech.tkenlightment.membership.member;

import com.tertech.tkenlightment.membership.member.domain.events.MemberProfileUpdatedEvent;
import com.tertech.tkenlightment.membership.member.domain.events.MemberRegisteredEvent;
import com.tertech.tkenlightment.membership.member.domain.events.MemberStatusChangedEvent;
import com.tertech.tkenlightment.membership.member.domain.models.MemberId;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.ChangeStatusCmd;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import com.tertech.tkenlightment.membership.member.domain.services.RegisterMemberCmd;
import com.tertech.tkenlightment.membership.member.domain.services.UpdateMemberProfileCmd;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.MemberAlreadyExistsException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class MemberService {

    private final MemberRepository memberRepository;
    private final MembershipNumberSequenceRepository sequenceRepository;
    private final MemberMapper mapper;
    private final SpringEventPublisher eventPublisher;
    private final Clock clock;

    MemberService(
            MemberRepository memberRepository,
            MembershipNumberSequenceRepository sequenceRepository,
            MemberMapper mapper,
            SpringEventPublisher eventPublisher,
            Clock clock) {
        this.memberRepository = memberRepository;
        this.sequenceRepository = sequenceRepository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    MemberResult register(RegisterMemberCmd cmd) {
        if (memberRepository.existsByEmail(cmd.email())) {
            throw new MemberAlreadyExistsException(cmd.email());
        }

        LocalDate today = LocalDate.now(clock);
        String membershipNumber = generateMembershipNumber(today.getYear());

        MemberEntity member = MemberEntity.create(
                membershipNumber,
                cmd.firstName(),
                cmd.lastName(),
                cmd.dateOfBirth(),
                cmd.email(),
                cmd.phone(),
                cmd.address(),
                today);

        member = memberRepository.save(member);

        eventPublisher.publish(new MemberRegisteredEvent(
                member.getId().value(),
                member.getMembershipNumber(),
                member.getEmail(),
                member.fullName()));

        return mapper.toResult(member);
    }

    @Transactional(readOnly = true)
    MemberResult getById(String memberId) {
        MemberEntity member = memberRepository.getById(new MemberId(memberId));
        return mapper.toResult(member);
    }

    @Transactional(readOnly = true)
    Page<MemberResult> search(String query, MemberStatus status, Pageable pageable) {
        return memberRepository.search(query, status, pageable).map(mapper::toResult);
    }

    @Transactional(readOnly = true)
    List<MemberResult> findActiveMembers() {
        return memberRepository.findActiveMembers().stream()
                .map(mapper::toResult)
                .toList();
    }

    MemberResult changeStatus(ChangeStatusCmd cmd) {
        MemberEntity member = memberRepository.getById(new MemberId(cmd.memberId()));
        String oldStatus = member.getStatus().name();
        member.changeStatus(cmd.newStatus());
        member = memberRepository.save(member);

        eventPublisher.publish(new MemberStatusChangedEvent(
                member.getId().value(),
                member.getEmail(),
                member.fullName(),
                oldStatus,
                cmd.newStatus().name()));

        return mapper.toResult(member);
    }

    MemberResult updateProfile(UpdateMemberProfileCmd cmd) {
        MemberEntity member = memberRepository.getById(new MemberId(cmd.memberId()));
        member.updateProfile(cmd.phone(), cmd.address());
        member = memberRepository.save(member);

        eventPublisher.publish(new MemberProfileUpdatedEvent(
                member.getId().value(), member.getEmail()));

        return mapper.toResult(member);
    }

    MemberResult inactivateMember(String memberId) {
        return changeStatus(new ChangeStatusCmd(memberId, MemberStatus.INACTIVE));
    }

    MemberResult activateMember(String memberId) {
        return changeStatus(new ChangeStatusCmd(memberId, MemberStatus.ACTIVE));
    }

    private String generateMembershipNumber(int year) {
        MembershipNumberSequenceEntity sequence = sequenceRepository
                .findByYearForUpdate(year)
                .orElseGet(() -> sequenceRepository.save(new MembershipNumberSequenceEntity(year)));

        String number = sequence.generateAndIncrement();
        sequenceRepository.save(sequence);
        return number;
    }
}

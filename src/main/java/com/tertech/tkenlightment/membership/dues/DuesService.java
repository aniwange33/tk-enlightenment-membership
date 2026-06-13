package com.tertech.tkenlightment.membership.dues;

import com.tertech.tkenlightment.membership.dues.domain.events.DuesPaidEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.DuesReminderEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.MemberAutoInactivatedEvent;
import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.DomainException;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DuesService {

    private static final Logger log = LoggerFactory.getLogger(DuesService.class);

    private final DuesRecordRepository duesRepository;
    private final MemberAPI memberAPI;
    private final SpringEventPublisher eventPublisher;
    private final Clock clock;

    DuesService(
            DuesRecordRepository duesRepository,
            MemberAPI memberAPI,
            SpringEventPublisher eventPublisher,
            Clock clock) {
        this.duesRepository = duesRepository;
        this.memberAPI = memberAPI;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    void createDuesRecord(String memberId, int year) {
        if (duesRepository.findByMemberIdAndYear(memberId, year).isPresent()) {
            log.debug("Dues record already exists for member {} and year {}", memberId, year);
            return;
        }
        duesRepository.save(DuesRecordEntity.create(memberId, year));
    }

    /**
     * Creates an unpaid dues record for the given year for every ACTIVE member that does not already
     * have one. Idempotent (safe to re-run) — without this, the April/May jobs only ever see members
     * registered in the current year.
     */
    void generateDuesForYear(int year) {
        List<MemberResult> activeMembers = memberAPI.findActiveMembers();
        log.info("Generating dues records for {} active members for year {}", activeMembers.size(), year);
        for (MemberResult member : activeMembers) {
            createDuesRecord(member.id(), year);
        }
    }

    DuesRecordEntity markPaid(String memberId, int year) {
        DuesRecordEntity record = duesRepository
                .findByMemberIdAndYear(memberId, year)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dues record not found for member " + memberId + " and year " + year));

        if (record.getPaid()) {
            throw new DomainException("Dues for year " + year + " are already paid");
        }

        MemberResult member = memberAPI.getMember(memberId);
        if (member.status() == MemberStatus.TERMINATED) {
            throw new DomainException("Cannot record dues for a terminated member");
        }

        record.markPaid(LocalDate.now(clock));
        record = duesRepository.save(record);

        if (member.status() == MemberStatus.INACTIVE) {
            memberAPI.activateMember(memberId);
        }

        eventPublisher.publish(new DuesPaidEvent(memberId, member.email(), member.firstName() + " " + member.lastName(), year));

        return record;
    }

    @Transactional(readOnly = true)
    List<DuesRecordEntity> getDuesHistory(String memberId) {
        return duesRepository.findByMemberIdOrderByYearDesc(memberId);
    }

    void sendDuesReminders(int year) {
        List<DuesRecordEntity> unpaidRecords = duesRepository.findUnpaidForYear(year);
        log.info("Sending dues reminders for year {} to {} unpaid members", year, unpaidRecords.size());

        for (DuesRecordEntity record : unpaidRecords) {
            try {
                MemberResult member = memberAPI.getMember(record.getMemberId());
                if (member.status() == MemberStatus.ACTIVE) {
                    eventPublisher.publish(new DuesReminderEvent(
                            record.getMemberId(),
                            member.email(),
                            member.firstName() + " " + member.lastName(),
                            year));
                }
            } catch (Exception e) {
                log.error("Failed to send dues reminder to member {} for year {}", record.getMemberId(), year, e);
            }
        }
    }

    void inactivateUnpaidMembers(int year) {
        List<DuesRecordEntity> unpaidRecords = duesRepository.findUnpaidForYear(year);
        log.info("Found {} unpaid dues records for year {}", unpaidRecords.size(), year);

        for (DuesRecordEntity record : unpaidRecords) {
            try {
                MemberResult member = memberAPI.getMember(record.getMemberId());
                if (member.status() == MemberStatus.ACTIVE) {
                    memberAPI.inactivateMember(record.getMemberId());
                    eventPublisher.publish(new MemberAutoInactivatedEvent(
                            record.getMemberId(),
                            member.email(),
                            member.firstName() + " " + member.lastName(),
                            year));
                    log.info("Auto-inactivated member {} for unpaid dues year {}", record.getMemberId(), year);
                }
            } catch (Exception e) {
                log.error("Failed to inactivate member {} for year {}", record.getMemberId(), year, e);
            }
        }
    }
}

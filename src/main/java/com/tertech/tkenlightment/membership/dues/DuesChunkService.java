package com.tertech.tkenlightment.membership.dues;

import com.tertech.tkenlightment.membership.dues.domain.events.DuesReminderEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.MemberAutoInactivatedEvent;
import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional worker for the dues batch jobs: each public method processes one chunk in its own
 * transaction (invoked from the non-transactional orchestrator in {@link DuesService}). This keeps a
 * long sweep from holding a single transaction open and makes partial progress durable at chunk
 * granularity. Per-record {@code try/catch} keeps one bad member from skipping its chunk-mates.
 */
@Service
@Transactional
class DuesChunkService {

    private static final Logger log = LoggerFactory.getLogger(DuesChunkService.class);

    private final DuesRecordRepository duesRepository;
    private final MemberAPI memberAPI;
    private final SpringEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    DuesChunkService(
            DuesRecordRepository duesRepository,
            MemberAPI memberAPI,
            SpringEventPublisher eventPublisher,
            MeterRegistry meterRegistry) {
        this.duesRepository = duesRepository;
        this.memberAPI = memberAPI;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
    }

    /** Idempotent: creates an unpaid record only if the member has none for the year. */
    void createDuesRecord(String memberId, int year) {
        if (duesRepository.findByMemberIdAndYear(memberId, year).isPresent()) {
            log.debug("Dues record already exists for member {} and year {}", memberId, year);
            return;
        }
        duesRepository.save(DuesRecordEntity.create(memberId, year));
        meterRegistry.counter("dues.records_generated").increment();
    }

    void createDuesRecordsForChunk(List<String> memberIds, int year) {
        for (String memberId : memberIds) {
            createDuesRecord(memberId, year);
        }
    }

    void sendRemindersForChunk(List<String> memberIds, int year) {
        for (String memberId : memberIds) {
            try {
                MemberResult member = memberAPI.getMember(memberId);
                if (member.status() == MemberStatus.ACTIVE) {
                    eventPublisher.publish(new DuesReminderEvent(
                            memberId, member.email(), member.firstName() + " " + member.lastName(), year));
                    meterRegistry.counter("dues.reminders_sent").increment();
                }
            } catch (Exception e) {
                log.error("Failed to send dues reminder to member {} for year {}", memberId, year, e);
            }
        }
    }

    void inactivateForChunk(List<String> memberIds, int year) {
        for (String memberId : memberIds) {
            try {
                MemberResult member = memberAPI.getMember(memberId);
                if (member.status() == MemberStatus.ACTIVE) {
                    memberAPI.inactivateMember(memberId);
                    eventPublisher.publish(new MemberAutoInactivatedEvent(
                            memberId, member.email(), member.firstName() + " " + member.lastName(), year));
                    meterRegistry.counter("members.auto_inactivated").increment();
                    log.info("Auto-inactivated member {} for unpaid dues year {}", memberId, year);
                }
            } catch (Exception e) {
                log.error("Failed to inactivate member {} for year {}", memberId, year, e);
            }
        }
    }
}

package com.tertech.tkenlightment.membership.dues;

import com.tertech.tkenlightment.membership.dues.domain.events.DuesPaidEvent;
import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.DomainException;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DuesService {

    private static final Logger log = LoggerFactory.getLogger(DuesService.class);

    private final DuesRecordRepository duesRepository;
    private final MemberAPI memberAPI;
    private final SpringEventPublisher eventPublisher;
    private final DuesChunkService duesChunkService;
    private final Clock clock;
    private final int chunkSize;

    DuesService(
            DuesRecordRepository duesRepository,
            MemberAPI memberAPI,
            SpringEventPublisher eventPublisher,
            DuesChunkService duesChunkService,
            Clock clock,
            @Value("${dues.batch.chunk-size:500}") int chunkSize) {
        this.duesRepository = duesRepository;
        this.memberAPI = memberAPI;
        this.eventPublisher = eventPublisher;
        this.duesChunkService = duesChunkService;
        this.clock = clock;
        this.chunkSize = chunkSize;
    }

    void createDuesRecord(String memberId, int year) {
        duesChunkService.createDuesRecord(memberId, year);
    }

    /**
     * Creates an unpaid dues record for the given year for every ACTIVE member that does not already
     * have one. Idempotent — without this, the April/May jobs only ever see members registered in the
     * current year. Runs outside a transaction and commits one transaction per chunk.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void generateDuesForYear(int year) {
        log.info("Generating dues records for active members for year {}", year);
        int page = 0;
        Page<MemberResult> members;
        do {
            members = memberAPI.listMembers(null, MemberStatus.ACTIVE, PageRequest.of(page, chunkSize));
            List<String> memberIds =
                    members.getContent().stream().map(MemberResult::id).toList();
            if (!memberIds.isEmpty()) {
                runChunk(() -> duesChunkService.createDuesRecordsForChunk(memberIds, year), "generate dues", year);
            }
            page++;
        } while (members.hasNext());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void sendDuesReminders(int year) {
        log.info("Sending dues reminders for year {}", year);
        forEachUnpaidChunk(year, memberIds -> duesChunkService.sendRemindersForChunk(memberIds, year), "send dues reminders");
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void inactivateUnpaidMembers(int year) {
        log.info("Inactivating unpaid members for year {}", year);
        forEachUnpaidChunk(year, memberIds -> duesChunkService.inactivateForChunk(memberIds, year), "inactivate unpaid members");
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

    /** Pages the unpaid records for the year and hands each page's member ids to {@code chunk}. */
    private void forEachUnpaidChunk(int year, Consumer<List<String>> chunk, String label) {
        int page = 0;
        Page<DuesRecordEntity> records;
        do {
            records = duesRepository.findByYearAndPaidFalse(year, PageRequest.of(page, chunkSize, Sort.by("memberId")));
            List<String> memberIds =
                    records.getContent().stream().map(DuesRecordEntity::getMemberId).toList();
            if (!memberIds.isEmpty()) {
                runChunk(() -> chunk.accept(memberIds), label, year);
            }
            page++;
        } while (records.hasNext());
    }

    /** Runs one chunk's transactional work; a failed chunk is logged so the sweep continues. */
    private void runChunk(Runnable chunk, String label, int year) {
        try {
            chunk.run();
        } catch (Exception e) {
            log.error("Chunk failed during {} for year {}", label, year, e);
        }
    }
}

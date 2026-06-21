package com.tertech.tkenlightment.membership.dues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tertech.tkenlightment.membership.dues.domain.events.DuesPaidEvent;
import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.DomainException;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.ResourceNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class DuesServiceTest {

    private static final int CHUNK_SIZE = 2;

    @Mock
    DuesRecordRepository duesRepository;

    @Mock
    MemberAPI memberAPI;

    @Mock
    SpringEventPublisher eventPublisher;

    @Mock
    DuesChunkService duesChunkService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-04-15T00:00:00Z"), ZoneOffset.UTC);
    private DuesService duesService;

    @BeforeEach
    void setUp() {
        duesService = new DuesService(
                duesRepository, memberAPI, eventPublisher, duesChunkService, clock, CHUNK_SIZE, new SimpleMeterRegistry());
    }

    // --- createDuesRecord (delegation) ---

    @Test
    void createDuesRecordDelegatesToChunkService() {
        duesService.createDuesRecord("m1", 2026);

        verify(duesChunkService).createDuesRecord("m1", 2026);
    }

    // --- generateDuesForYear (paging orchestration) ---

    @Test
    void generatePagesActiveMembersAndDelegatesEachChunk() {
        Pageable first = PageRequest.of(0, CHUNK_SIZE);
        Pageable second = PageRequest.of(1, CHUNK_SIZE);
        when(memberAPI.listMembers(isNull(), eq(MemberStatus.ACTIVE), eq(first)))
                .thenReturn(new PageImpl<>(List.of(member("m1"), member("m2")), first, 3));
        when(memberAPI.listMembers(isNull(), eq(MemberStatus.ACTIVE), eq(second)))
                .thenReturn(new PageImpl<>(List.of(member("m3")), second, 3));

        duesService.generateDuesForYear(2026);

        verify(duesChunkService).createDuesRecordsForChunk(List.of("m1", "m2"), 2026);
        verify(duesChunkService).createDuesRecordsForChunk(List.of("m3"), 2026);
    }

    @Test
    void generateContinuesToNextChunkWhenOneChunkFails() {
        Pageable first = PageRequest.of(0, CHUNK_SIZE);
        Pageable second = PageRequest.of(1, CHUNK_SIZE);
        when(memberAPI.listMembers(isNull(), eq(MemberStatus.ACTIVE), eq(first)))
                .thenReturn(new PageImpl<>(List.of(member("m1"), member("m2")), first, 3));
        when(memberAPI.listMembers(isNull(), eq(MemberStatus.ACTIVE), eq(second)))
                .thenReturn(new PageImpl<>(List.of(member("m3")), second, 3));
        doThrow(new RuntimeException("chunk boom"))
                .when(duesChunkService)
                .createDuesRecordsForChunk(List.of("m1", "m2"), 2026);

        duesService.generateDuesForYear(2026);

        // Second chunk still runs despite the first chunk failing.
        verify(duesChunkService).createDuesRecordsForChunk(List.of("m3"), 2026);
    }

    // --- sendDuesReminders / inactivateUnpaidMembers (paging orchestration) ---

    @Test
    void sendRemindersPagesUnpaidRecordsAndDelegatesEachChunk() {
        stubTwoUnpaidPages();

        duesService.sendDuesReminders(2026);

        verify(duesChunkService).sendRemindersForChunk(List.of("m1", "m2"), 2026);
        verify(duesChunkService).sendRemindersForChunk(List.of("m3"), 2026);
    }

    @Test
    void inactivatePagesUnpaidRecordsAndDelegatesEachChunk() {
        stubTwoUnpaidPages();

        duesService.inactivateUnpaidMembers(2026);

        verify(duesChunkService).inactivateForChunk(List.of("m1", "m2"), 2026);
        verify(duesChunkService).inactivateForChunk(List.of("m3"), 2026);
    }

    private void stubTwoUnpaidPages() {
        Pageable first = PageRequest.of(0, CHUNK_SIZE, Sort.by("memberId"));
        Pageable second = PageRequest.of(1, CHUNK_SIZE, Sort.by("memberId"));
        when(duesRepository.findByYearAndPaidFalse(2026, first))
                .thenReturn(new PageImpl<>(
                        List.of(DuesRecordEntity.create("m1", 2026), DuesRecordEntity.create("m2", 2026)), first, 3));
        when(duesRepository.findByYearAndPaidFalse(2026, second))
                .thenReturn(new PageImpl<>(List.of(DuesRecordEntity.create("m3", 2026)), second, 3));
    }

    // --- markPaid ---

    @Test
    void marksDuesPaidAndPublishesEvent() {
        DuesRecordEntity unpaid = DuesRecordEntity.create("m1", 2026);
        when(duesRepository.findByMemberIdAndYear("m1", 2026)).thenReturn(Optional.of(unpaid));
        when(duesRepository.save(unpaid)).thenReturn(unpaid);
        when(memberAPI.getMember("m1")).thenReturn(member("m1", MemberStatus.ACTIVE));

        DuesRecordEntity result = duesService.markPaid("m1", 2026);

        assertThat(result.getPaid()).isTrue();
        assertThat(result.getPaidDate()).isEqualTo(LocalDate.of(2026, 4, 15));
        verify(eventPublisher).publish(any(DuesPaidEvent.class));
    }

    @Test
    void reactivatesInactiveMemberOnPayment() {
        DuesRecordEntity unpaid = DuesRecordEntity.create("m1", 2026);
        when(duesRepository.findByMemberIdAndYear("m1", 2026)).thenReturn(Optional.of(unpaid));
        when(duesRepository.save(unpaid)).thenReturn(unpaid);
        when(memberAPI.getMember("m1")).thenReturn(member("m1", MemberStatus.INACTIVE));

        duesService.markPaid("m1", 2026);

        verify(memberAPI).activateMember("m1");
    }

    @Test
    void doesNotReactivateActiveMemberOnPayment() {
        DuesRecordEntity unpaid = DuesRecordEntity.create("m1", 2026);
        when(duesRepository.findByMemberIdAndYear("m1", 2026)).thenReturn(Optional.of(unpaid));
        when(duesRepository.save(unpaid)).thenReturn(unpaid);
        when(memberAPI.getMember("m1")).thenReturn(member("m1", MemberStatus.ACTIVE));

        duesService.markPaid("m1", 2026);

        verify(memberAPI, never()).activateMember(any());
    }

    @Test
    void rejectsPaymentForTerminatedMember() {
        DuesRecordEntity unpaid = DuesRecordEntity.create("m1", 2026);
        when(duesRepository.findByMemberIdAndYear("m1", 2026)).thenReturn(Optional.of(unpaid));
        when(memberAPI.getMember("m1")).thenReturn(member("m1", MemberStatus.TERMINATED));

        assertThatThrownBy(() -> duesService.markPaid("m1", 2026))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("terminated");

        verify(duesRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void rejectsPaymentWhenAlreadyPaid() {
        DuesRecordEntity paid = DuesRecordEntity.create("m1", 2026);
        paid.markPaid(LocalDate.of(2026, 2, 1));
        when(duesRepository.findByMemberIdAndYear("m1", 2026)).thenReturn(Optional.of(paid));

        assertThatThrownBy(() -> duesService.markPaid("m1", 2026))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void rejectsPaymentWhenNoRecordExists() {
        when(duesRepository.findByMemberIdAndYear("m1", 2026)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> duesService.markPaid("m1", 2026))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static MemberResult member(String id) {
        return member(id, MemberStatus.ACTIVE);
    }

    private static MemberResult member(String id, MemberStatus status) {
        return new MemberResult(
                id, "TEC-2026-001", "Test", "Member",
                LocalDate.of(1990, 1, 1), id + "@example.com", "+100", "Addr",
                LocalDate.of(2026, 1, 1), status);
    }
}

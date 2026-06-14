package com.tertech.tkenlightment.membership.dues;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tertech.tkenlightment.membership.dues.domain.events.DuesReminderEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.MemberAutoInactivatedEvent;
import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuesChunkServiceTest {

    @Mock
    DuesRecordRepository duesRepository;

    @Mock
    MemberAPI memberAPI;

    @Mock
    SpringEventPublisher eventPublisher;

    private DuesChunkService chunkService;

    @BeforeEach
    void setUp() {
        chunkService = new DuesChunkService(duesRepository, memberAPI, eventPublisher);
    }

    // --- createDuesRecordsForChunk ---

    @Test
    void createsRecordsForMembersWithoutOne() {
        when(duesRepository.findByMemberIdAndYear(anyString(), anyInt())).thenReturn(Optional.empty());

        chunkService.createDuesRecordsForChunk(List.of("m1", "m2"), 2026);

        verify(duesRepository, times(2)).save(any(DuesRecordEntity.class));
    }

    @Test
    void skipsMembersThatAlreadyHaveARecordForTheYear() {
        when(duesRepository.findByMemberIdAndYear("m1", 2026))
                .thenReturn(Optional.of(DuesRecordEntity.create("m1", 2026)));

        chunkService.createDuesRecordsForChunk(List.of("m1"), 2026);

        verify(duesRepository, never()).save(any(DuesRecordEntity.class));
    }

    // --- sendRemindersForChunk ---

    @Test
    void publishesReminderForActiveMember() {
        when(memberAPI.getMember("m1")).thenReturn(member("m1", MemberStatus.ACTIVE));

        chunkService.sendRemindersForChunk(List.of("m1"), 2026);

        verify(eventPublisher).publish(any(DuesReminderEvent.class));
    }

    @Test
    void doesNotRemindNonActiveMember() {
        when(memberAPI.getMember("m1")).thenReturn(member("m1", MemberStatus.INACTIVE));

        chunkService.sendRemindersForChunk(List.of("m1"), 2026);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void reminderFailureForOneMemberDoesNotStopTheRest() {
        when(memberAPI.getMember("bad")).thenThrow(new RuntimeException("boom"));
        when(memberAPI.getMember("good")).thenReturn(member("good", MemberStatus.ACTIVE));

        chunkService.sendRemindersForChunk(List.of("bad", "good"), 2026);

        verify(eventPublisher).publish(any(DuesReminderEvent.class));
    }

    // --- inactivateForChunk ---

    @Test
    void inactivatesUnpaidActiveMembersAndPublishesEvent() {
        when(memberAPI.getMember("m1")).thenReturn(member("m1", MemberStatus.ACTIVE));

        chunkService.inactivateForChunk(List.of("m1"), 2026);

        verify(memberAPI).inactivateMember("m1");
        verify(eventPublisher).publish(any(MemberAutoInactivatedEvent.class));
    }

    @Test
    void skipsNonActiveMembersDuringInactivation() {
        when(memberAPI.getMember("m1")).thenReturn(member("m1", MemberStatus.SUSPENDED));

        chunkService.inactivateForChunk(List.of("m1"), 2026);

        verify(memberAPI, never()).inactivateMember(any());
        verify(eventPublisher, never()).publish(any());
    }

    private static MemberResult member(String id, MemberStatus status) {
        return new MemberResult(
                id, "TEC-2026-001", "Test", "Member",
                LocalDate.of(1990, 1, 1), id + "@example.com", "+100", "Addr",
                LocalDate.of(2026, 1, 1), status);
    }
}

package com.tertech.tkenlightment.membership.dues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tertech.tkenlightment.membership.dues.domain.events.DuesPaidEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.MemberAutoInactivatedEvent;
import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.DomainException;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.ResourceNotFoundException;
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

@ExtendWith(MockitoExtension.class)
class DuesServiceTest {

    @Mock
    DuesRecordRepository duesRepository;

    @Mock
    MemberAPI memberAPI;

    @Mock
    SpringEventPublisher eventPublisher;

    private final Clock clock = Clock.fixed(Instant.parse("2026-04-15T00:00:00Z"), ZoneOffset.UTC);
    private DuesService duesService;

    @BeforeEach
    void setUp() {
        duesService = new DuesService(duesRepository, memberAPI, eventPublisher, clock);
    }

    // --- generateDuesForYear ---

    @Test
    void generatesRecordsForActiveMembersWithoutOne() {
        when(memberAPI.findActiveMembers())
                .thenReturn(List.of(member("m1", MemberStatus.ACTIVE), member("m2", MemberStatus.ACTIVE)));
        when(duesRepository.findByMemberIdAndYear(anyString(), anyInt())).thenReturn(Optional.empty());

        duesService.generateDuesForYear(2026);

        verify(duesRepository, times(2)).save(any(DuesRecordEntity.class));
    }

    @Test
    void skipsMembersThatAlreadyHaveARecordForTheYear() {
        when(memberAPI.findActiveMembers()).thenReturn(List.of(member("m1", MemberStatus.ACTIVE)));
        when(duesRepository.findByMemberIdAndYear("m1", 2026))
                .thenReturn(Optional.of(DuesRecordEntity.create("m1", 2026)));

        duesService.generateDuesForYear(2026);

        verify(duesRepository, never()).save(any(DuesRecordEntity.class));
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

    // --- inactivateUnpaidMembers ---

    @Test
    void inactivatesUnpaidActiveMembersAndPublishesEvent() {
        DuesRecordEntity unpaid = DuesRecordEntity.create("m1", 2026);
        when(duesRepository.findUnpaidForYear(2026)).thenReturn(List.of(unpaid));
        when(memberAPI.getMember("m1")).thenReturn(member("m1", MemberStatus.ACTIVE));

        duesService.inactivateUnpaidMembers(2026);

        verify(memberAPI).inactivateMember("m1");
        verify(eventPublisher).publish(any(MemberAutoInactivatedEvent.class));
    }

    @Test
    void skipsNonActiveMembersDuringInactivation() {
        DuesRecordEntity unpaid = DuesRecordEntity.create("m1", 2026);
        when(duesRepository.findUnpaidForYear(2026)).thenReturn(List.of(unpaid));
        when(memberAPI.getMember("m1")).thenReturn(member("m1", MemberStatus.SUSPENDED));

        duesService.inactivateUnpaidMembers(2026);

        verify(memberAPI, never()).inactivateMember(any());
        verify(eventPublisher, never()).publish(any());
    }

    private static MemberResult member(String id, MemberStatus status) {
        return new MemberResult(
                id,
                "TEC-2026-001",
                "Test",
                "Member",
                LocalDate.of(1990, 1, 1),
                id + "@example.com",
                "+100",
                "Addr",
                LocalDate.of(2026, 1, 1),
                status);
    }
}

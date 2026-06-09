package com.tertech.tkenlightment.membership.dues;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tertech.tkenlightment.membership.dues.domain.events.DuesReminderEvent;
import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuesServiceReminderTests {

    @Mock
    DuesRecordRepository duesRepository;

    @Mock
    MemberAPI memberAPI;

    @Mock
    SpringEventPublisher eventPublisher;

    @Mock
    Clock clock;

    @InjectMocks
    DuesService duesService;

    @Test
    void shouldPublishReminderEventForEachUnpaidActiveMember() {
        DuesRecordEntity unpaidRecord = DuesRecordEntity.create("member-1", 2026);

        MemberResult activeMember = new MemberResult(
                "member-1", "TEC-2026-001", "Alice", "Walker",
                LocalDate.of(1990, 1, 1), "alice@example.com", "+123",
                "1 Main St", LocalDate.now(), MemberStatus.ACTIVE);

        when(duesRepository.findUnpaidForYear(2026)).thenReturn(List.of(unpaidRecord));
        when(memberAPI.getMember("member-1")).thenReturn(activeMember);

        duesService.sendDuesReminders(2026);

        verify(eventPublisher).publish(any(DuesReminderEvent.class));
    }

    @Test
    void shouldNotPublishReminderForInactiveMember() {
        DuesRecordEntity unpaidRecord = DuesRecordEntity.create("member-2", 2026);

        MemberResult inactiveMember = new MemberResult(
                "member-2", "TEC-2026-002", "Bob", "Jones",
                LocalDate.of(1985, 3, 15), "bob@example.com", "+456",
                "2 Oak Ave", LocalDate.now(), MemberStatus.INACTIVE);

        when(duesRepository.findUnpaidForYear(2026)).thenReturn(List.of(unpaidRecord));
        when(memberAPI.getMember("member-2")).thenReturn(inactiveMember);

        duesService.sendDuesReminders(2026);

        verify(eventPublisher, never()).publish(any(DuesReminderEvent.class));
    }
}

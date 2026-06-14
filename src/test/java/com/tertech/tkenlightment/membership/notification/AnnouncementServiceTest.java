package com.tertech.tkenlightment.membership.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.MemberResult;
import com.tertech.tkenlightment.membership.notification.rest.dtos.RecipientGroup;
import com.tertech.tkenlightment.membership.shared.domain.events.SpringEventPublisher;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    MemberAPI memberAPI;

    @Mock
    SpringEventPublisher eventPublisher;

    @InjectMocks
    AnnouncementService service;

    @Captor
    ArgumentCaptor<AnnouncementRequestedEvent> eventCaptor;

    private static MemberResult memberWithEmail(String email) {
        return memberWithStatus(email, MemberStatus.ACTIVE);
    }

    private static MemberResult memberWithStatus(String email, MemberStatus status) {
        return new MemberResult(
                "id-" + email, "TEC-2026-001", "First", "Last",
                LocalDate.of(1990, 1, 1), email, "+123", "1 Main St",
                LocalDate.now(), status);
    }

    @Test
    void activeGroupTargetsActiveMembersAndPublishesEvent() {
        when(memberAPI.findActiveMembers())
                .thenReturn(List.of(memberWithEmail("a@example.com"), memberWithEmail("b@example.com")));

        int count = service.sendAnnouncement("Subject", "Body", RecipientGroup.ACTIVE_MEMBERS);

        assertThat(count).isEqualTo(2);
        verify(memberAPI, never()).findAllMembers();
        verify(eventPublisher).publish(eventCaptor.capture());
        AnnouncementRequestedEvent event = eventCaptor.getValue();
        assertThat(event.subject()).isEqualTo("Subject");
        assertThat(event.body()).isEqualTo("Body");
        assertThat(event.recipientEmails()).containsExactly("a@example.com", "b@example.com");
    }

    @Test
    void allGroupTargetsAllMembers() {
        when(memberAPI.findAllMembers())
                .thenReturn(List.of(
                        memberWithEmail("a@example.com"),
                        memberWithEmail("b@example.com"),
                        memberWithEmail("c@example.com")));

        int count = service.sendAnnouncement("Subject", "Body", RecipientGroup.ALL_MEMBERS);

        assertThat(count).isEqualTo(3);
        verify(memberAPI, never()).findActiveMembers();
        verify(eventPublisher).publish(any(AnnouncementRequestedEvent.class));
    }

    @Test
    void allGroupExcludesTerminatedMembers() {
        when(memberAPI.findAllMembers())
                .thenReturn(List.of(
                        memberWithStatus("active@example.com", MemberStatus.ACTIVE),
                        memberWithStatus("suspended@example.com", MemberStatus.SUSPENDED),
                        memberWithStatus("left@example.com", MemberStatus.TERMINATED)));

        int count = service.sendAnnouncement("Subject", "Body", RecipientGroup.ALL_MEMBERS);

        assertThat(count).isEqualTo(2);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().recipientEmails())
                .containsExactly("active@example.com", "suspended@example.com")
                .doesNotContain("left@example.com");
    }

    @Test
    void noRecipientsPublishesNothingAndReturnsZero() {
        when(memberAPI.findActiveMembers()).thenReturn(List.of());

        int count = service.sendAnnouncement("Subject", "Body", RecipientGroup.ACTIVE_MEMBERS);

        assertThat(count).isZero();
        verify(eventPublisher, never()).publish(any());
    }
}

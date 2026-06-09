package com.tertech.tkenlightment.membership.notification;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.tertech.tkenlightment.membership.dues.domain.events.DuesPaidEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.DuesReminderEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.MemberAutoInactivatedEvent;
import com.tertech.tkenlightment.membership.member.domain.events.MemberRegisteredEvent;
import com.tertech.tkenlightment.membership.member.domain.events.MemberStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTests {

    @Mock
    EmailService emailService;

    MemberEventNotificationListener memberListener;
    DuesEventNotificationListener duesListener;

    @BeforeEach
    void setUp() {
        memberListener = new MemberEventNotificationListener(emailService);
        duesListener = new DuesEventNotificationListener(emailService);
    }

    @Test
    void shouldSendWelcomeEmailOnMemberRegistered() {
        memberListener.onMemberRegistered(
                new MemberRegisteredEvent("m1", "TEC-2026-001", "jane@example.com", "Jane Doe"));

        verify(emailService).send(
                eq("jane@example.com"),
                contains("Welcome"),
                contains("TEC-2026-001"));
    }

    @Test
    void shouldIncludeMemberNameInWelcomeEmail() {
        memberListener.onMemberRegistered(
                new MemberRegisteredEvent("m1", "TEC-2026-001", "jane@example.com", "Jane Doe"));

        verify(emailService).send(
                eq("jane@example.com"),
                contains("Welcome"),
                contains("Jane Doe"));
    }

    @Test
    void shouldSendStatusChangeEmailWithNewStatus() {
        memberListener.onMemberStatusChanged(
                new MemberStatusChangedEvent("m2", "bob@example.com", "Bob Smith", "ACTIVE", "SUSPENDED"));

        verify(emailService).send(
                eq("bob@example.com"),
                contains("status"),
                contains("SUSPENDED"));
    }

    @Test
    void shouldSendDuesConfirmationEmailWithYear() {
        duesListener.onDuesPaid(new DuesPaidEvent("m3", "carol@example.com", "Carol White", 2026));

        verify(emailService).send(
                eq("carol@example.com"),
                contains("2026"),
                contains("paid"));
    }

    @Test
    void shouldSendInactivationEmailWithYear() {
        duesListener.onMemberAutoInactivated(
                new MemberAutoInactivatedEvent("m4", "dave@example.com", "Dave Brown", 2026));

        verify(emailService).send(
                eq("dave@example.com"),
                contains("INACTIVE"),
                contains("2026"));
    }

    @Test
    void shouldSendReminderEmailWithDeadline() {
        duesListener.onDuesReminder(new DuesReminderEvent("m5", "eve@example.com", "Eve Green", 2026));

        verify(emailService).send(
                eq("eve@example.com"),
                contains("April 30"),
                contains("April 30"));
    }
}

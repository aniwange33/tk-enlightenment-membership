package com.tertech.tkenlightment.membership.notification;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.tertech.tkenlightment.membership.auth.domain.events.AccountCreatedEvent;
import com.tertech.tkenlightment.membership.auth.domain.events.PasswordResetRequestedEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.DuesPaidEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.DuesReminderEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.MemberAutoInactivatedEvent;
import com.tertech.tkenlightment.membership.member.domain.events.MemberStatusChangedEvent;
import java.util.List;
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
    AnnouncementEventListener announcementListener;

    @BeforeEach
    void setUp() {
        memberListener = new MemberEventNotificationListener(
                emailService, new NotificationProperties("noreply@taraku.test", "https://app.test"));
        duesListener = new DuesEventNotificationListener(emailService);
        announcementListener = new AnnouncementEventListener(emailService);
    }

    @Test
    void shouldSendWelcomeEmailOnAccountCreated() {
        memberListener.onAccountCreated(
                new AccountCreatedEvent("jane@example.com", "Jane Doe", "TEC-2026-001", "Temp1234"));

        verify(emailService).send(
                eq("jane@example.com"),
                contains("Welcome"),
                contains("TEC-2026-001"));
    }

    @Test
    void shouldIncludeTempPasswordAndNameInWelcomeEmail() {
        memberListener.onAccountCreated(
                new AccountCreatedEvent("jane@example.com", "Jane Doe", "TEC-2026-001", "Temp1234"));

        verify(emailService).send(
                eq("jane@example.com"),
                contains("Welcome"),
                contains("Temp1234"));
    }

    @Test
    void shouldIncludeLoginUrlInWelcomeEmail() {
        memberListener.onAccountCreated(
                new AccountCreatedEvent("jane@example.com", "Jane Doe", "TEC-2026-001", "Temp1234"));

        verify(emailService).send(
                eq("jane@example.com"),
                contains("Welcome"),
                contains("https://app.test/login"));
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
    void shouldSendResetEmailWithTokenLink() {
        memberListener.onPasswordResetRequested(
                new PasswordResetRequestedEvent("frank@example.com", "rawtoken123"));

        verify(emailService).send(
                eq("frank@example.com"),
                contains("Reset"),
                contains("https://app.test/reset?token=rawtoken123"));
    }

    @Test
    void shouldSendAnnouncementToEachRecipientWithBrandedBody() {
        announcementListener.onAnnouncementRequested(new AnnouncementRequestedEvent(
                "Club News", "Meeting on Friday", List.of("a@example.com", "b@example.com")));

        // Subject is the admin's; body is wrapped in the club greeting/sign-off.
        verify(emailService).send(eq("a@example.com"), eq("Club News"), contains("Meeting on Friday"));
        verify(emailService).send(eq("b@example.com"), eq("Club News"), contains("Taraku Enlightenment Club"));
    }

    @Test
    void shouldContinueAnnouncementWhenOneRecipientFails() {
        doThrow(new RuntimeException("smtp down"))
                .when(emailService)
                .send(eq("bad@example.com"), anyString(), anyString());

        announcementListener.onAnnouncementRequested(new AnnouncementRequestedEvent(
                "Club News", "Body", List.of("bad@example.com", "good@example.com")));

        // The failure is swallowed and the remaining recipient still receives the email.
        verify(emailService).send(eq("good@example.com"), eq("Club News"), contains("Body"));
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

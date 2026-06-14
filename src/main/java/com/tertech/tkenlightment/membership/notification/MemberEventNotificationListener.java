package com.tertech.tkenlightment.membership.notification;

import com.tertech.tkenlightment.membership.auth.domain.events.AccountCreatedEvent;
import com.tertech.tkenlightment.membership.auth.domain.events.PasswordResetRequestedEvent;
import com.tertech.tkenlightment.membership.member.domain.events.MemberStatusChangedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class MemberEventNotificationListener {

    private final EmailService emailService;
    private final NotificationProperties properties;

    MemberEventNotificationListener(EmailService emailService, NotificationProperties properties) {
        this.emailService = emailService;
        this.properties = properties;
    }

    @ApplicationModuleListener
    void onAccountCreated(AccountCreatedEvent event) {
        emailService.send(
                event.email(),
                "Welcome to Taraku Enlightenment Club — " + event.membershipNumber(),
                """
                Dear %s,

                Welcome to the Taraku Enlightenment Club! Your membership has been registered.

                Membership Number: %s
                Temporary Password: %s

                Please log in and change your password on first login.

                Regards,
                Taraku Enlightenment Club
                """.formatted(event.fullName(), event.membershipNumber(), event.tempPassword()));
    }

    @ApplicationModuleListener
    void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        String resetLink = properties.resetLinkBaseUrl() + "?token=" + event.rawToken();
        emailService.send(
                event.email(),
                "Reset your Taraku Enlightenment Club password",
                """
                Hello,

                We received a request to reset your password. Use the link below to choose a new one.
                This link expires in 24 hours and can be used once.

                %s

                If you did not request this, you can safely ignore this email.

                Regards,
                Taraku Enlightenment Club
                """.formatted(resetLink));
    }

    @ApplicationModuleListener
    void onMemberStatusChanged(MemberStatusChangedEvent event) {
        emailService.send(
                event.email(),
                "Your membership status has changed",
                """
                Dear %s,

                Your membership status has been updated to: %s

                If you have questions, please contact the club administration.

                Regards,
                Taraku Enlightenment Club
                """.formatted(event.fullName(), event.newStatus()));
    }
}

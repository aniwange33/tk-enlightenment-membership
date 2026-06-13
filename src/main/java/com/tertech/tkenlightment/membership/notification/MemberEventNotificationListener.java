package com.tertech.tkenlightment.membership.notification;

import com.tertech.tkenlightment.membership.auth.domain.events.AccountCreatedEvent;
import com.tertech.tkenlightment.membership.member.domain.events.MemberStatusChangedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class MemberEventNotificationListener {

    private final EmailService emailService;

    MemberEventNotificationListener(EmailService emailService) {
        this.emailService = emailService;
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

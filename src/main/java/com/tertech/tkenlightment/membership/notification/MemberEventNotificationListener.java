package com.tertech.tkenlightment.membership.notification;

import com.tertech.tkenlightment.membership.member.domain.events.MemberRegisteredEvent;
import com.tertech.tkenlightment.membership.member.domain.events.MemberStatusChangedEvent;
import java.util.random.RandomGenerator;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class MemberEventNotificationListener {

    private final EmailService emailService;

    MemberEventNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @ApplicationModuleListener
    void onMemberRegistered(MemberRegisteredEvent event) {
        String tempPassword = generateTempPassword();
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
                """.formatted(event.fullName(), event.membershipNumber(), tempPassword));
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

    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        RandomGenerator rng = RandomGenerator.getDefault();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

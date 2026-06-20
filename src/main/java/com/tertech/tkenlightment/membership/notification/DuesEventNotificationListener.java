package com.tertech.tkenlightment.membership.notification;

import com.tertech.tkenlightment.membership.dues.domain.events.DuesPaidEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.DuesReminderEvent;
import com.tertech.tkenlightment.membership.dues.domain.events.MemberAutoInactivatedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class DuesEventNotificationListener {

    private final EmailService emailService;

    DuesEventNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @ApplicationModuleListener
    void onDuesPaid(DuesPaidEvent event) {
        emailService.send(
                event.email(),
                "Dues payment confirmed for " + event.year(),
                """
                Dear %s,

                Your dues for the year %d have been marked as paid. Thank you!

                Regards,
                Taraku Enlightenment Club
                """.formatted(event.fullName(), event.year()));
    }

    @ApplicationModuleListener
    void onMemberAutoInactivated(MemberAutoInactivatedEvent event) {
        emailService.send(
                event.email(),
                "Your membership status has changed to INACTIVE",
                """
                Dear %s,

                Your membership status has been changed to INACTIVE because your dues for %d were not paid by April 30.

                Please contact the club administration to settle your dues and reinstate your membership.

                Regards,
                Taraku Enlightenment Club
                """.formatted(event.fullName(), event.year()));
    }

    @ApplicationModuleListener
    void onDuesReminder(DuesReminderEvent event) {
        emailService.send(
                event.email(),
                "Dues reminder — please pay before April 30",
                """
                Dear %s,

                This is a reminder that your club dues for %d are due by April 30.

                Please ensure payment is made before the deadline to keep your membership ACTIVE.

                Regards,
                Taraku Enlightenment Club
                """.formatted(event.fullName(), event.year()));
    }
}

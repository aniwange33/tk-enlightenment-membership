package com.tertech.tkenlightment.membership.notification;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;
    private final MeterRegistry meterRegistry;

    EmailService(JavaMailSender mailSender, NotificationProperties properties, MeterRegistry meterRegistry) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            meterRegistry.counter("notification.email.sent", "outcome", "success").increment();
        } catch (RuntimeException e) {
            meterRegistry.counter("notification.email.sent", "outcome", "failure").increment();
            throw e;
        }
    }
}

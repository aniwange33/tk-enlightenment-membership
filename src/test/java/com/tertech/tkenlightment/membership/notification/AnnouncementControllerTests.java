package com.tertech.tkenlightment.membership.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.tertech.tkenlightment.membership.BaseIT;
import com.tertech.tkenlightment.membership.notification.rest.dtos.AnnouncementResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class AnnouncementControllerTests extends BaseIT {

    @Test
    void adminSendsAnnouncementToActiveMembersAndEachReceivesIt() {
        String alice = "announce.alice@example.com";
        String bob = "announce.bob@example.com";
        registerMember(alice);
        registerMember(bob);

        String subject = "Annual Gathering 2026";
        MvcTestResult result = sendAnnouncement(tokens.admin(), subject, "See you Friday", "ACTIVE_MEMBERS");

        assertThat(result).hasStatusOk();
        AnnouncementResponse body = readBody(result, AnnouncementResponse.class);
        assertThat(body.recipientCount()).isGreaterThanOrEqualTo(2);

        // Each targeted member is emailed asynchronously after the request commits.
        List<String> recipients = awaitAnnouncementRecipients(subject);
        assertThat(recipients).contains(alice, bob);
    }

    @Test
    void allMembersExcludesTerminatedExMembers() {
        String staying = "announce.staying@example.com";
        String left = "announce.left@example.com";
        registerMember(staying);
        String leftId = registerMember(left);
        // Terminate one member: they have left the club.
        MvcTestResult terminated = mvc.patch()
                .uri("/api/members/{id}/status", leftId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"status\": \"TERMINATED\" }")
                .exchange();
        assertThat(terminated).hasStatusOk();

        String subject = "Members-Only Notice " + leftId;
        assertThat(sendAnnouncement(tokens.admin(), subject, "Body", "ALL_MEMBERS")).hasStatusOk();

        List<String> recipients = awaitAnnouncementRecipients(subject);
        assertThat(recipients).contains(staying).doesNotContain(left);
    }

    @Test
    void memberCannotSendAnnouncement() {
        String memberId = registerMember("announce.member@example.com");

        MvcTestResult result =
                sendAnnouncement(tokens.member(memberId), "Nope", "Body", "ALL_MEMBERS");

        assertThat(result).hasStatus(403);
    }

    @Test
    void blankSubjectIsRejected() {
        MvcTestResult result = sendAnnouncement(tokens.admin(), "", "Body", "ACTIVE_MEMBERS");

        assertThat(result).hasStatus(400);
    }

    @Test
    void unknownRecipientGroupIsRejected() {
        MvcTestResult result = sendAnnouncement(tokens.admin(), "Subject", "Body", "EVERYONE");

        assertThat(result).hasStatus(400);
    }

    // ---- helpers ----

    private MvcTestResult sendAnnouncement(String token, String subject, String body, String group) {
        return mvc.post()
                .uri("/api/announcements")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{ \"subject\": \"%s\", \"body\": \"%s\", \"recipientGroup\": \"%s\" }",
                        subject, body, group))
                .exchange();
    }

    private List<String> awaitAnnouncementRecipients(String subject) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            verify(mailSender, atLeastOnce()).send(captor.capture());
            assertThat(recipientsForSubject(captor, subject)).isNotEmpty();
        });
        return recipientsForSubject(captor, subject);
    }

    private List<String> recipientsForSubject(ArgumentCaptor<SimpleMailMessage> captor, String subject) {
        List<String> recipients = new ArrayList<>();
        for (SimpleMailMessage message : captor.getAllValues()) {
            if (subject.equals(message.getSubject()) && message.getTo() != null) {
                recipients.add(message.getTo()[0]);
            }
        }
        return recipients;
    }

    private String registerMember(String email) {
        MvcTestResult result = mvc.post()
                .uri("/api/members")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        """
                        {
                            "firstName": "Test",
                            "lastName": "Member",
                            "dateOfBirth": "1990-01-15",
                            "email": "%s"
                        }
                        """,
                        email))
                .exchange();
        String location = result.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}

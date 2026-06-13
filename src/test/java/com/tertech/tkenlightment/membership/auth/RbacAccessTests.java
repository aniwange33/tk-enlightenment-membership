package com.tertech.tkenlightment.membership.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.tertech.tkenlightment.membership.BaseIT;
import com.tertech.tkenlightment.membership.auth.rest.dtos.MeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class RbacAccessTests extends BaseIT {

    @Test
    void memberCanReadOwnProfileViaMe() {
        String memberId = registerMember("owner.me@example.com");

        MvcTestResult result = mvc.get()
                .uri("/api/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.member(memberId)))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(MeResponse.class).satisfies(me -> {
            assertThat(me.id()).isEqualTo(memberId);
            assertThat(me.email()).isEqualTo("owner.me@example.com");
        });
    }

    @Test
    void memberCanReadOwnMemberRecordButNotOthers() {
        String ownId = registerMember("owner.self@example.com");
        String otherId = registerMember("other.member@example.com");
        String token = tokens.member(ownId);

        MvcTestResult own = mvc.get()
                .uri("/api/members/{id}", ownId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .exchange();
        assertThat(own).hasStatusOk();

        MvcTestResult other = mvc.get()
                .uri("/api/members/{id}", otherId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .exchange();
        assertThat(other).hasStatus(403);
    }

    @Test
    void memberCannotPayDues() {
        String memberId = registerMember("nopay.member@example.com");

        MvcTestResult result = mvc.post()
                .uri("/api/members/{id}/dues/{year}/pay", memberId, 2026)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.member(memberId)))
                .exchange();

        assertThat(result).hasStatus(403);
    }

    @Test
    void memberCanReadOwnDuesHistory() {
        String memberId = registerMember("dues.member@example.com");

        MvcTestResult result = mvc.get()
                .uri("/api/me/dues")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.member(memberId)))
                .exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void adminHasNoLinkedMemberSoMeReturns404() {
        MvcTestResult result = mvc.get()
                .uri("/api/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .exchange();

        assertThat(result).hasStatus(404);
    }

    @Test
    void memberCanUpdateOwnContactDetails() {
        String memberId = registerMember("update.me@example.com");

        MvcTestResult result = mvc.put()
                .uri("/api/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.member(memberId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "phone": "+15551234", "address": "9 New Road" }
                        """)
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(MeResponse.class).satisfies(me -> {
            assertThat(me.phone()).isEqualTo("+15551234");
            assertThat(me.address()).isEqualTo("9 New Road");
        });
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

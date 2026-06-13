package com.tertech.tkenlightment.membership.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.tertech.tkenlightment.membership.BaseIT;
import com.tertech.tkenlightment.membership.member.rest.dtos.MemberResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class MemberControllerTests extends BaseIT {

    @Test
    void shouldRegisterMember() {
        MvcTestResult result = mvc.post()
                .uri("/api/members")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "John",
                            "lastName": "Doe",
                            "dateOfBirth": "1990-01-15",
                            "email": "john.doe@example.com",
                            "phone": "+1234567890",
                            "address": "123 Main St"
                        }
                        """)
                .exchange();

        assertThat(result).hasStatus(201);
        assertThat(result).bodyJson().convertTo(MemberResponse.class).satisfies(response -> {
            assertThat(response.id()).isNotNull();
            assertThat(response.membershipNumber()).startsWith("TEC-");
            assertThat(response.firstName()).isEqualTo("John");
            assertThat(response.lastName()).isEqualTo("Doe");
            assertThat(response.email()).isEqualTo("john.doe@example.com");
            assertThat(response.status().name()).isEqualTo("ACTIVE");
            assertThat(response.joinDate()).isEqualTo(LocalDate.now());
        });
    }

    @Test
    void shouldRejectDuplicateEmail() {
        String body = """
                {
                    "firstName": "Jane",
                    "lastName": "Doe",
                    "dateOfBirth": "1991-05-20",
                    "email": "duplicate@example.com",
                    "phone": "+1234567890"
                }
                """;

        mvc.post()
                .uri("/api/members")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();

        MvcTestResult result = mvc.post()
                .uri("/api/members")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();

        assertThat(result).hasStatus(409);
    }

    @Test
    void shouldRejectInvalidRequest() {
        MvcTestResult result = mvc.post()
                .uri("/api/members")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "",
                            "email": "not-an-email"
                        }
                        """)
                .exchange();

        assertThat(result).hasStatus(400);
    }

    @Test
    void shouldGetMemberById() {
        String id = registerAndExtractId("Alice", "Smith", "alice.smith@example.com");

        MvcTestResult getResult = mvc.get()
                .uri("/api/members/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .exchange();

        assertThat(getResult).hasStatusOk();
        assertThat(getResult).bodyJson().convertTo(MemberResponse.class).satisfies(response -> {
            assertThat(response.id()).isEqualTo(id);
            assertThat(response.email()).isEqualTo("alice.smith@example.com");
        });
    }

    @Test
    void shouldListMembers() {
        registerAndExtractId("Bob", "Jones", "bob.jones@example.com");

        MvcTestResult result = mvc.get()
                .uri("/api/members")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void shouldChangeStatus() {
        String id = registerAndExtractId("Charlie", "Brown", "charlie.brown@example.com");

        MvcTestResult statusResult = mvc.patch()
                .uri("/api/members/{id}/status", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "status": "SUSPENDED" }
                        """)
                .exchange();

        assertThat(statusResult).hasStatusOk();
        assertThat(statusResult).bodyJson().convertTo(MemberResponse.class).satisfies(response -> {
            assertThat(response.status().name()).isEqualTo("SUSPENDED");
        });
    }

    @Test
    void shouldUpdateProfile() {
        String id = registerAndExtractId("Diana", "Prince", "diana.prince@example.com");

        MvcTestResult updateResult = mvc.put()
                .uri("/api/members/{id}/profile", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "phone": "+9876543210",
                            "address": "456 Oak Ave"
                        }
                        """)
                .exchange();

        assertThat(updateResult).hasStatusOk();
        assertThat(updateResult).bodyJson().convertTo(MemberResponse.class).satisfies(response -> {
            assertThat(response.phone()).isEqualTo("+9876543210");
            assertThat(response.address()).isEqualTo("456 Oak Ave");
        });
    }

    @Test
    void shouldRejectUnauthenticatedRequest() {
        MvcTestResult result = mvc.get().uri("/api/members").exchange();

        assertThat(result).hasStatus(401);
    }

    @Test
    void shouldForbidMemberFromListingMembers() {
        MvcTestResult result = mvc.get()
                .uri("/api/members")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.member("some-member")))
                .exchange();

        assertThat(result).hasStatus(403);
    }

    private String registerAndExtractId(String firstName, String lastName, String email) {
        MvcTestResult result = mvc.post()
                .uri("/api/members")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        """
                        {
                            "firstName": "%s",
                            "lastName": "%s",
                            "dateOfBirth": "1990-01-15",
                            "email": "%s"
                        }
                        """,
                        firstName, lastName, email))
                .exchange();

        // Extract ID from Location header: /api/members/{id}
        String location = result.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}

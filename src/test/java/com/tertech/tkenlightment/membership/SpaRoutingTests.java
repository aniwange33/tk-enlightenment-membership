package com.tertech.tkenlightment.membership;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The SPA shell and client-side routes are public and fall back to index.html; the REST API stays
 * protected. (Uses a minimal stand-in index.html on the test classpath.)
 */
class SpaRoutingTests extends BaseIT {

    @Test
    void rootForwardsToTheSpaShell() {
        // The welcome-page mapping forwards "/" to index.html (MockMvc reports the forward URL).
        MvcTestResult result = mvc.get().uri("/").exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).hasForwardedUrl("index.html");
    }

    @Test
    void clientSideRouteFallsBackToIndexWithoutAuth() {
        // A deep link / refresh on a SPA route must serve the shell, not 401.
        MvcTestResult result = mvc.get().uri("/admin").exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyText().contains("data-taraku-spa");
    }

    @Test
    void apiStaysProtected() {
        MvcTestResult result = mvc.get().uri("/api/members").exchange();
        assertThat(result).hasStatus(401);
    }
}

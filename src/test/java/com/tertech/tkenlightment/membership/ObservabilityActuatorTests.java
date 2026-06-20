package com.tertech.tkenlightment.membership;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Health/info are public; the rest of actuator requires ADMIN. */
class ObservabilityActuatorTests extends BaseIT {

    @Test
    void healthIsPublic() {
        MvcTestResult result = mvc.get().uri("/actuator/health").exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyText().contains("UP");
    }

    @Test
    void readinessProbeIsPublic() {
        assertThat(mvc.get().uri("/actuator/health/readiness").exchange()).hasStatusOk();
    }

    @Test
    void metricsRequiresAuth() {
        assertThat(mvc.get().uri("/actuator/metrics").exchange()).hasStatus(401);
    }

    @Test
    void metricsAllowedForAdmin() {
        MvcTestResult result = mvc.get()
                .uri("/actuator/metrics")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.admin()))
                .exchange();
        assertThat(result).hasStatusOk();
    }
}

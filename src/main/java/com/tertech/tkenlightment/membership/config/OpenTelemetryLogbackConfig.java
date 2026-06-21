package com.tertech.tkenlightment.membership.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;

/**
 * Installs the OpenTelemetry SDK into the Logback {@code OpenTelemetryAppender} (declared in
 * logback-spring.xml) so application logs are exported over OTLP, correlated with traces. Spring Boot
 * adds the appender dependency but does not wire the SDK into it out of the box. Guarded so tests
 * (no OpenTelemetry bean / export disabled) load cleanly.
 */
@Configuration
class OpenTelemetryLogbackConfig {

    OpenTelemetryLogbackConfig(ObjectProvider<OpenTelemetry> openTelemetry) {
        openTelemetry.ifAvailable(OpenTelemetryAppender::install);
    }
}

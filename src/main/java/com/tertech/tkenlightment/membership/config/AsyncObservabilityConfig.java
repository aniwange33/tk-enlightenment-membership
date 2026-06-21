package com.tertech.tkenlightment.membership.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;

/**
 * Propagates the Micrometer Observation / trace context across async boundaries so a request's trace
 * continues into the {@code @ApplicationModuleListener} work it triggers (e.g. registration → async
 * dues-record creation → welcome email appear in one trace rather than disconnected ones).
 * Spring Boot applies this {@link TaskDecorator} bean to the auto-configured async task executor.
 */
@Configuration
class AsyncObservabilityConfig {

    @Bean
    TaskDecorator contextPropagatingTaskDecorator() {
        return new ContextPropagatingTaskDecorator();
    }
}

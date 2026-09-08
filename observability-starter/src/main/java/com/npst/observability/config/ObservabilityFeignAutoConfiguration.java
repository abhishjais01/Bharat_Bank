package com.npst.observability.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Carries the correlation id across Feign calls.
 *
 * <p>The Spring Boot services in this platform call each other with OpenFeign,
 * so without this a trace stops at the first service-to-service hop and a
 * customer journey can no longer be reconstructed end to end. Applies only
 * when Feign is actually on the classpath, so nothing is imposed on a service
 * that does not use it.
 */
@AutoConfiguration
@AutoConfigureAfter(ObservabilityAutoConfiguration.class)
@ConditionalOnClass(RequestInterceptor.class)
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "observability", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ObservabilityFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "observabilityFeignTraceInterceptor")
    public RequestInterceptor observabilityFeignTraceInterceptor(
            ObservabilityProperties properties) {

        String header = properties.getTrace().getHeader();
        String mdcKey = properties.getTrace().getMdcKey();

        return template -> {
            String traceId = MDC.get(mdcKey);

            if (traceId != null && !traceId.isBlank()) {
                template.header(header, traceId);
            }
        };
    }
}

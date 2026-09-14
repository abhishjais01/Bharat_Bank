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

// passes the trace id on Feign calls, only when Feign is on the classpath
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

        // add the header only if this request has a trace id
        return template -> {
            String traceId = MDC.get(mdcKey);

            if (traceId != null && !traceId.isBlank()) {
                template.header(header, traceId);
            }
        };
    }
}

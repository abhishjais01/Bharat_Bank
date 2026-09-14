package com.npst.observability.config;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

// passes the trace id on WebClient calls (add the filter bean to your WebClient)
@AutoConfiguration
@AutoConfigureAfter(ObservabilityAutoConfiguration.class)
@ConditionalOnClass(WebClient.class)
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "observability", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ObservabilityWebClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "observabilityWebClientTraceFilter")
    public ExchangeFilterFunction observabilityWebClientTraceFilter(
            ObservabilityProperties properties) {

        String header = properties.getTrace().getHeader();
        String mdcKey = properties.getTrace().getMdcKey();

        // copy the trace id onto the outgoing request
        return (request, next) -> {

            String traceId = MDC.get(mdcKey);

            if (traceId == null || traceId.isBlank()) {
                return next.exchange(request);
            }

            return next.exchange(ClientRequest.from(request)
                    .header(header, traceId)
                    .build());
        };
    }
}

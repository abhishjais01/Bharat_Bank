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

/**
 * Carries the correlation id across WebClient calls, for services that use the
 * reactive client instead of RestTemplate or Feign.
 *
 * <p>Contributed as an {@link ExchangeFilterFunction} rather than a WebClient
 * bean - the same reasoning as the RestTemplate customizer: a starter must not
 * claim the application's own client.
 *
 * <p>Note the MDC caveat. This reads the trace id at assembly time, on the
 * calling thread. A pipeline that hops schedulers before subscribing would
 * need MDC bridged through the Reactor context; that is out of scope until a
 * service in this platform actually goes reactive.
 */
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

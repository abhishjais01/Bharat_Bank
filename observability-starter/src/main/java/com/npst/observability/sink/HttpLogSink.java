package com.npst.observability.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.npst.observability.client.TraceRestTemplateInterceptor;
import com.npst.observability.config.ObservabilityProperties;
import com.npst.observability.contract.AuditIngestRequest;
import com.npst.observability.contract.LogIngestRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Ships one application log to the central logging-api over plain HTTP.
 *
 * <p>Two properties matter more than what it does:
 *
 * <ul>
 *   <li>It owns a private RestTemplate <em>with timeouts</em>. The starter no
 *       longer publishes a RestTemplate bean, and the old shared instance had
 *       no timeout at all - a stalled logging-api would block bank request
 *       threads indefinitely.</li>
 *   <li>A failure can never surface to the caller, but is never silent
 *       either. The original code swallowed every exception without logging,
 *       so a rejected log looked identical to a delivered one.</li>
 * </ul>
 */
public class HttpLogSink implements LogSink {

    private static final Logger log = LoggerFactory.getLogger(HttpLogSink.class);

    private final ObservabilityProperties properties;
    private final RestTemplate restTemplate;
    private final Counter sent;
    private final Counter failed;

    public HttpLogSink(ObservabilityProperties properties,
                       TraceRestTemplateInterceptor traceInterceptor,
                       ObjectMapper objectMapper,
                       MeterRegistry meterRegistry) {

        this.properties = properties;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getSink().getConnectTimeout());
        factory.setReadTimeout(properties.getSink().getReadTimeout());

        this.restTemplate = new RestTemplate(factory);
        this.restTemplate.setInterceptors(List.of(traceInterceptor));

        // Serialize with the same ObjectMapper that writes the file log, so
        // wire format and file format cannot drift - notably the Instant
        // timestamp, which a default mapper emits as an epoch number rather
        // than ISO-8601, and logging-api would reject every line.
        this.restTemplate.getMessageConverters()
                .removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        this.restTemplate.getMessageConverters()
                .add(0, new MappingJackson2HttpMessageConverter(objectMapper));

        this.sent = SinkMetrics.counter(meterRegistry, "sent",
                "Logs accepted by logging-api");
        this.failed = SinkMetrics.counter(meterRegistry, "failed",
                "Logs rejected by logging-api or lost in transit");
    }

    @Override
    public void send(LogIngestRequest request) {
        post(properties.getSink().getEndpoint(), request, request.getTraceId(), "log");
    }

    @Override
    public void sendAudit(AuditIngestRequest request) {
        post(properties.getSink().getAuditEndpoint(), request, request.getTraceId(), "audit");
    }

    private void post(String endpoint, Object payload, String traceId, String kind) {

        if (!properties.getSink().isEnabled() || endpoint == null || endpoint.isBlank()) {
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForEntity(endpoint, new HttpEntity<>(payload, headers), Void.class);

            SinkMetrics.increment(sent);

        } catch (Exception ex) {
            SinkMetrics.increment(failed);

            // Never propagate, never stay silent. The original code swallowed
            // everything, so a rejected record looked like a delivered one.
            log.warn("Failed to ship {} to {} : traceId={} reason={}",
                    kind, endpoint, traceId, ex.getMessage());
        }
    }
}

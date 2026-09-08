package com.npst.observability.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.npst.observability.config.ObservabilityProperties;
import com.npst.observability.contract.LogIngestRequest;
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
 * <p>Two properties of this class matter more than what it does:
 *
 * <ul>
 *   <li>It owns a private RestTemplate <em>with timeouts</em>. The starter no
 *       longer publishes a RestTemplate bean, which used to hijack the one the
 *       host application wanted, and the old instance had no timeout at all -
 *       a stalled logging-api would block bank request threads forever.</li>
 *   <li>A failure here can never surface to the caller. Logging is a
 *       side-channel; a customer's balance enquiry must succeed even when the
 *       observability platform is down.</li>
 * </ul>
 *
 * <p>Step 3 moves the send off the request thread entirely.
 */
public class LoggingClient {

    private static final Logger log = LoggerFactory.getLogger(LoggingClient.class);

    private final ObservabilityProperties properties;
    private final RestTemplate restTemplate;

    public LoggingClient(ObservabilityProperties properties,
                         TraceRestTemplateInterceptor traceInterceptor,
                         ObjectMapper objectMapper) {

        this.properties = properties;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getSink().getConnectTimeout());
        factory.setReadTimeout(properties.getSink().getReadTimeout());

        this.restTemplate = new RestTemplate(factory);
        this.restTemplate.setInterceptors(List.of(traceInterceptor));

        // Serialize the payload with the same ObjectMapper that writes the
        // file log, so wire format and file format cannot drift - notably the
        // Instant timestamp, which a default mapper emits as an epoch number
        // rather than ISO-8601.
        this.restTemplate.getMessageConverters()
                .removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        this.restTemplate.getMessageConverters()
                .add(0, new MappingJackson2HttpMessageConverter(objectMapper));
    }

    public void send(LogIngestRequest request) {

        if (!properties.getSink().isEnabled()) {
            return;
        }

        String endpoint = properties.getSink().getEndpoint();

        if (endpoint == null || endpoint.isBlank()) {
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForEntity(endpoint, new HttpEntity<>(request, headers), Void.class);

        } catch (Exception ex) {
            // Never propagate. Do not stay silent either - the old code
            // swallowed everything, so a rejected log looked identical to a
            // delivered one.
            log.warn("Failed to ship log to {} : traceId={} reason={}",
                    endpoint, request.getTraceId(), ex.getMessage());
        }
    }
}

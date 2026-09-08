package com.npst.observability.filter;

import com.npst.observability.config.ObservabilityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Establishes the correlation id for the request.
 *
 * <p>The id is generated once at the API gateway and travels down the call
 * chain in a header, so this filter <em>honours</em> an inbound value and only
 * mints one when none arrives. That is what allows a single customer journey
 * to be reconstructed across several microservices.
 */
public class TraceFilter extends OncePerRequestFilter {

    private final ObservabilityProperties properties;

    public TraceFilter(ObservabilityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = properties.getTrace().getHeader();
        String mdcKey = properties.getTrace().getMdcKey();

        String traceId = request.getHeader(header);

        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        request.setAttribute(mdcKey, traceId);
        MDC.put(mdcKey, traceId);
        response.setHeader(header, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Remove only what this filter added. MDC.clear() would also wipe
            // keys the host application put there for its own logging.
            MDC.remove(mdcKey);
        }
    }
}

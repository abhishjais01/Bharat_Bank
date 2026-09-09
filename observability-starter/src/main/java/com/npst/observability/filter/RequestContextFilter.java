package com.npst.observability.filter;

import com.npst.observability.config.ObservabilityProperties;
import com.npst.observability.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Establishes everything we know about the caller, once, at the edge.
 *
 * <p>Replaces the earlier TraceFilter, which handled only the correlation id.
 * The audit trail has to record where a request came from - channel, device, IP,
 * customer - and capturing that per call site would be both repetitive and
 * unreliable. Capturing it here means every log line in the request, including
 * ones written by code that has never heard of this platform, carries it.
 *
 * <p>The correlation id is <em>honoured</em> rather than generated: the gateway
 * mints it and it travels down the call chain, which is what allows one customer
 * journey to be reconstructed across several microservices. A fresh id is
 * created only when none arrives.
 */
public class RequestContextFilter extends OncePerRequestFilter {

    private final ObservabilityProperties properties;

    public RequestContextFilter(ObservabilityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String traceHeader = properties.getTrace().getHeader();
        String mdcKey = properties.getTrace().getMdcKey();
        ObservabilityProperties.Context context = properties.getContext();

        String traceId = request.getHeader(traceHeader);

        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(mdcKey, traceId);
        request.setAttribute(mdcKey, traceId);
        response.setHeader(traceHeader, traceId);

        // All optional. An internal call or a scheduled job simply carries fewer.
        RequestContext.put(RequestContext.CHANNEL, request.getHeader(context.getChannelHeader()));
        RequestContext.put(RequestContext.DEVICE_ID, request.getHeader(context.getDeviceHeader()));
        RequestContext.put(RequestContext.CUSTOMER_ID, request.getHeader(context.getCustomerHeader()));
        RequestContext.put(RequestContext.IP_ADDRESS, clientIpOf(request, context));

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Only this platform's keys - the host application's own MDC
            // entries are none of our business.
            RequestContext.clear();
            MDC.remove(mdcKey);
        }
    }

    /**
     * Behind a load balancer the socket address is the balancer, so a forwarded
     * header wins when present. X-Forwarded-For is a comma separated chain and
     * the first entry is the original client.
     */
    private static String clientIpOf(HttpServletRequest request,
                                     ObservabilityProperties.Context context) {

        for (String header : context.getIpHeaders()) {

            String value = request.getHeader(header);

            if (value != null && !value.isBlank()) {
                int comma = value.indexOf(',');
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }

        return request.getRemoteAddr();
    }
}

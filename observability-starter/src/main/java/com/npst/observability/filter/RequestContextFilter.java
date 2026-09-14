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

// first filter on every request: puts the trace id and caller headers into MDC
public class RequestContextFilter extends OncePerRequestFilter {

    private final ObservabilityProperties properties;

    public RequestContextFilter(ObservabilityProperties properties) {
        this.properties = properties;
    }

    // set up the context, run the request, then clean up
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // header names from config
        String traceHeader = properties.getTrace().getHeader();
        String mdcKey = properties.getTrace().getMdcKey();
        ObservabilityProperties.Context context = properties.getContext();

        String traceId = request.getHeader(traceHeader);

        // reuse the caller's trace id, create one only when it's missing
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        // make the trace id available to logs, the interceptor and the caller
        MDC.put(mdcKey, traceId);
        request.setAttribute(mdcKey, traceId);
        response.setHeader(traceHeader, traceId);

        // optional caller details
        RequestContext.put(RequestContext.CHANNEL, request.getHeader(context.getChannelHeader()));
        RequestContext.put(RequestContext.DEVICE_ID, request.getHeader(context.getDeviceHeader()));
        RequestContext.put(RequestContext.CUSTOMER_ID, request.getHeader(context.getCustomerHeader()));
        RequestContext.put(RequestContext.IP_ADDRESS, clientIpOf(request, context));

        // continue with the rest of the request
        try {
            filterChain.doFilter(request, response);
        } finally {
            // remove only our own keys, the app may have its own MDC entries
            RequestContext.clear();
            MDC.remove(mdcKey);
        }
    }

    // real client IP: forwarded header first, then the socket address
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

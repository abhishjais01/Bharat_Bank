package com.npst.observability.client;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;

import java.io.IOException;

// adds the trace id header to outgoing RestTemplate calls
public class TraceRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution)
            throws IOException {

        // pass the current trace id on, so the next service logs under the same id
        String traceId = MDC.get("traceId");

        if (traceId != null) {
            request.getHeaders().add("X-Trace-Id", traceId);
        }

        return execution.execute(request, body);
    }
}
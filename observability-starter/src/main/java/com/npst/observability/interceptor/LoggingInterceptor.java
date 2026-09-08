package com.npst.observability.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingInterceptor.class);

    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        request.setAttribute(START_TIME, System.currentTimeMillis());

        log.info("Request Started : method={} uri={} traceId={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getAttribute("traceId"));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        Object start = request.getAttribute(START_TIME);

        long duration = start instanceof Long startedAt
                ? System.currentTimeMillis() - startedAt
                : -1;

        log.info("Request Completed : method={} uri={} status={} duration={}ms traceId={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration,
                request.getAttribute("traceId"));
    }
}

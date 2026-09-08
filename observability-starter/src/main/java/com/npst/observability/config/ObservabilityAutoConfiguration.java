package com.npst.observability.config;

import com.npst.observability.client.TraceRestTemplateInterceptor;
import com.npst.observability.filter.TraceFilter;
import com.npst.observability.interceptor.LoggingInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
public class ObservabilityAutoConfiguration {

    @Bean
    public TraceRestTemplateInterceptor traceRestTemplateInterceptor() {
        return new TraceRestTemplateInterceptor();
    }

    @Bean
    public RestTemplate restTemplate(TraceRestTemplateInterceptor interceptor) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(interceptor));
        return restTemplate;
    }

    @Bean
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

    @Bean
    public TraceFilter traceFilter() {
        return new TraceFilter();
    }
}

package com.npst.observability.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.npst.observability.client.TraceRestTemplateInterceptor;
import com.npst.observability.config.bank.BankResolver;
import com.npst.observability.config.bank.PropertyBankResolver;
import com.npst.observability.exception.GlobalExceptionHandler;
import com.npst.observability.aop.LogRegistryAspect;
import com.npst.observability.filter.RequestContextFilter;
import com.npst.observability.interceptor.LoggingInterceptor;
import com.npst.observability.logger.CommonLogger;
import com.npst.observability.logger.CommonLoggerImpl;
import com.npst.observability.masking.MetadataMasker;
import com.npst.observability.sink.AsyncLogSink;
import com.npst.observability.sink.HttpLogSink;
import com.npst.observability.sink.LogSink;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// creates all starter beans automatically in any Spring Boot app that adds this dependency
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "observability", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    // bank, environment and service name from config
    @Bean
    @ConditionalOnMissingBean
    public BankResolver bankResolver(ObservabilityProperties properties,
                                     Environment environment) {
        return new PropertyBankResolver(properties, environment);
    }

    // masking rules for sensitive values
    @Bean
    @ConditionalOnMissingBean
    public MetadataMasker metadataMasker(ObservabilityProperties properties) {
        return new MetadataMasker(properties.getMasking());
    }

    // trace id header for outgoing calls
    @Bean
    @ConditionalOnMissingBean
    public TraceRestTemplateInterceptor traceRestTemplateInterceptor() {
        return new TraceRestTemplateInterceptor();
    }

    // adds the trace interceptor to every RestTemplate built with RestTemplateBuilder
    @Bean
    @ConditionalOnClass(RestTemplateCustomizer.class)
    @ConditionalOnMissingBean(name = "observabilityTraceRestTemplateCustomizer")
    public RestTemplateCustomizer observabilityTraceRestTemplateCustomizer(
            TraceRestTemplateInterceptor interceptor) {
        return restTemplate -> restTemplate.getInterceptors().add(interceptor);
    }

    // HttpLogSink and AsyncLogSink are built in one bean on purpose. As separate beans,
    // @ConditionalOnMissingBean(LogSink.class) would match HttpLogSink and skip the async wrapper.
    @Bean
    @ConditionalOnMissingBean(LogSink.class)
    public LogSink logSink(ObservabilityProperties properties,
                           TraceRestTemplateInterceptor interceptor,
                           ObjectProvider<ObjectMapper> objectMapper,
                           ObjectProvider<MeterRegistry> meterRegistry) {

        MeterRegistry registry = meterRegistry.getIfAvailable();

        // HTTP sender, wrapped in a background queue unless async is turned off
        HttpLogSink httpSink = new HttpLogSink(properties, interceptor,
                resolveMapper(objectMapper), registry);

        if (!properties.getSink().getAsync().isEnabled()) {
            return httpSink;
        }

        return new AsyncLogSink(httpSink, properties.getSink().getAsync(), registry);
    }

    // main logger used by the aspect and by services directly
    @Bean
    @ConditionalOnMissingBean
    public CommonLogger commonLogger(ObjectProvider<ObjectMapper> objectMapper,
                                     BankResolver bankResolver,
                                     LogSink logSink,
                                     MetadataMasker masker,
                                     ObservabilityProperties properties) {
        return new CommonLoggerImpl(resolveMapper(objectMapper), bankResolver,
                logSink, masker, properties);
    }

    // filter that reads the trace id and caller headers on every request
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(name = "observabilityRequestContextFilterRegistration")
    public FilterRegistrationBean<RequestContextFilter> observabilityRequestContextFilterRegistration(
            ObservabilityProperties properties) {

        FilterRegistrationBean<RequestContextFilter> registration =
                new FilterRegistrationBean<>(new RequestContextFilter(properties));

        // run first so everything after it already has the trace id
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");

        return registration;
    }

    // writes request start and end lines
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

    // registers the interceptor with Spring MVC
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(name = "observabilityWebMvcConfigurer")
    public WebMvcConfigurer observabilityWebMvcConfigurer(LoggingInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }

    // the aspect behind @LogRegistry (turn off with observability.aop.enabled=false)
    @Bean
    @ConditionalOnClass(org.aspectj.lang.ProceedingJoinPoint.class)
    @ConditionalOnProperty(prefix = "observability.aop", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public LogRegistryAspect logRegistryAspect(CommonLogger commonLogger,
                                               ObjectProvider<ObjectMapper> objectMapper) {
        return new LogRegistryAspect(commonLogger, resolveMapper(objectMapper));
    }

    // optional catch-all exception handler, off by default
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "observability.exception-handler", name = "enabled",
            havingValue = "true")
    @ConditionalOnMissingBean
    public GlobalExceptionHandler observabilityGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    // use the app's ObjectMapper if it has one
    private static ObjectMapper resolveMapper(ObjectProvider<ObjectMapper> provider) {
        return provider.getIfAvailable(ObservabilityAutoConfiguration::defaultObjectMapper);
    }

    // fallback mapper that writes dates as ISO text
    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}

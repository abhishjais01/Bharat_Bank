package com.npst.observability.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.npst.observability.client.TraceRestTemplateInterceptor;
import com.npst.observability.config.bank.BankResolver;
import com.npst.observability.config.bank.PropertyBankResolver;
import com.npst.observability.exception.GlobalExceptionHandler;
import com.npst.observability.filter.TraceFilter;
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

/**
 * Wires the whole observability platform.
 *
 * <p>This class is the reason the starter is reusable. Previously the beans
 * were plain {@code @Component}s under {@code com.npst.observability}, so they
 * were discovered only because the sample application happened to share that
 * base package. Any real banking service - {@code com.bank.accounts} and the
 * like - would have started without a {@code CommonLogger} and failed on
 * injection.
 *
 * <p>Every bean is {@code @ConditionalOnMissingBean}, so a service can replace
 * any single piece without forking, and the whole configuration is gated on
 * {@code observability.enabled}.
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "observability", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BankResolver bankResolver(ObservabilityProperties properties,
                                     Environment environment) {
        return new PropertyBankResolver(properties, environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public MetadataMasker metadataMasker(ObservabilityProperties properties) {
        return new MetadataMasker(properties.getMasking());
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceRestTemplateInterceptor traceRestTemplateInterceptor() {
        return new TraceRestTemplateInterceptor();
    }

    /**
     * Propagates the correlation id on every RestTemplate the application
     * builds through RestTemplateBuilder.
     *
     * <p>Note what is deliberately absent: a {@code RestTemplate} bean. The
     * starter used to publish one, which then silently became the host
     * application's own RestTemplate. A library must not claim that name.
     */
    @Bean
    @ConditionalOnClass(RestTemplateCustomizer.class)
    @ConditionalOnMissingBean(name = "observabilityTraceRestTemplateCustomizer")
    public RestTemplateCustomizer observabilityTraceRestTemplateCustomizer(
            TraceRestTemplateInterceptor interceptor) {
        return restTemplate -> restTemplate.getInterceptors().add(interceptor);
    }

    /**
     * The sink chain: an HTTP sink to logging-api, wrapped by default in a
     * bounded async queue so a customer's request thread never waits on - or
     * fails because of - the observability platform.
     *
     * <p>Built in one bean method on purpose. Exposing the HTTP sink as a
     * separate bean looked tidier but was a trap: HttpLogSink <em>is</em> a
     * LogSink, so {@code @ConditionalOnMissingBean(LogSink.class)} on the
     * wrapper saw it and silently skipped the async layer, leaving every log
     * shipping inline on the request thread. One bean, one type, no ambiguity
     * - and an application overriding {@link LogSink} replaces the whole chain,
     * which is the sane unit of replacement anyway.
     */
    @Bean
    @ConditionalOnMissingBean(LogSink.class)
    public LogSink logSink(ObservabilityProperties properties,
                           TraceRestTemplateInterceptor interceptor,
                           ObjectProvider<ObjectMapper> objectMapper,
                           ObjectProvider<MeterRegistry> meterRegistry) {

        MeterRegistry registry = meterRegistry.getIfAvailable();

        HttpLogSink httpSink = new HttpLogSink(properties, interceptor,
                resolveMapper(objectMapper), registry);

        if (!properties.getSink().getAsync().isEnabled()) {
            return httpSink;
        }

        return new AsyncLogSink(httpSink, properties.getSink().getAsync(), registry);
    }

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

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(name = "observabilityTraceFilterRegistration")
    public FilterRegistrationBean<TraceFilter> observabilityTraceFilterRegistration(
            ObservabilityProperties properties) {

        FilterRegistrationBean<TraceFilter> registration =
                new FilterRegistrationBean<>(new TraceFilter(properties));

        // First in the chain: everything logged afterwards must already carry
        // the correlation id.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");

        return registration;
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

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

    /**
     * Off by default - see {@link GlobalExceptionHandler}. A starter has no
     * business handling the host application's exceptions unless asked to.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "observability.exception-handler", name = "enabled",
            havingValue = "true")
    @ConditionalOnMissingBean
    public GlobalExceptionHandler observabilityGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    private static ObjectMapper resolveMapper(ObjectProvider<ObjectMapper> provider) {
        return provider.getIfAvailable(ObservabilityAutoConfiguration::defaultObjectMapper);
    }

    /**
     * Fallback for a non-web application, where Spring Boot contributes no
     * ObjectMapper. JavaTimeModule matters: without it an Instant is written as
     * an epoch number instead of ISO-8601.
     */
    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}

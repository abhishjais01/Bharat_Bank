package com.npst.observability.sink;

import com.npst.observability.contract.LogIngestRequest;

/**
 * Where an enriched log goes after the SDK is done with it.
 *
 * <p>An interface rather than a concrete HTTP client because the destination
 * is expected to change: the platform architecture puts Kafka in front of the
 * log processor eventually. When that day comes a {@code KafkaLogSink} is an
 * additional bean, not a rewrite of {@link com.npst.observability.logger.CommonLogger}.
 *
 * <p>Implementations must never throw. Logging is a side channel; a customer's
 * balance enquiry has to succeed even when the observability platform is down.
 */
public interface LogSink {

    void send(LogIngestRequest request);
}

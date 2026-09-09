package com.bank.mock.cbs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Talks to the CBS over HTTP.
 *
 * <p>Built through {@link RestTemplateBuilder} on purpose. The observability
 * starter contributes a {@code RestTemplateCustomizer}, so every template built
 * this way carries the correlation id outbound automatically - this class
 * contains no tracing code and needs none. That is the behaviour a real service
 * inherits for free.
 */
@Component
public class HttpCbsClient implements CbsClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HttpCbsClient(RestTemplateBuilder builder,
                         @Value("${mock.cbs.base-url}") String baseUrl,
                         @Value("${mock.cbs.timeout:3s}") Duration timeout) {

        this.baseUrl = baseUrl;
        this.restTemplate = builder
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .build();
    }

    @Override
    public BigDecimal fetchBalance(String accountNumber, String simulate) {

        Map<String, Object> response = get(
                uri("/accounts/{account}/balance", simulate, accountNumber), Map.class);

        return new BigDecimal(String.valueOf(response.get("balance")));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchAccounts(String customerId, String simulate) {
        return get(uri("/customers/{customer}/accounts", simulate, customerId), List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchTransactions(String accountNumber,
                                                       LocalDate from,
                                                       LocalDate to,
                                                       String simulate) {

        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/accounts/{account}/transactions")
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParamIfPresent("simulate", java.util.Optional.ofNullable(simulate))
                .buildAndExpand(accountNumber)
                .toUriString();

        return get(uri, List.class);
    }

    @Override
    public String debit(String accountNumber, BigDecimal amount, String simulate) {

        Map<String, Object> request = Map.of(
                "accountNumber", accountNumber,
                "amount", amount);

        try {
            Map<String, Object> response =
                    restTemplate.postForObject(uri("/transfers", simulate), request, Map.class);

            return String.valueOf(response.get("cbsReference"));

        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            throw translate(ex.getStatusCode(), ex);

        } catch (RestClientException ex) {
            throw new CbsUnavailableException("CBS did not respond", ex);
        }
    }

    private <T> T get(String uri, Class<T> type) {
        try {
            return restTemplate.getForObject(uri, type);

        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            throw translate(ex.getStatusCode(), ex);

        } catch (RestClientException ex) {
            throw new CbsUnavailableException("CBS did not respond", ex);
        }
    }

    /**
     * A CBS outage and a business rejection reach us the same way - as a status
     * code - but they are very different events, and the logging platform has
     * to record them at different levels.
     */
    private static RuntimeException translate(HttpStatusCode status, Exception cause) {

        if (status.value() == 402) {
            return new com.bank.mock.exception.InsufficientFundsException(
                    "Insufficient balance in the debit account");
        }

        return new CbsUnavailableException("CBS returned " + status.value(), cause);
    }

    private String uri(String path, String simulate, Object... variables) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(path)
                .queryParamIfPresent("simulate", java.util.Optional.ofNullable(simulate))
                .buildAndExpand(variables)
                .toUriString();
    }
}

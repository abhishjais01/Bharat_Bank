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

// CBS client over HTTP (points at the mock CBS in this app)
@Component
public class HttpCbsClient implements CbsClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HttpCbsClient(RestTemplateBuilder builder,
                         @Value("${mock.cbs.base-url}") String baseUrl,
                         @Value("${mock.cbs.timeout:3s}") Duration timeout) {

        this.baseUrl = baseUrl;
        // built from RestTemplateBuilder so the trace id header is added automatically
        this.restTemplate = builder
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .build();
    }

    // GET the account balance
    @Override
    public BigDecimal fetchBalance(String accountNumber, String simulate) {

        Map<String, Object> response = get(
                uri("/accounts/{account}/balance", simulate, accountNumber), Map.class);

        return new BigDecimal(String.valueOf(response.get("balance")));
    }

    // GET the customer's accounts
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchAccounts(String customerId, String simulate) {
        return get(uri("/customers/{customer}/accounts", simulate, customerId), List.class);
    }

    // GET transactions for a date range
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

    // POST a debit and return the CBS reference
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

    // GET with the same error handling
    private <T> T get(String uri, Class<T> type) {
        try {
            return restTemplate.getForObject(uri, type);

        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            throw translate(ex.getStatusCode(), ex);

        } catch (RestClientException ex) {
            throw new CbsUnavailableException("CBS did not respond", ex);
        }
    }

    // 402 means insufficient funds, anything else is a CBS failure
    private static RuntimeException translate(HttpStatusCode status, Exception cause) {

        if (status.value() == 402) {
            return new com.bank.mock.exception.InsufficientFundsException(
                    "Insufficient balance in the debit account");
        }

        return new CbsUnavailableException("CBS returned " + status.value(), cause);
    }

    // build the CBS URL
    private String uri(String path, String simulate, Object... variables) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(path)
                .queryParamIfPresent("simulate", java.util.Optional.ofNullable(simulate))
                .buildAndExpand(variables)
                .toUriString();
    }
}

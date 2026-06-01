package com.personal.finance.transaction.client.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.personal.finance.common.exception.BaseException;
import com.personal.finance.common.exception.ErrorCode;
import com.personal.finance.transaction.config.TransactionProperties;
import com.personal.finance.transaction.enums.AccountStatus;
import com.personal.finance.transaction.exception.AccountClosedException;
import com.personal.finance.transaction.exception.AccountNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Wraps Account Service REST calls — sole job is to translate raw HTTP into
 * domain exceptions so service code never imports Spring web error types.
 *
 * <p>{@code X-User-Id} is forwarded on every outbound call per the prompt's
 * "Inter-service calls" rule.
 */
@Component
@Slf4j
public class AccountServiceClient {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final RestTemplate restTemplate;
    private final TransactionProperties properties;

    public AccountServiceClient(@Qualifier("accountServiceRestTemplate") RestTemplate restTemplate,
                                TransactionProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Spec §3.2 step 2 — confirm account exists, belongs to user, and is
     * ACTIVE. Throws {@link AccountNotFoundException} on 404,
     * {@link AccountClosedException} on CLOSED status.
     */
    public AccountSummary fetchActiveAccount(String userId, UUID accountId) {
        AccountSummary summary = fetch(userId, accountId);
        if (summary.getStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException(accountId);
        }
        return summary;
    }

    /** Same as {@link #fetchActiveAccount} but does not reject CLOSED. */
    public AccountSummary fetch(String userId, UUID accountId) {
        String url = properties.getAccountService().getBaseUrl() + "/v1/accounts/" + accountId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(USER_ID_HEADER, userId);
        try {
            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            return parse(accountId, resp.getBody());
        } catch (HttpClientErrorException.NotFound ex) {
            throw new AccountNotFoundException(accountId);
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Account Service call failed for accountId={} uid={}", accountId, userId, ex);
            throw new AccountServiceCallException(ex);
        }
    }

    /**
     * Unwraps finance-common's {@code ApiResponse} envelope:
     * {@code { "success": true, "data": { accountId, status, currency, ... }, ... }}.
     */
    private AccountSummary parse(UUID accountId, JsonNode body) {
        if (body == null) {
            throw new AccountServiceCallException(new IllegalStateException("Empty body from Account Service"));
        }
        JsonNode data = body.has("data") ? body.get("data") : body;
        if (data == null || data.isNull()) {
            throw new AccountNotFoundException(accountId);
        }
        AccountSummary out = new AccountSummary();
        out.setAccountId(UUID.fromString(data.get("accountId").asText()));
        out.setStatus(AccountStatus.valueOf(data.get("status").asText()));
        if (data.has("currency") && !data.get("currency").isNull()) {
            out.setCurrency(data.get("currency").asText());
        }
        return out;
    }

    /** Internal — translates any unexpected upstream failure into a 502-equivalent. */
    static class AccountServiceCallException extends BaseException {
        AccountServiceCallException(Throwable cause) {
            super(ErrorCode.EXT_001, HttpStatus.BAD_GATEWAY, cause);
        }
    }
}

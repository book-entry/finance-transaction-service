package com.personal.finance.transaction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view over {@code app.*} keys — Account Service client config. */
@Data
@ConfigurationProperties(prefix = "app")
public class TransactionProperties {

    private final AccountService accountService = new AccountService();

    @Data
    public static class AccountService {
        private String baseUrl = "http://localhost:8081";
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 5000;
    }
}

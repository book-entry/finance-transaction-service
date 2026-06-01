package com.personal.finance.transaction.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/** Single {@link RestTemplate} used by {@code AccountServiceClient}. */
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final TransactionProperties properties;

    @Bean("accountServiceRestTemplate")
    public RestTemplate accountServiceRestTemplate(RestTemplateBuilder builder) {
        TransactionProperties.AccountService cfg = properties.getAccountService();
        return builder
                .connectTimeout(Duration.ofMillis(cfg.getConnectTimeoutMs()))
                .readTimeout(Duration.ofMillis(cfg.getReadTimeoutMs()))
                .build();
    }
}

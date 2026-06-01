package com.personal.finance.transaction.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Transaction-service security chain — same pattern as
 * {@code AccountSecurityConfig}. Authorization is purely header-based
 * ({@code X-User-Id}); Spring Security is engaged only to preempt the
 * default HTTP Basic auto-config that finance-common drags onto the
 * classpath.
 */
@Configuration
@Slf4j
public class TransactionSecurityConfig {

    private static final String[] PERMITTED_PATTERNS = {
            "/v1/transactions/**",
            "/v1/categories/**",
            "/actuator/health",
            "/actuator/info"
    };

    @Bean("transactionSecurityFilterChain")
    @Order(0)
    public SecurityFilterChain transactionSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(PERMITTED_PATTERNS)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        log.info("Transaction-service security chain registered — permitting: {}",
                String.join(", ", PERMITTED_PATTERNS));
        return http.build();
    }
}

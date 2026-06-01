package com.personal.finance.transaction;

import com.personal.finance.transaction.config.TransactionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Transaction Service entry point — spec §3.2 / §3.3. Owns
 * {@code transaction_db} (transactions + categories). Calls Account Service
 * to validate account ownership on every transaction write.
 */
@SpringBootApplication
@EnableConfigurationProperties(TransactionProperties.class)
public class FinanceTransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceTransactionServiceApplication.class, args);
    }
}

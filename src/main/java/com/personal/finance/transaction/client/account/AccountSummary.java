package com.personal.finance.transaction.client.account;

import com.personal.finance.transaction.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Minimal projection of the Account Service response — only the fields the
 * transaction service consults. Mutable (Lombok {@code @Data}) so Jackson can
 * deserialise without a custom constructor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountSummary {
    private UUID accountId;
    private AccountStatus status;
    private String currency;
}

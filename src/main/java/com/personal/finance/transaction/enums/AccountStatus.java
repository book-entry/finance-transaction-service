package com.personal.finance.transaction.enums;

/**
 * Mirror of Account Service's status enum — used when deserialising the
 * account fetch response in {@code AccountServiceClient}. Keeping a local
 * copy avoids a circular module dependency on finance-account-service.
 */
public enum AccountStatus {
    ACTIVE,
    CLOSED
}

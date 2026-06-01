package com.personal.finance.transaction.exception;

import com.personal.finance.common.exception.BaseException;
import com.personal.finance.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Raised when Account Service returns 404 for a referenced account. The spec
 * does not distinguish "doesn't exist" from "not owned by user" by design —
 * both surface as 404 ACCOUNT_NOT_FOUND.
 */
public class AccountNotFoundException extends BaseException {
    public AccountNotFoundException(UUID accountId) {
        super(ErrorCode.ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND,
                "Account " + accountId + " not found for this user");
    }
}

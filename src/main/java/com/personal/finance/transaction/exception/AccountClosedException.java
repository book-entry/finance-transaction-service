package com.personal.finance.transaction.exception;

import com.personal.finance.common.exception.BaseException;
import com.personal.finance.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/** Spec §3.2: 422 when posting to a CLOSED account. */
public class AccountClosedException extends BaseException {
    public AccountClosedException(UUID accountId) {
        super(ErrorCode.ACCOUNT_CLOSED, HttpStatus.UNPROCESSABLE_ENTITY,
                "Account " + accountId + " is CLOSED and rejects new transactions");
    }
}

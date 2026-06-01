package com.personal.finance.transaction.exception;

import com.personal.finance.common.exception.BaseException;
import com.personal.finance.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** Raised when a transaction lookup yields no row OR the row is soft-deleted OR not owned by the user. */
public class TransactionNotFoundException extends BaseException {
    public TransactionNotFoundException(String message) {
        super(ErrorCode.TRANSACTION_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}

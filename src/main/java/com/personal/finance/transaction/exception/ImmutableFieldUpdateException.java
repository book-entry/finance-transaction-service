package com.personal.finance.transaction.exception;

import com.personal.finance.common.exception.BaseException;
import com.personal.finance.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * 422 — {@code PATCH /v1/transactions/{id}} attempted to mutate a field that
 * double-entry accounting hygiene says must stay frozen (account, entryType,
 * amount, currency, source). Callers who really need to change one of those
 * delete the transaction and create a new one.
 *
 * <p>Uses the generic {@code BUS_001} code rather than a dedicated one to
 * avoid a {@code finance-common} version bump.
 */
public class ImmutableFieldUpdateException extends BaseException {
    public ImmutableFieldUpdateException(List<String> fields) {
        super(ErrorCode.BUS_001, HttpStatus.UNPROCESSABLE_ENTITY,
                "Immutable field(s) cannot be updated via PATCH: " + String.join(", ", fields)
                        + " — delete and recreate the transaction instead.");
    }
}

package com.personal.finance.transaction.exception;

import com.personal.finance.common.exception.BaseException;
import com.personal.finance.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** Spec §3.2 PATCH /transactions/{id}/category — 400 when both fields are blank. */
public class InvalidCategoryRequestException extends BaseException {
    public InvalidCategoryRequestException() {
        super(ErrorCode.INVALID_CATEGORY_REQUEST, HttpStatus.BAD_REQUEST,
                "Exactly one of categoryId or categoryName must be provided");
    }
}

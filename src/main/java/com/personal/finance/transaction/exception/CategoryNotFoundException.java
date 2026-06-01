package com.personal.finance.transaction.exception;

import com.personal.finance.common.exception.BaseException;
import com.personal.finance.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class CategoryNotFoundException extends BaseException {
    public CategoryNotFoundException(String message) {
        super(ErrorCode.CATEGORY_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}

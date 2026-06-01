package com.personal.finance.transaction.exception;

import com.personal.finance.common.exception.BaseException;
import com.personal.finance.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class CategoryNameConflictException extends BaseException {
    public CategoryNameConflictException(String name) {
        super(ErrorCode.CATEGORY_NAME_CONFLICT, HttpStatus.CONFLICT,
                "Category name '" + name + "' already exists for this user");
    }
}

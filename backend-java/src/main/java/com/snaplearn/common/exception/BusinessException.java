package com.snaplearn.common.exception;

import lombok.Getter;

/**
 * @author 13934
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int httpStatus;

    public BusinessException(Exception e) {
        super(e);
        this.httpStatus = 500;
    }

    public BusinessException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }
}

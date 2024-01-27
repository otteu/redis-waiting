package com.example.flow.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ErrorCode {

    QUEUE_ALEADY_REGISTER_USER(HttpStatus.CONFLICT, "UQ-0001", "Already registerd in queue");

    private final HttpStatus httpStatus;
    private final String code;
    private final String reason;

    public AppException build() {
        return new AppException(httpStatus, code, reason);
    }

    public AppException build(Object ...args) {
        return new AppException(httpStatus,code, reason.formatted(args));
    }
}

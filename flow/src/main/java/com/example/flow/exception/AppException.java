package com.example.flow.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public class AppException extends RuntimeException {
    private HttpStatus httpStatus;
    private String code;
    private String reason;
}

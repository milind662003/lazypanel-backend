package com.milind.lazypanel.exception;

public class TokenRefreshException extends RuntimeException {
    public TokenRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.milind.lazypanel.exception;

import com.milind.lazypanel.dto.ErrorResponse;
import com.milind.lazypanel.util.CookieUtility;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Duration;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> createErrorResponseEntity(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message));
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshException(TokenRefreshException e, HttpServletResponse response) {

        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtility.createAccessTokenCookie("", Duration.ZERO).toString());

        return createErrorResponseEntity(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
        return createErrorResponseEntity(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(GoogleSheetsException.class)
    public ResponseEntity<ErrorResponse> handleGoogleSheetsException(GoogleSheetsException e) {
        return createErrorResponseEntity(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(GoogleTokenException.class)
    public ResponseEntity<ErrorResponse> handleGoogleTokenException(GoogleTokenException e) {
        return createErrorResponseEntity(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return createErrorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }
}

package com.niki.fluttrading.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps input-validation failures (e.g. an empty/missing uploaded file) to a
 * client error (400) instead of letting them fall through to the default
 * 500 Internal Server Error. Also catches any other unexpected exception,
 * logs it with full detail, and returns a generic (non-leaking) 500 response.
 * <p>
 * Extends {@link ResponseEntityExceptionHandler} so standard Spring MVC
 * exceptions (bean-validation failures, malformed JSON, unsupported media
 * type, missing multipart parts, etc.) keep their correct built-in status
 * codes (400/415/...) instead of being swallowed by the generic
 * {@code Exception} handler below.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Rejected request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception ex) {
        log.error("Unexpected error while handling request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred. Please try again later.");
    }
}


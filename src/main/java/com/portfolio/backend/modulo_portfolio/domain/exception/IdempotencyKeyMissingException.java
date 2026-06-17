package com.portfolio.backend.modulo_portfolio.domain.exception;

public class IdempotencyKeyMissingException extends RuntimeException {
    public IdempotencyKeyMissingException(String message) {
        super(message);
    }
}

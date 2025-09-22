package com.est.zouraPoc.exception;

public class TokenParsingException extends RuntimeException {
    public TokenParsingException(String message) {
        super(message);
    }

    public TokenParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.est.zouraPoc.exception;

import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for reactive OAuth token service
 * with specific handling for Redis and WebClient failures
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle Redis connection failures gracefully
     */
    @ExceptionHandler(RedisConnectionFailureException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleRedisConnectionFailure(RedisConnectionFailureException ex) {
        logger.warn("Redis connection failed - service will continue with fallback cache: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success_with_fallback");
        response.put("message", "Service is running with fallback cache due to Redis unavailability");
        response.put("cache_status", "fallback");
        response.put("service_available", true);

        return Mono.just(ResponseEntity.ok(response));
    }

    /**
     * Handle token generation exceptions
     */
    @ExceptionHandler(TokenGenerationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleTokenGenerationException(TokenGenerationException ex) {
        logger.error("Token generation failed: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Failed to generate OAuth token");
        response.put("error_type", "token_generation_failed");
        response.put("error_details", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Handle token parsing exceptions
     */
    @ExceptionHandler(TokenParsingException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleTokenParsingException(TokenParsingException ex) {
        logger.error("Token parsing failed: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Failed to parse OAuth token response");
        response.put("error_type", "token_parsing_failed");
        response.put("error_details", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response));
    }

    /**
     * Handle WebClient exceptions (network issues, timeouts)
     */
    @ExceptionHandler(WebClientException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleWebClientException(WebClientException ex) {
        logger.error("WebClient request failed: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Failed to communicate with OAuth server");
        response.put("error_type", "network_error");
        response.put("error_details", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response));
    }

    /**
     * Handle illegal argument exceptions (invalid client credentials)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.error("Invalid argument provided: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Invalid request parameters");
        response.put("error_type", "invalid_parameters");
        response.put("error_details", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    /**
     * Handle generic runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception occurred: {}", ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "An unexpected error occurred");
        response.put("error_type", "runtime_error");
        response.put("service_available", true);

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "An unexpected error occurred");
        response.put("error_type", "unexpected_error");
        response.put("timestamp", System.currentTimeMillis());

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(WebExchangeBindException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        response.put("errors", errors);
        response.put("message", "Validation failed");
        response.put("timestamp", Instant.now());

        return ResponseEntity.badRequest().body(response);
    }

}
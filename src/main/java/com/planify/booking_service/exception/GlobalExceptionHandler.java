package com.planify.booking_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler za REST API
 * Ujame izjeme iz fault tolerance fallbackov in pripravi ustrezne HTTP odgovore
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        // Preveri ali prihaja iz circuit breaker fallback
        if (ex.getMessage() != null && ex.getMessage().contains("temporarily unavailable")) {
            log.warn("Service unavailable due to circuit breaker: {}", ex.getMessage());
            
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now());
            body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
            body.put("error", "Service Unavailable");
            body.put("message", ex.getMessage());
            
            return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
        }
        
        // Za vse ostale runtime izjeme vrnemo kodo 500
        log.error("Internal server error", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred");
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

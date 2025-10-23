package com.example.bankcards.controller.advice;

import com.example.bankcards.dto.common.ApiError;
import com.example.bankcards.dto.common.ApiValidationError;
import com.example.bankcards.exception.BankCardsException;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.CryptoException;
import com.example.bankcards.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiValidationError> violations = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            violations.add(new ApiValidationError(fieldError.getField(), fieldError.getDefaultMessage(), fieldError.getRejectedValue()));
        }
        String message = "Validation failed";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiValidationError> violations = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            violations.add(new ApiValidationError(
                    violation.getPropertyPath().toString(),
                    violation.getMessage(),
                    violation.getInvalidValue()));
        }
        String message = "Validation failed";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request, violations);
    }

    @ExceptionHandler(BankCardsException.class)
    public ResponseEntity<ApiError> handleDomain(BankCardsException ex, HttpServletRequest request) {
        HttpStatus status = ex instanceof CryptoException ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        if (status.is5xxServerError()) {
            log.error("Critical application error", ex);
        }
        return buildErrorResponse(status, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request,
                List.of());
    }

    private ResponseEntity<ApiError> buildErrorResponse(
            HttpStatus status, String message, HttpServletRequest request, List<ApiValidationError> violations) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                violations == null || violations.isEmpty() ? null : violations);
        return ResponseEntity.status(status).body(error);
    }
}

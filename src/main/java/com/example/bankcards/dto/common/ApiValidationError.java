package com.example.bankcards.dto.common;

public record ApiValidationError(String field, String message, Object rejectedValue) {
}

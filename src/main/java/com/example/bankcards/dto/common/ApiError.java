package com.example.bankcards.dto.common;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<ApiValidationError> violations) {
}

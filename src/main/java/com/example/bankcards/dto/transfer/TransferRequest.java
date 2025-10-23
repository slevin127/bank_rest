package com.example.bankcards.dto.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "Source card id is required") UUID sourceCardId,
        @NotNull(message = "Target card id is required") UUID targetCardId,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
                BigDecimal amount,
        @Size(max = 255, message = "Description must be up to 255 characters") String description) {
}

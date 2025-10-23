package com.example.bankcards.dto.card;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CardCreateRequest(
        @NotNull(message = "Owner id is required") UUID ownerId,
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "\\d{16}", message = "Card number must contain exactly 16 digits")
                String cardNumber,
        @NotNull(message = "Expiration date is required")
        @Future(message = "Expiration date must be in the future")
                LocalDate expirationDate,
        @NotNull(message = "Initial balance is required")
        @DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
                BigDecimal initialBalance) {
}

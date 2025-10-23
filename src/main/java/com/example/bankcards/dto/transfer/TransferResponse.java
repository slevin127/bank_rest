package com.example.bankcards.dto.transfer;

import com.example.bankcards.entity.TransferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID sourceCardId,
        UUID targetCardId,
        BigDecimal amount,
        TransferStatus status,
        String description,
        LocalDateTime createdAt) {
}

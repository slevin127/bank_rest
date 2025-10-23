package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.NotNull;

public record CardStatusUpdateRequest(@NotNull(message = "Status is required") CardStatus status) {
}

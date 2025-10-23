package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardFilterRequest {

    private UUID ownerId;
    private CardStatus status;
    private String maskedNumber;
    private BigDecimal minBalance;
    private BigDecimal maxBalance;

    public boolean hasOwnerFilter() {
        return ownerId != null;
    }

    public boolean hasStatusFilter() {
        return status != null;
    }

    public boolean hasMaskedNumberFilter() {
        return maskedNumber != null && !maskedNumber.isBlank();
    }

    public boolean hasMinBalanceFilter() {
        return minBalance != null;
    }

    public boolean hasMaxBalanceFilter() {
        return maxBalance != null;
    }
}

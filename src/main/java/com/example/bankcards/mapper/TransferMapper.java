package com.example.bankcards.mapper;

import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.CardTransfer;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    public TransferResponse toResponse(CardTransfer transfer) {
        if (transfer == null) {
            return null;
        }
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceCard().getId(),
                transfer.getTargetCard().getId(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getDescription(),
                transfer.getCreatedAt());
    }
}

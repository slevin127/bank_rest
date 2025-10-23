package com.example.bankcards.mapper;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

    public CardResponse toResponse(Card card) {
        if (card == null) {
            return null;
        }
        return new CardResponse(
                card.getId(),
                card.getOwner().getId(),
                card.getOwner().getFullName(),
                card.getMaskedNumber(),
                card.getStatus(),
                card.getExpirationDate(),
                card.getBalance());
    }
}

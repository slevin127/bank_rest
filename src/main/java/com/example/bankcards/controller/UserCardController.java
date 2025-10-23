package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardFilterRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.security.AuthenticatedUser;
import com.example.bankcards.service.CardService;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Validated
public class UserCardController {

    private final CardService cardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public PageResponse<CardResponse> myCards(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String maskedNumber,
            @RequestParam(required = false) BigDecimal minBalance,
            @RequestParam(required = false) BigDecimal maxBalance,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        CardFilterRequest filter = new CardFilterRequest();
        filter.setStatus(status);
        filter.setMaskedNumber(maskedNumber);
        filter.setMinBalance(minBalance);
        filter.setMaxBalance(maxBalance);
        return cardService.findCardsForOwner(user.id(), filter, pageable);
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public CardResponse getCard(
            @AuthenticationPrincipal @NotNull AuthenticatedUser user, @PathVariable UUID cardId) {
        if (user.hasRole(Role.ADMIN)) {
            return cardService.getCard(cardId);
        }
        return cardService.getCardForOwner(cardId, user.id());
    }

    @PostMapping("/{cardId}/block-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('USER')")
    public CardResponse requestBlock(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID cardId) {
        return cardService.requestBlock(cardId, user.id());
    }
}

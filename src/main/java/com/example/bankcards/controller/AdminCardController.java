package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardFilterRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardStatusUpdateRequest;
import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@Validated
public class AdminCardController {

    private final CardService cardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse createCard(@Valid @RequestBody CardCreateRequest request) {
        return cardService.createCard(request);
    }

    @PatchMapping("/{cardId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse updateStatus(
            @PathVariable UUID cardId, @Valid @RequestBody CardStatusUpdateRequest request) {
        return cardService.updateStatus(cardId, request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<CardResponse> findCards(
            @ParameterObject CardFilterRequest filter,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return cardService.findCards(filter, pageable);
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse getCard(@PathVariable UUID cardId) {
        return cardService.getCard(cardId);
    }

    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCard(@PathVariable UUID cardId) {
        cardService.deleteCard(cardId);
    }
}

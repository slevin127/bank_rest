package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardFilterRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardStatusUpdateRequest;
import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardSpecifications;
import com.example.bankcards.util.CardMaskingUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final UserService userService;
    private final CryptoService cryptoService;
    private final CardMapper cardMapper;

    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        UserAccount owner = userService.requireActiveUser(request.ownerId());
        validateCardNumberExpiration(request.expirationDate());
        String maskedNumber = CardMaskingUtil.mask(request.cardNumber());
        if (cardRepository.existsByOwnerIdAndMaskedNumber(owner.getId(), maskedNumber)) {
            throw new BusinessException("Card with the same number already exists for this owner");
        }
        EncryptedData encrypted = cryptoService.encrypt(request.cardNumber());

        Card card = new Card();
        card.setOwner(owner);
        card.setMaskedNumber(maskedNumber);
        card.setEncryptedNumber(encrypted.cipherText());
        card.setEncryptionIv(encrypted.initializationVector());
        card.setExpirationDate(request.expirationDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(request.initialBalance());

        cardRepository.save(card);
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse updateStatus(UUID cardId, CardStatusUpdateRequest request) {
        Card card = requireCard(cardId);
        card.setStatus(request.status());
        return cardMapper.toResponse(card);
    }

    @Transactional(readOnly = true)
    public CardResponse getCard(UUID cardId) {
        return cardMapper.toResponse(requireCard(cardId));
    }

    @Transactional(readOnly = true)
    public CardResponse getCardForOwner(UUID cardId, UUID ownerId) {
        Card card = cardRepository
                .findByIdAndOwnerId(cardId, ownerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Card not found for the specified owner"));
        return cardMapper.toResponse(card);
    }

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> findCards(CardFilterRequest filter, Pageable pageable) {
        Page<Card> page = cardRepository.findAll(CardSpecifications.withFilter(filter), pageable);
        Page<CardResponse> mapped = page.map(cardMapper::toResponse);
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> findCardsForOwner(
            UUID ownerId, CardFilterRequest filter, Pageable pageable) {
        CardFilterRequest effectiveFilter = new CardFilterRequest();
        effectiveFilter.setOwnerId(ownerId);
        if (filter != null) {
            effectiveFilter.setStatus(filter.getStatus());
            effectiveFilter.setMaskedNumber(filter.getMaskedNumber());
            effectiveFilter.setMinBalance(filter.getMinBalance());
            effectiveFilter.setMaxBalance(filter.getMaxBalance());
        }
        Page<Card> page = cardRepository.findAll(CardSpecifications.withFilter(effectiveFilter), pageable);
        Page<CardResponse> mapped = page.map(cardMapper::toResponse);
        return PageResponse.from(mapped);
    }

    @Transactional
    public void deleteCard(UUID cardId) {
        Card card = requireCard(cardId);
        cardRepository.delete(card);
    }

    @Transactional
    public Card lockCard(UUID cardId) {
        return cardRepository
                .findWithLockingById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    @Transactional
    public void updateBalance(Card card, BigDecimal newBalance) {
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Card balance cannot become negative");
        }
        card.setBalance(newBalance);
    }

    @Transactional
    public CardResponse requestBlock(UUID cardId, UUID ownerId) {
        Card card = cardRepository
                .findByIdAndOwnerId(cardId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found for the specified owner"));
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BusinessException("Card is already blocked");
        }
        card.setStatus(CardStatus.BLOCKED);
        return cardMapper.toResponse(card);
    }

    Card requireCard(UUID cardId) {
        return cardRepository
                .findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    private void validateCardNumberExpiration(LocalDate expirationDate) {
        if (expirationDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Card expiration date must be in the future");
        }
    }
}

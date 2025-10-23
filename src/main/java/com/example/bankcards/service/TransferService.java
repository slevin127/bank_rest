package com.example.bankcards.service;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.CardTransfer;
import com.example.bankcards.entity.TransferStatus;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.repository.CardTransferRepository;
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
public class TransferService {

    private final CardService cardService;
    private final CardTransferRepository transferRepository;
    private final TransferMapper transferMapper;

    @Transactional
    public TransferResponse transferBetweenOwnCards(UUID userId, TransferRequest request) {
        if (request.sourceCardId().equals(request.targetCardId())) {
            throw new BusinessException("Source and target cards must differ");
        }
        Card sourceCard = cardService.lockCard(request.sourceCardId());
        Card targetCard = cardService.lockCard(request.targetCardId());

        validateOwnership(userId, sourceCard, targetCard);
        ensureTransferable(sourceCard);
        ensureTransferable(targetCard);

        BigDecimal amount = request.amount();
        if (sourceCard.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient funds on the source card");
        }

        cardService.updateBalance(sourceCard, sourceCard.getBalance().subtract(amount));
        cardService.updateBalance(targetCard, targetCard.getBalance().add(amount));

        CardTransfer transfer = new CardTransfer();
        transfer.setSourceCard(sourceCard);
        transfer.setTargetCard(targetCard);
        transfer.setAmount(amount);
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setDescription(request.description());
        transferRepository.save(transfer);

        return transferMapper.toResponse(transfer);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> getUserTransfers(UUID userId, Pageable pageable) {
        Page<CardTransfer> page = transferRepository
                .findBySourceCardOwnerIdOrTargetCardOwnerId(userId, userId, pageable);
        Page<TransferResponse> mapped = page.map(transferMapper::toResponse);
        return PageResponse.from(mapped);
    }

    private void validateOwnership(UUID userId, Card sourceCard, Card targetCard) {
        if (!userId.equals(sourceCard.getOwner().getId()) || !userId.equals(targetCard.getOwner().getId())) {
            throw new BusinessException("Cards must belong to the authenticated user");
        }
    }

    private void ensureTransferable(Card card) {
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BusinessException("Card is blocked");
        }
        if (card.getStatus() == CardStatus.EXPIRED || card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Card is expired");
        }
    }
}

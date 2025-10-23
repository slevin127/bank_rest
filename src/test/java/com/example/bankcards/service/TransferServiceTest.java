package com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.CardTransfer;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.TransferStatus;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.repository.CardTransferRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardService cardService;

    @Mock
    private CardTransferRepository transferRepository;

    @Mock
    private TransferMapper transferMapper;

    @InjectMocks
    private TransferService transferService;

    private UserAccount owner;
    private Card sourceCard;
    private Card targetCard;

    @BeforeEach
    void setUp() {
        owner = new UserAccount();
        owner.setId(UUID.randomUUID());
        owner.setUsername("user");
        owner.setFullName("User Test");
        owner.setRoles(Set.of(Role.USER));
        owner.setEnabled(true);

        sourceCard = new Card();
        sourceCard.setId(UUID.randomUUID());
        sourceCard.setOwner(owner);
        sourceCard.setStatus(CardStatus.ACTIVE);
        sourceCard.setExpirationDate(LocalDate.now().plusYears(1));
        sourceCard.setBalance(BigDecimal.valueOf(100));

        targetCard = new Card();
        targetCard.setId(UUID.randomUUID());
        targetCard.setOwner(owner);
        targetCard.setStatus(CardStatus.ACTIVE);
        targetCard.setExpirationDate(LocalDate.now().plusYears(1));
        targetCard.setBalance(BigDecimal.valueOf(50));
    }

    @Test
    void transferBetweenOwnCards_shouldUpdateBalancesAndPersist() {
        TransferRequest request = new TransferRequest(sourceCard.getId(), targetCard.getId(), BigDecimal.valueOf(40), "Savings");
        UUID userId = owner.getId();

        when(cardService.lockCard(sourceCard.getId())).thenReturn(sourceCard);
        when(cardService.lockCard(targetCard.getId())).thenReturn(targetCard);
        when(transferRepository.save(any(CardTransfer.class))).thenAnswer(invocation -> {
            CardTransfer transfer = invocation.getArgument(0);
            transfer.setId(UUID.randomUUID());
            transfer.setCreatedAt(LocalDateTime.now());
            return transfer;
        });
        TransferResponse response = new TransferResponse(
                UUID.randomUUID(),
                sourceCard.getId(),
                targetCard.getId(),
                request.amount(),
                TransferStatus.COMPLETED,
                request.description(),
                LocalDateTime.now());
        when(transferMapper.toResponse(any(CardTransfer.class))).thenReturn(response);

        TransferResponse result = transferService.transferBetweenOwnCards(userId, request);

        verify(cardService).updateBalance(sourceCard, BigDecimal.valueOf(60));
        verify(cardService).updateBalance(targetCard, BigDecimal.valueOf(90));

        ArgumentCaptor<CardTransfer> transferCaptor = ArgumentCaptor.forClass(CardTransfer.class);
        verify(transferRepository).save(transferCaptor.capture());
        assertEquals(request.amount(), transferCaptor.getValue().getAmount());
        assertEquals(response, result);
    }

    @Test
    void transferBetweenOwnCards_shouldFailWhenInsufficientFunds() {
        TransferRequest request = new TransferRequest(sourceCard.getId(), targetCard.getId(), BigDecimal.valueOf(400), null);
        when(cardService.lockCard(sourceCard.getId())).thenReturn(sourceCard);
        when(cardService.lockCard(targetCard.getId())).thenReturn(targetCard);

        assertThrows(BusinessException.class, () -> transferService.transferBetweenOwnCards(owner.getId(), request));
    }

    @Test
    void transferBetweenOwnCards_shouldFailWhenDifferentOwner() {
        UserAccount anotherOwner = new UserAccount();
        anotherOwner.setId(UUID.randomUUID());
        targetCard.setOwner(anotherOwner);
        TransferRequest request = new TransferRequest(sourceCard.getId(), targetCard.getId(), BigDecimal.ONE, null);

        when(cardService.lockCard(sourceCard.getId())).thenReturn(sourceCard);
        when(cardService.lockCard(targetCard.getId())).thenReturn(targetCard);

        assertThrows(BusinessException.class, () -> transferService.transferBetweenOwnCards(owner.getId(), request));
    }
}

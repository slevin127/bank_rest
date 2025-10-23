package com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
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
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserService userService;

    @Mock
    private CryptoService cryptoService;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    private UserAccount owner;
    private CardResponse cardResponse;

    @BeforeEach
    void setUp() {
        owner = new UserAccount();
        owner.setId(UUID.randomUUID());
        owner.setUsername("john");
        owner.setFullName("John Doe");
        owner.setRoles(Set.of(Role.USER));
        owner.setEnabled(true);

        cardResponse = new CardResponse(
                UUID.randomUUID(),
                owner.getId(),
                owner.getFullName(),
                "**** **** **** 1234",
                CardStatus.BLOCKED,
                LocalDate.now().plusYears(3),
                BigDecimal.TEN);
    }

    @Test
    void createCard_shouldPersistCardAndReturnResponse() {
        CardCreateRequest request = new CardCreateRequest(
                owner.getId(),
                "1111222233331234",
                LocalDate.now().plusYears(2),
                BigDecimal.TEN);
        when(userService.requireActiveUser(owner.getId())).thenReturn(owner);
        when(cardRepository.existsByOwnerIdAndMaskedNumber(eq(owner.getId()), any())).thenReturn(false);
        when(cryptoService.encrypt(request.cardNumber())).thenReturn(new EncryptedData("cipher", "iv"));
        when(cardMapper.toResponse(any(Card.class))).thenReturn(cardResponse);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse result = cardService.createCard(request);

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(captor.capture());
        Card saved = captor.getValue();
        assertEquals("**** **** **** 1234", saved.getMaskedNumber());
        assertEquals("cipher", saved.getEncryptedNumber());
        assertEquals(owner, saved.getOwner());
        assertEquals(cardResponse, result);
    }

    @Test
    void createCard_shouldThrowWhenDuplicateDetected() {
        CardCreateRequest request = new CardCreateRequest(
                owner.getId(),
                "1111222233331234",
                LocalDate.now().plusYears(2),
                BigDecimal.TEN);
        when(userService.requireActiveUser(owner.getId())).thenReturn(owner);
        when(cardRepository.existsByOwnerIdAndMaskedNumber(eq(owner.getId()), any())).thenReturn(true);

        assertThrows(BusinessException.class, () -> cardService.createCard(request));
    }

    @Test
    void updateBalance_shouldThrowWhenNegative() {
        Card card = new Card();
        card.setBalance(BigDecimal.ONE);

        assertThrows(BusinessException.class, () -> cardService.updateBalance(card, BigDecimal.valueOf(-1)));
    }

    @Test
    void requestBlock_shouldSetStatusToBlocked() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card();
        card.setOwner(owner);
        card.setStatus(CardStatus.ACTIVE);
        when(cardRepository.findByIdAndOwnerId(cardId, owner.getId())).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.requestBlock(cardId, owner.getId());

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        assertEquals(cardResponse, result);
    }

    @Test
    void requestBlock_shouldThrowWhenAlreadyBlocked() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card();
        card.setOwner(owner);
        card.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findByIdAndOwnerId(cardId, owner.getId())).thenReturn(Optional.of(card));

        assertThrows(BusinessException.class, () -> cardService.requestBlock(cardId, owner.getId()));
    }
}

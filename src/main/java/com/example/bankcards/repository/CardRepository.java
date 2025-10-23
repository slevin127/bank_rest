package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card> {

    Page<Card> findByOwnerId(UUID ownerId, Pageable pageable);

    Optional<Card> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByOwnerIdAndMaskedNumber(UUID ownerId, String maskedNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Card> findWithLockingById(UUID id);
}

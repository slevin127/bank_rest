package com.example.bankcards.repository;

import com.example.bankcards.entity.CardTransfer;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTransferRepository extends JpaRepository<CardTransfer, UUID> {

    Page<CardTransfer> findBySourceCardOwnerIdOrTargetCardOwnerId(UUID sourceOwnerId, UUID targetOwnerId, Pageable pageable);
}

package com.example.bankcards.repository;

import com.example.bankcards.entity.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    Page<UserAccount> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String username,
            String fullName,
            Pageable pageable);
}

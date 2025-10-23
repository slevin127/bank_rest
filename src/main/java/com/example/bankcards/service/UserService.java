package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserAccountRepository userRepository;

    public UserAccount getById(UUID id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserAccount requireActiveUser(UUID id) {
        UserAccount user = getById(id);
        if (!user.isEnabled()) {
            throw new BusinessException("User account is disabled");
        }
        return user;
    }

    public Optional<UserAccount> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<UserAccount> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public UserAccount save(UserAccount user, Set<Role> roles) {
        EnumSet<Role> roleSet = roles == null || roles.isEmpty()
                ? EnumSet.noneOf(Role.class)
                : EnumSet.copyOf(roles);
        user.setRoles(roleSet);
        return userRepository.save(user);
    }

    @Transactional
    public UserAccount update(UserAccount user) {
        return userRepository.save(user);
    }

    @Transactional
    public void updateLastLogin(UUID userId) {
        userRepository
                .findById(userId)
                .ifPresent(user -> {
                    user.setLastLoginAt(LocalDateTime.now());
                    userRepository.save(user);
                });
    }

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }
}

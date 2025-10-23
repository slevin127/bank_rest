package com.example.bankcards.dto.user;

import com.example.bankcards.entity.Role;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String fullName,
        String email,
        boolean enabled,
        Set<Role> roles,
        LocalDateTime lastLoginAt) {
}

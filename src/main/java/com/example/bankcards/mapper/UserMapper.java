package com.example.bankcards.mapper;

import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserAccount user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.isEnabled(),
                user.getRoles(),
                user.getLastLoginAt());
    }
}

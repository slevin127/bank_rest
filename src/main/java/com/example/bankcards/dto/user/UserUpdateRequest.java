package com.example.bankcards.dto.user;

import com.example.bankcards.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UserUpdateRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @Email(message = "Email must be valid") String email,
        @NotNull(message = "Roles are required")
        @NotEmpty(message = "Roles cannot be empty") Set<Role> roles,
        boolean enabled,
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters") String password) {
}

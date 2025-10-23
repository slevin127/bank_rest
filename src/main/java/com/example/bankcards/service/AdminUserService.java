package com.example.bankcards.service;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.user.UserCreateRequest;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.dto.user.UserUpdateRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.UserAccountRepository;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserAccountRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public AdminUserService(
            UserAccountRepository userRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    public PageResponse<UserResponse> listUsers(String search, Pageable pageable) {
        Page<UserAccount> page;
        if (search != null && !search.isBlank()) {
            String term = search.trim();
            page = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                    term, term, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(userMapper::toResponse));
    }

    public UserResponse getUser(UUID userId) {
        return userMapper.toResponse(userService.getById(userId));
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        userService.findByUsername(request.username()).ifPresent(u -> {
            throw new BusinessException("Username already in use");
        });
        if (request.email() != null && !request.email().isBlank()) {
            userService.findByEmail(request.email()).ifPresent(u -> {
                throw new BusinessException("Email already in use");
            });
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setEnabled(request.enabled());

        Set<Role> roles = EnumSet.copyOf(request.roles());
        userService.save(user, roles);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        UserAccount user = userService.getById(userId);

        if (request.email() != null && !Objects.equals(request.email(), user.getEmail())) {
            userService.findByEmail(request.email()).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new BusinessException("Email already in use");
                }
            });
            user.setEmail(request.email());
        }

        user.setFullName(request.fullName());
        user.setEnabled(request.enabled());
        user.setRoles(EnumSet.copyOf(request.roles()));

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        userService.update(user);
        return userMapper.toResponse(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        userService.delete(userId);
    }
}

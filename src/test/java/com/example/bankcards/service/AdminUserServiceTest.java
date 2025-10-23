package  com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.user.UserCreateRequest;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.dto.user.UserUpdateRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.example.bankcards.service.AdminUserService;
import com.example.bankcards.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void listUsers_shouldDelegateToRepository() {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        Page<UserAccount> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(new UserResponse(
                user.getId(), "user", "User", "user@example.com", true, Set.of(Role.USER), null));

        PageResponse<UserResponse> response = adminUserService.listUsers(null, PageRequest.of(0, 20));

        assertEquals(1, response.totalElements());
        verify(userRepository).findAll(any(PageRequest.class));
    }

    @Test
    void listUsers_shouldUseSearchWhenProvided() {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        Page<UserAccount> page = new PageImpl<>(List.of(user));
        when(userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(new UserResponse(
                user.getId(), "user", "User", "user@example.com", true, Set.of(Role.USER), null));

        adminUserService.listUsers("value", PageRequest.of(0, 20));

        verify(userRepository)
                .findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(anyString(), anyString(), any(PageRequest.class));
    }

    @Test
    void createUser_shouldPersistAndReturnResponse() {
        UserCreateRequest request = new UserCreateRequest(
                "username",
                "Password123",
                "Full Name",
                "user@example.com",
                Set.of(Role.USER),
                true);
        UserResponse response = new UserResponse(
                UUID.randomUUID(),
                request.username(),
                request.fullName(),
                request.email(),
                true,
                request.roles(),
                null);
        when(userService.findByUsername(request.username())).thenReturn(Optional.empty());
        when(userService.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(userMapper.toResponse(any(UserAccount.class))).thenReturn(response);
        when(userService.save(any(UserAccount.class), eq(request.roles()))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = adminUserService.createUser(request);

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userService).save(userCaptor.capture(), eq(request.roles()));
        UserAccount saved = userCaptor.getValue();
        assertEquals("hashed", saved.getPasswordHash());
        assertEquals(response, result);
    }

    @Test
    void createUser_shouldThrowWhenUsernameTaken() {
        UserCreateRequest request = new UserCreateRequest(
                "username",
                "Password123",
                "Full Name",
                null,
                Set.of(Role.USER),
                true);
        when(userService.findByUsername(request.username())).thenReturn(Optional.of(new UserAccount()));

        assertThrows(BusinessException.class, () -> adminUserService.createUser(request));
    }

    @Test
    void updateUser_shouldUpdateFieldsAndEncodePassword() {
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setEmail("old@example.com");
        user.setFullName("Old Name");
        user.setEnabled(true);
        user.setPasswordHash("old");
        UserUpdateRequest request = new UserUpdateRequest(
                "New Name",
                "new@example.com",
                Set.of(Role.ADMIN),
                false,
                "NewPassword123");
        when(userService.getById(userId)).thenReturn(user);
        when(userService.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(userMapper.toResponse(user)).thenReturn(new UserResponse(
                userId,
                "username",
                request.fullName(),
                request.email(),
                request.enabled(),
                request.roles(),
                null));

        UserResponse response = adminUserService.updateUser(userId, request);

        assertEquals("New Name", user.getFullName());
        assertEquals("new@example.com", user.getEmail());
        assertEquals(false, user.isEnabled());
        assertEquals("hashed", user.getPasswordHash());
        assertEquals(response.fullName(), request.fullName());
    }

    @Test
    void updateUser_shouldThrowWhenEmailTakenByAnotherUser() {
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setEmail("old@example.com");
        UserAccount another = new UserAccount();
        another.setId(UUID.randomUUID());
        when(userService.getById(userId)).thenReturn(user);
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.of(another));
        UserUpdateRequest request = new UserUpdateRequest(
                "Name",
                "new@example.com",
                Set.of(Role.USER),
                true,
                null);

        assertThrows(BusinessException.class, () -> adminUserService.updateUser(userId, request));
    }
}

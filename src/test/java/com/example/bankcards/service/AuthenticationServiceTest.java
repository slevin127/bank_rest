package com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankcards.config.AppProperties;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RefreshTokenRequest;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.dto.auth.TokenResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.security.AuthenticatedUser;
import com.example.bankcards.security.BankUserDetailsService;
import com.example.bankcards.security.JwtService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private BankUserDetailsService userDetailsService;

    private AppProperties properties;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getJwt().setAccessTokenExpirationMinutes(60);
        properties.getJwt().setRefreshTokenExpirationDays(7);
        properties.getJwt().setIssuer("test-issuer");
        authenticationService = new AuthenticationService(authenticationManager, passwordEncoder, userService, jwtService, userDetailsService, properties);
    }

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest request = new RegisterRequest("user", "password123", "User Name", "user@example.com");
        when(userService.findByUsername(request.username())).thenReturn(Optional.empty());
        when(userService.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        when(userService.save(userCaptor.capture(), eq(Set.of(Role.USER)))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(AuthenticatedUser.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(AuthenticatedUser.class))).thenReturn("refresh");

        TokenResponse response = authenticationService.register(request);

        UserAccount saved = userCaptor.getValue();
        assertEquals("user", saved.getUsername());
        assertEquals("hashed", saved.getPasswordHash());
        assertEquals("User Name", saved.getFullName());
        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        assertEquals(60L * 60, response.expiresIn());
    }

    @Test
    void register_shouldThrowWhenUsernameExists() {
        RegisterRequest request = new RegisterRequest("user", "password123", "User Name", "user@example.com");
        when(userService.findByUsername(request.username())).thenReturn(Optional.of(new UserAccount()));

        assertThrows(BusinessException.class, () -> authenticationService.register(request));
    }

    @Test
    void login_shouldAuthenticateAndReturnTokens() {
        LoginRequest request = new LoginRequest("user", "pass");
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setUsername("user");
        user.setPasswordHash("hashed");
        user.setRoles(Set.of(Role.USER));
        user.setEnabled(true);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(jwtService.generateAccessToken(authenticatedUser)).thenReturn("access");
        when(jwtService.generateRefreshToken(authenticatedUser)).thenReturn("refresh");

        TokenResponse response = authenticationService.login(request);

        verify(userService).updateLastLogin(user.getId());
        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
    }

    @Test
    void refresh_shouldReturnNewTokensWhenValid() {
        String refreshToken = "refresh";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setUsername("user");
        user.setPasswordHash("hashed");
        user.setRoles(Set.of(Role.USER));
        user.setEnabled(true);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);

        when(jwtService.isExpired(refreshToken)).thenReturn(false);
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUsername(refreshToken)).thenReturn("user");
        when(userDetailsService.loadUserByUsername("user")).thenReturn(authenticatedUser);
        when(jwtService.generateAccessToken(authenticatedUser)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(authenticatedUser)).thenReturn("new-refresh");

        TokenResponse response = authenticationService.refresh(request);

        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
    }

    @Test
    void refresh_shouldThrowWhenTokenInvalid() {
        String refreshToken = "invalid";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        when(jwtService.isExpired(refreshToken)).thenReturn(true);

        assertThrows(BusinessException.class, () -> authenticationService.refresh(request));
    }
}

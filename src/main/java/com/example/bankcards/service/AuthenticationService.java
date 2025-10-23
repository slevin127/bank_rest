package com.example.bankcards.service;

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
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final JwtService jwtService;
    private final BankUserDetailsService userDetailsService;
    private final AppProperties properties;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        userService.findByUsername(request.username()).ifPresent(u -> {
            throw new BusinessException("Username already in use");
        });
        if (request.email() != null) {
            userService.findByEmail(request.email()).ifPresent(u -> {
                throw new BusinessException("Email already in use");
            });
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setEnabled(true);
        user.setLastLoginAt(LocalDateTime.now());

        userService.save(user, Set.of(Role.USER));
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);
        return buildTokenResponse(authenticatedUser);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        userService.updateLastLogin(user.id());
        return buildTokenResponse(user);
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        if (jwtService.isExpired(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new BusinessException("Refresh token is invalid or expired");
        }
        String username = jwtService.extractUsername(refreshToken);
        AuthenticatedUser user = (AuthenticatedUser) userDetailsService.loadUserByUsername(username);
        return buildTokenResponse(user);
    }

    private TokenResponse buildTokenResponse(AuthenticatedUser user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        long expiresIn = properties.getJwt().getAccessTokenExpirationMinutes() * 60L;
        return TokenResponse.of(accessToken, refreshToken, expiresIn);
    }
}

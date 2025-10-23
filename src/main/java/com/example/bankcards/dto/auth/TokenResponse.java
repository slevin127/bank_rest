package com.example.bankcards.dto.auth;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType) {

    public static TokenResponse of(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, refreshToken, expiresInSeconds, "Bearer");
    }
}

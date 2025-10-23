package com.example.bankcards.security;

import com.example.bankcards.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";

    private final AppProperties properties;
    private final Key signingKey;

    public JwtService(AppProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(AuthenticatedUser user) {
        long expirationMinutes = properties.getJwt().getAccessTokenExpirationMinutes();
        return buildToken(user, ACCESS_TOKEN, expirationMinutes, ChronoUnit.MINUTES);
    }

    public String generateRefreshToken(AuthenticatedUser user) {
        long expirationDays = properties.getJwt().getRefreshTokenExpirationDays();
        return buildToken(user, REFRESH_TOKEN, expirationDays, ChronoUnit.DAYS);
    }

    public boolean isTokenValid(String token, AuthenticatedUser user) {
        Claims claims = extractAllClaims(token);
        String username = claims.getSubject();
        return username.equals(user.getUsername()) && !isTokenExpired(claims);
    }

    public boolean isRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        return REFRESH_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isExpired(String token) {
        return isTokenExpired(extractAllClaims(token));
    }

    private String buildToken(
            AuthenticatedUser user, String tokenType, long amountToAdd, ChronoUnit unit) {
        Instant now = Instant.now();
        Instant expiry = now.plus(amountToAdd, unit);
        return Jwts.builder()
                .setClaims(Map.of(TOKEN_TYPE_CLAIM, tokenType))
                .setSubject(user.getUsername())
                .setId(user.id().toString())
                .setIssuer(properties.getJwt().getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

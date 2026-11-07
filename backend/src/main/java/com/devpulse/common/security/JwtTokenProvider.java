package com.devpulse.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long tokenValidityInMs;
    private final long refreshTokenValidityInMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:devpulse-secret-key-devpulse-monitoring-saas-platform-2026-very-secure-key}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long tokenValidityInMs, // 24 hours
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshTokenValidityInMs // 7 days
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenValidityInMs = tokenValidityInMs;
        this.refreshTokenValidityInMs = refreshTokenValidityInMs;
    }

    public String generateAccessToken(UUID userId, String email, UUID orgId, String orgRole, String globalRole) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenValidityInMs);

        var builder = Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("globalRole", globalRole)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key);

        if (orgId != null) {
            builder.claim("orgId", orgId.toString());
        }
        if (orgRole != null) {
            builder.claim("orgRole", orgRole);
        }

        return builder.compact();
    }

    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityInMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}


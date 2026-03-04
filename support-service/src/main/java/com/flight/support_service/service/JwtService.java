package com.flight.support_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ── key ──────────────────────────────────────────────────────────────────

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── extraction helpers ────────────────────────────────────────────────────

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        // Support both "role" and "roles" claim keys used by auth-service
        Object role = claims.get("role");
        if (role == null) {
            role = claims.get("roles");
        }
        return role != null ? role.toString() : "ROLE_USER";
    }

    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userId = claims.get("userId");
        return userId != null ? userId.toString() : extractUsername(token);
    }

    // ── validation ────────────────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token, String username) {
        try {
            String extracted = extractUsername(token);
            return extracted.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}

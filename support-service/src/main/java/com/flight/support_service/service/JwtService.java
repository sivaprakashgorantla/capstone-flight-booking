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

    /** Returns JWT subject — which is the username (e.g. "admin"). */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Returns the user's actual email address from the "email" claim
     * (added by auth-service since the fix). Falls back to subject if absent.
     */
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        Object email = claims.get("email");
        return (email != null && !email.toString().isBlank())
                ? email.toString()
                : extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the role string. auth-service now sets "role" (singular) = "ADMIN"/"USER".
     * Falls back to "roles" list if "role" is absent (old tokens).
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        Object role = claims.get("role");
        if (role == null) {
            role = claims.get("roles");
        }
        return role != null ? role.toString() : "ROLE_USER";
    }

    /** Returns userId from the "userId" claim; falls back to username if absent. */
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

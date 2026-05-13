package com.library.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

// Utility class for creating and validating JWT tokens
// JWT = JSON Web Token — a compact, digitally signed way to carry user identity
// The token is signed with a secret key, so it cannot be forged without it
@Component
public class JwtUtil {

    // Secret key used to sign the token — loaded from Vault or application.yml
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Token validity period in milliseconds (default: 24 hours)
    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    // Convert the secret string into a cryptographic signing key
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Generate a new JWT token after a successful login
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)                              // Role stored as a custom claim
                .issuedAt(new Date())                            // Token creation timestamp
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())                       // HMAC-SHA256 digital signature
                .compact();
    }

    // Extract the username from a valid token
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // Returns true if the token is valid (not expired and not tampered with)
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token is expired, forged, or malformed
            return false;
        }
    }

    // Parse the token and extract its payload (claims)
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

package com.library.api;

import com.library.api.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

// Unit tests for JwtUtil — covers token generation, validation, and expiry
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Must be at least 32 characters for HMAC-SHA256
    private static final String TEST_SECRET =
            "test-secret-key-that-is-at-least-256-bits-long-for-testing";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inject @Value fields manually since there is no Spring context in unit tests
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 3600000L); // 1 hour
    }

    @Test
    @DisplayName("generateToken: should return a non-blank token string")
    void generateToken_shouldReturnToken() {
        String token = jwtUtil.generateToken("user1", "ROLE_USER");

        assertThat(token).isNotBlank();
        // JWT format: three Base64 parts separated by dots
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractUsername: should return the correct username from the token")
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken("user1", "ROLE_USER");

        String extracted = jwtUtil.extractUsername(token);

        assertThat(extracted).isEqualTo("user1");
    }

    @Test
    @DisplayName("isTokenValid: should return true for a freshly generated token")
    void isTokenValid_shouldReturnTrue_forValidToken() {
        String token = jwtUtil.generateToken("admin", "ROLE_ADMIN");

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid: should return false for a tampered token")
    void isTokenValid_shouldReturnFalse_forTamperedToken() {
        String token = jwtUtil.generateToken("user1", "ROLE_USER");

        // Tamper with the signature part (last segment)
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidsignature";

        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid: should return false for an expired token")
    void isTokenValid_shouldReturnFalse_forExpiredToken() {
        // Set expiration to -1 second (already expired)
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", -1000L);

        String expiredToken = jwtUtil.generateToken("user1", "ROLE_USER");

        assertThat(jwtUtil.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid: should return false for a completely invalid string")
    void isTokenValid_shouldReturnFalse_forGarbageInput() {
        assertThat(jwtUtil.isTokenValid("this.is.not.a.jwt")).isFalse();
        assertThat(jwtUtil.isTokenValid("")).isFalse();
        assertThat(jwtUtil.isTokenValid("randomstring")).isFalse();
    }

    @Test
    @DisplayName("generateToken: different users should get different tokens")
    void generateToken_shouldProduceDifferentTokensForDifferentUsers() {
        String token1 = jwtUtil.generateToken("user1", "ROLE_USER");
        String token2 = jwtUtil.generateToken("admin", "ROLE_ADMIN");

        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtUtil.extractUsername(token1)).isEqualTo("user1");
        assertThat(jwtUtil.extractUsername(token2)).isEqualTo("admin");
    }
}

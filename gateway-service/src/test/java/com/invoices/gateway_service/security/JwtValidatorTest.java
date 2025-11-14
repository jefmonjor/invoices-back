package com.invoices.gateway_service.security;

import com.invoices.gateway_service.exception.InvalidTokenException;
import com.invoices.gateway_service.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JWT validation logic.
 * Tests cover token validation, expiration, signature verification, and claims extraction.
 */
class JwtValidatorTest {

    private JwtValidator jwtValidator;
    private String secretKey;
    private String issuer;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        // Use a test secret key (must be at least 32 characters for HS256)
        secretKey = "test-secret-key-for-jwt-validation-that-is-long-enough-32-chars";
        issuer = "test-issuer";
        signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        jwtValidator = new JwtValidator();
        ReflectionTestUtils.setField(jwtValidator, "secret", secretKey);
        ReflectionTestUtils.setField(jwtValidator, "issuer", issuer);
    }

    @Test
    void shouldValidateValidToken() {
        // Given
        String token = createValidToken("testuser", 3600000); // 1 hour

        // When
        boolean isValid = jwtValidator.isValid(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldExtractUsernameFromToken() {
        // Given
        String username = "john.doe";
        String token = createValidToken(username, 3600000);

        // When
        String extractedUsername = jwtValidator.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void shouldExtractAllClaimsFromToken() {
        // Given
        String token = createValidToken("testuser", 3600000);

        // When
        Claims claims = jwtValidator.extractClaims(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.getIssuer()).isEqualTo(issuer);
    }

    @Test
    void shouldRejectExpiredToken() {
        // Given - token expired 1 hour ago
        String token = createValidToken("testuser", -3600000);

        // When
        boolean isValid = jwtValidator.isValid(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() {
        // Given - create token with different secret
        String differentSecret = "different-secret-key-for-jwt-that-is-long-enough-32-chars!!!";
        SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject("testuser")
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(differentKey)
                .compact();

        // When
        boolean isValid = jwtValidator.isValid(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectMalformedToken() {
        // Given
        String malformedToken = "this.is.not.a.valid.jwt.token";

        // When
        boolean isValid = jwtValidator.isValid(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectNullToken() {
        // When
        boolean isValid = jwtValidator.isValid(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectEmptyToken() {
        // When
        boolean isValid = jwtValidator.isValid("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldExtractClaimsFromValidToken() {
        // Given - token with different issuer (won't be validated by issuer in extractClaims)
        String token = createValidToken("testuser", 3600000);

        // When
        Claims claims = jwtValidator.extractClaims(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testuser");
    }

    @Test
    void shouldExtractUsernameFromTokenWithMultipleClaims() {
        // Given - token with additional claims
        String token = Jwts.builder()
                .subject("admin-user")
                .issuer(issuer)
                .claim("role", "ADMIN")
                .claim("email", "admin@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();

        // When
        String username = jwtValidator.extractUsername(token);
        Claims claims = jwtValidator.extractClaims(token);

        // Then
        assertThat(username).isEqualTo("admin-user");
        assertThat(claims.get("role")).isEqualTo("ADMIN");
        assertThat(claims.get("email")).isEqualTo("admin@example.com");
    }

    @Test
    void shouldValidateTokenJustBeforeExpiration() {
        // Given - token expires in 1 second
        String token = createValidToken("testuser", 1000);

        // When
        boolean isValid = jwtValidator.isValid(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldHandleTokenWithNoSubject() {
        // Given - token without subject
        String token = Jwts.builder()
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();

        // When - extract username should return null
        String username = jwtValidator.extractUsername(token);

        // Then
        assertThat(username).isNull();
    }

    @Test
    void shouldValidateMultipleTokensWithDifferentUsers() {
        // Given
        String token1 = createValidToken("user1", 3600000);
        String token2 = createValidToken("user2", 3600000);
        String token3 = createValidToken("user3", 3600000);

        // When/Then
        assertThat(jwtValidator.isValid(token1)).isTrue();
        assertThat(jwtValidator.isValid(token2)).isTrue();
        assertThat(jwtValidator.isValid(token3)).isTrue();

        assertThat(jwtValidator.extractUsername(token1)).isEqualTo("user1");
        assertThat(jwtValidator.extractUsername(token2)).isEqualTo("user2");
        assertThat(jwtValidator.extractUsername(token3)).isEqualTo("user3");
    }

    @Test
    void shouldRejectTokenWithInvalidFormat() {
        // Given
        String[] invalidTokens = {
                "invalid",
                "header.payload",  // Missing signature
                "...",  // Empty parts
                "Bearer token"  // With Bearer prefix
        };

        // When/Then
        for (String invalidToken : invalidTokens) {
            boolean isValid = jwtValidator.isValid(invalidToken);
            assertThat(isValid).isFalse();
        }
    }

    /**
     * Helper method to create a valid JWT token for testing
     */
    private String createValidToken(String username, long expirationMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }
}

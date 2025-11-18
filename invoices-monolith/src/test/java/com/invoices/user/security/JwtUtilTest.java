package com.invoices.user.security;

import com.invoices.user.exception.InvalidTokenException;
import com.invoices.user.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtUtil.
 * Tests JWT token generation, validation, and extraction.
 *
 * Tests real JWT functionality (not mocked).
 * Follows AAA pattern: Arrange-Act-Assert.
 */
@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    // JWT configuration
    private static final String SECRET = "mySecretKeyForJWTTokenGenerationThatNeedsToBeAtLeast256BitsLong12345678";
    private static final Long EXPIRATION = 3600000L; // 1 hour
    private static final String ISSUER = "user-service-test";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // Inject configuration values using reflection
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
        ReflectionTestUtils.setField(jwtUtil, "issuer", ISSUER);

        // Create test user details
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(authorities)
                .build();
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT token")
        void shouldGenerateValidToken() {
            // Act
            String token = jwtUtil.generateToken(userDetails);

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
        }

        @Test
        @DisplayName("Should include username in token")
        void shouldIncludeUsernameInToken() {
            // Act
            String token = jwtUtil.generateToken(userDetails);
            String username = jwtUtil.extractUsername(token);

            // Assert
            assertThat(username).isEqualTo(userDetails.getUsername());
        }

        @Test
        @DisplayName("Should include roles in token claims")
        void shouldIncludeRolesInToken() {
            // Act
            String token = jwtUtil.generateToken(userDetails);
            Claims claims = jwtUtil.extractAllClaims(token);

            // Assert
            assertThat(claims.get("roles")).isNotNull();
            assertThat(claims.get("roles").toString()).contains("ROLE_USER");
            assertThat(claims.get("roles").toString()).contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Should set correct issuer in token")
        void shouldSetCorrectIssuer() {
            // Act
            String token = jwtUtil.generateToken(userDetails);
            Claims claims = jwtUtil.extractAllClaims(token);

            // Assert
            assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        }

        @Test
        @DisplayName("Should set expiration time correctly")
        void shouldSetExpirationTimeCorrectly() {
            // Act
            String token = jwtUtil.generateToken(userDetails);
            Date expiration = jwtUtil.extractExpiration(token);

            // Assert
            assertThat(expiration).isNotNull();
            Date expectedExpiration = new Date(System.currentTimeMillis() + EXPIRATION);
            // Allow 1 second tolerance for test execution time
            assertThat(expiration).isCloseTo(expectedExpiration, within(1000L));
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            // Arrange
            UserDetails user1 = User.builder()
                    .username("user1@example.com")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            UserDetails user2 = User.builder()
                    .username("user2@example.com")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // Act
            String token1 = jwtUtil.generateToken(user1);
            String token2 = jwtUtil.generateToken(user2);

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate correct token successfully")
        void shouldValidateCorrectToken() {
            // Arrange
            String token = jwtUtil.generateToken(userDetails);

            // Act
            Boolean isValid = jwtUtil.validateToken(token, userDetails);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should validate token without user details")
        void shouldValidateTokenWithoutUserDetails() {
            // Arrange
            String token = jwtUtil.generateToken(userDetails);

            // Act
            Boolean isValid = jwtUtil.validateToken(token);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject token for wrong user")
        void shouldRejectTokenForWrongUser() {
            // Arrange
            String token = jwtUtil.generateToken(userDetails);

            UserDetails wrongUser = User.builder()
                    .username("wrong@example.com")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // Act
            Boolean isValid = jwtUtil.validateToken(token, wrongUser);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void shouldRejectMalformedToken() {
            // Arrange
            String malformedToken = "this.is.not.a.valid.jwt.token";

            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.validateToken(malformedToken))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should reject token with invalid signature")
        void shouldRejectTokenWithInvalidSignature() {
            // Arrange
            String token = jwtUtil.generateToken(userDetails);
            // Tamper with the token
            String tamperedToken = token.substring(0, token.length() - 10) + "tampered00";

            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.validateToken(tamperedToken))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should reject empty token")
        void shouldRejectEmptyToken() {
            // Arrange
            String emptyToken = "";

            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.validateToken(emptyToken))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should reject null token")
        void shouldRejectNullToken() {
            // Arrange
            String nullToken = null;

            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.validateToken(nullToken))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    @DisplayName("Token Expiration Tests")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Arrange - create JWT util with very short expiration
            JwtUtil shortExpirationJwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "secret", SECRET);
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "expiration", 1L); // 1ms expiration
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "issuer", ISSUER);

            String token = shortExpirationJwtUtil.generateToken(userDetails);

            // Wait for token to expire
            try {
                Thread.sleep(10); // Wait 10ms to ensure expiration
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act & Assert
            assertThatThrownBy(() -> shortExpirationJwtUtil.extractAllClaims(token))
                    .isInstanceOf(TokenExpiredException.class);
        }

        @Test
        @DisplayName("Should return false for expired token validation")
        void shouldReturnFalseForExpiredTokenValidation() {
            // Arrange - create JWT util with very short expiration
            JwtUtil shortExpirationJwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "secret", SECRET);
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "expiration", 1L);
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "issuer", ISSUER);

            String token = shortExpirationJwtUtil.generateToken(userDetails);

            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act
            Boolean isValid = shortExpirationJwtUtil.validateToken(token);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should extract expiration date correctly")
        void shouldExtractExpirationDateCorrectly() {
            // Arrange
            Date beforeGeneration = new Date();
            String token = jwtUtil.generateToken(userDetails);

            // Act
            Date expiration = jwtUtil.extractExpiration(token);

            // Assert
            assertThat(expiration).isAfter(beforeGeneration);
            assertThat(expiration).isBefore(new Date(System.currentTimeMillis() + EXPIRATION + 1000));
        }
    }

    @Nested
    @DisplayName("Claims Extraction Tests")
    class ClaimsExtractionTests {

        @Test
        @DisplayName("Should extract username from token")
        void shouldExtractUsername() {
            // Arrange
            String token = jwtUtil.generateToken(userDetails);

            // Act
            String username = jwtUtil.extractUsername(token);

            // Assert
            assertThat(username).isEqualTo(userDetails.getUsername());
        }

        @Test
        @DisplayName("Should extract all claims from token")
        void shouldExtractAllClaims() {
            // Arrange
            String token = jwtUtil.generateToken(userDetails);

            // Act
            Claims claims = jwtUtil.extractAllClaims(token);

            // Assert
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(userDetails.getUsername());
            assertThat(claims.getIssuer()).isEqualTo(ISSUER);
            assertThat(claims.get("roles")).isNotNull();
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
        }

        @Test
        @DisplayName("Should extract custom claim using function")
        void shouldExtractCustomClaimUsingFunction() {
            // Arrange
            String token = jwtUtil.generateToken(userDetails);

            // Act
            String issuer = jwtUtil.extractClaim(token, Claims::getIssuer);

            // Assert
            assertThat(issuer).isEqualTo(ISSUER);
        }

        @Test
        @DisplayName("Should extract issued date from token")
        void shouldExtractIssuedDate() {
            // Arrange
            Date beforeGeneration = new Date();
            String token = jwtUtil.generateToken(userDetails);

            // Act
            Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);

            // Assert
            assertThat(issuedAt).isNotNull();
            assertThat(issuedAt).isAfterOrEqualTo(beforeGeneration);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle user with single role")
        void shouldHandleUserWithSingleRole() {
            // Arrange
            UserDetails singleRoleUser = User.builder()
                    .username("singleRole@example.com")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // Act
            String token = jwtUtil.generateToken(singleRoleUser);
            Claims claims = jwtUtil.extractAllClaims(token);

            // Assert
            assertThat(claims.get("roles")).isNotNull();
            assertThat(claims.get("roles").toString()).contains("ROLE_USER");
        }

        @Test
        @DisplayName("Should handle user with no roles")
        void shouldHandleUserWithNoRoles() {
            // Arrange
            UserDetails noRolesUser = User.builder()
                    .username("noRoles@example.com")
                    .password("password")
                    .authorities(List.of())
                    .build();

            // Act
            String token = jwtUtil.generateToken(noRolesUser);
            Claims claims = jwtUtil.extractAllClaims(token);

            // Assert
            assertThat(token).isNotNull();
            assertThat(claims.get("roles")).isNotNull();
        }

        @Test
        @DisplayName("Should handle long username")
        void shouldHandleLongUsername() {
            // Arrange
            String longUsername = "very.long.email.address.that.might.cause.issues@example.com";
            UserDetails longUsernameUser = User.builder()
                    .username(longUsername)
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // Act
            String token = jwtUtil.generateToken(longUsernameUser);
            String extractedUsername = jwtUtil.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(longUsername);
        }

        @Test
        @DisplayName("Should handle special characters in username")
        void shouldHandleSpecialCharactersInUsername() {
            // Arrange
            String specialUsername = "user+tag@example.com";
            UserDetails specialUser = User.builder()
                    .username(specialUsername)
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // Act
            String token = jwtUtil.generateToken(specialUser);
            String extractedUsername = jwtUtil.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(specialUsername);
        }
    }
}

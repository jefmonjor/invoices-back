package com.invoices.gateway_service.security;

import com.invoices.gateway_service.config.SecurityProperties;
import com.invoices.gateway_service.exception.InvalidTokenException;
import com.invoices.gateway_service.exception.TokenExpiredException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for JWT Authentication Filter.
 * Tests the complete flow of JWT validation in the gateway filter chain.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtValidator jwtValidator;

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private String validToken;
    private String secretKey;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        // Setup test JWT token
        secretKey = "test-secret-key-for-jwt-validation-that-is-long-enough-32-chars";
        signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        validToken = Jwts.builder()
                .subject("testuser")
                .issuer("test-issuer")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();

        // Setup public paths
        List<String> publicPaths = Arrays.asList(
                "/api/auth/**",
                "/actuator/**",
                "/health",
                "/swagger-ui/**",
                "/v3/api-docs/**"
        );
        when(securityProperties.getPublicPaths()).thenReturn(publicPaths);
    }

    @Test
    void shouldAllowPublicPathWithoutToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtValidator, never()).isValid(anyString());
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void shouldAllowActuatorEndpointsWithoutToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getMethod()).thenReturn("GET");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtValidator, never()).isValid(anyString());
    }

    @Test
    void shouldAllowSwaggerUIWithoutToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
        when(request.getMethod()).thenReturn("GET");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtValidator, never()).isValid(anyString());
    }

    @Test
    void shouldAuthenticateWithValidToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/invoices");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtValidator.isValid(validToken)).thenReturn(true);
        when(jwtValidator.extractUsername(validToken)).thenReturn("testuser");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtValidator).isValid(validToken);
        verify(jwtValidator).extractUsername(validToken);
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());

        // Verify security context was set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("testuser");
    }

    @Test
    void shouldRejectProtectedPathWithoutToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/invoices");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).sendError(
                eq(HttpServletResponse.SC_UNAUTHORIZED),
                eq("Missing or invalid Authorization header")
        );
        verify(filterChain, never()).doFilter(request, response);
        verify(jwtValidator, never()).isValid(anyString());
    }

    @Test
    void shouldRejectTokenWithoutBearerPrefix() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/invoices");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(validToken); // Without "Bearer "

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).sendError(
                eq(HttpServletResponse.SC_UNAUTHORIZED),
                anyString()
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldRejectInvalidToken() throws ServletException, IOException {
        // Given
        String invalidToken = "invalid.jwt.token";
        when(request.getRequestURI()).thenReturn("/api/invoices");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtValidator.isValid(invalidToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtValidator).isValid(invalidToken);
        verify(response).sendError(
                eq(HttpServletResponse.SC_UNAUTHORIZED),
                eq("Invalid or expired JWT token")
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldRejectExpiredToken() throws ServletException, IOException {
        // Given
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuer("test-issuer")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // Expired 1 hour ago
                .signWith(signingKey)
                .compact();

        when(request.getRequestURI()).thenReturn("/api/invoices");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
        when(jwtValidator.isValid(expiredToken))
                .thenThrow(new TokenExpiredException("JWT token has expired"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).sendError(
                eq(HttpServletResponse.SC_UNAUTHORIZED),
                eq("JWT token has expired")
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldHandleInvalidTokenException() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/invoices");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtValidator.isValid(validToken))
                .thenThrow(new InvalidTokenException("Invalid JWT signature"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).sendError(
                eq(HttpServletResponse.SC_UNAUTHORIZED),
                eq("Invalid JWT token")
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldSetUsernameHeaderForDownstreamServices() throws ServletException, IOException {
        // Given
        String username = "john.doe";
        when(request.getRequestURI()).thenReturn("/api/invoices");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtValidator.isValid(validToken)).thenReturn(true);
        when(jwtValidator.extractUsername(validToken)).thenReturn(username);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(request).setAttribute("X-Auth-User", username);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateMultipleConsecutiveRequests() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/invoices");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtValidator.isValid(validToken)).thenReturn(true);
        when(jwtValidator.extractUsername(validToken)).thenReturn("testuser");

        // When - Make 3 consecutive requests
        for (int i = 0; i < 3; i++) {
            SecurityContextHolder.clearContext(); // Clear between requests
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        }

        // Then
        verify(jwtValidator, times(3)).isValid(validToken);
        verify(filterChain, times(3)).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void shouldHandleDifferentProtectedPaths() throws ServletException, IOException {
        // Given
        String[] protectedPaths = {
                "/api/invoices",
                "/api/users",
                "/api/documents",
                "/api/traces"
        };

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtValidator.isValid(validToken)).thenReturn(true);
        when(jwtValidator.extractUsername(validToken)).thenReturn("testuser");

        // When/Then
        for (String path : protectedPaths) {
            SecurityContextHolder.clearContext();
            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(jwtValidator, atLeastOnce()).isValid(validToken);
        }

        verify(filterChain, times(protectedPaths.length)).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void shouldHandleEmptyAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/invoices");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).sendError(
                eq(HttpServletResponse.SC_UNAUTHORIZED),
                eq("Missing or invalid Authorization header")
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldHandleBearerPrefixWithSpaces() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/invoices");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer   " + validToken); // Extra spaces
        when(jwtValidator.isValid(validToken)).thenReturn(true);
        when(jwtValidator.extractUsername(validToken)).thenReturn("testuser");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        // Should still extract token correctly (trimming is handled by substring)
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldHandleUnexpectedException() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/invoices");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtValidator.isValid(validToken))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).sendError(
                eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
                eq("Authentication error")
        );
        verify(filterChain, never()).doFilter(request, response);
    }
}

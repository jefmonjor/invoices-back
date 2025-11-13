package com.invoices.gateway_service.security;

import com.invoices.gateway_service.config.SecurityProperties;
import com.invoices.gateway_service.exception.InvalidTokenException;
import com.invoices.gateway_service.exception.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter that processes JWT authentication for each request in the Gateway.
 * Validates JWT tokens and sets authentication context.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USERNAME_HEADER = "X-Auth-User";

    private final JwtValidator jwtValidator;
    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Filters incoming requests to validate JWT tokens.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        log.debug("Processing request: {} {}", request.getMethod(), requestPath);

        // Check if path is public
        if (isPublicPath(requestPath)) {
            log.debug("Public path detected, skipping JWT validation: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt == null) {
                log.warn("No JWT token found in protected route: {}", requestPath);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
                return;
            }

            if (!jwtValidator.isValid(jwt)) {
                log.warn("Invalid JWT token for route: {}", requestPath);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }

            // Extract username and set authentication
            String username = jwtValidator.extractUsername(jwt);
            authenticateUser(username, request);

            // Add username to request header for downstream services
            request.setAttribute(USERNAME_HEADER, username);

            log.info("Successfully authenticated request for user: {} on path: {}", username, requestPath);

        } catch (InvalidTokenException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            return;
        } catch (TokenExpiredException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token has expired");
            return;
        } catch (Exception e) {
            log.error("Error processing JWT authentication: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Checks if the request path is a public path that doesn't require authentication.
     *
     * @param requestPath the request path
     * @return true if public path, false otherwise
     */
    private boolean isPublicPath(String requestPath) {
        return securityProperties.getPublicPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    /**
     * Extracts JWT token from the Authorization header.
     *
     * @param request the HTTP request
     * @return the JWT token or null if not present
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            log.debug("Extracted JWT token from request");
            return token;
        }

        return null;
    }

    /**
     * Sets the authentication context for the user.
     *
     * @param username the username
     * @param request the HTTP request
     */
    private void authenticateUser(String username, HttpServletRequest request) {
        // Create a simple authentication token
        // In gateway, we don't load full user details, just set basic authentication
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Set authentication for user: {}", username);
    }
}

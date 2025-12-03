package com.invoices.security;

import com.invoices.user.exception.InvalidTokenException;
import com.invoices.user.exception.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that processes JWT authentication for each request.
 * Extends OncePerRequestFilter to ensure a single execution per request.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /**
     * Filters incoming requests to validate JWT tokens.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateWithJwt(jwt, request);
            }
        } catch (InvalidTokenException e) {
            log.warn("Invalid JWT token in request: {}", e.getMessage());
        } catch (TokenExpiredException e) {
            log.warn("Expired JWT token in request: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing JWT authentication: {}", e.getMessage(), e);
        } finally {
            // Ensure chain continues and context is cleared
            try {
                filterChain.doFilter(request, response);
            } finally {
                // Clear company context to prevent data leakage in thread pool
                com.invoices.security.context.CompanyContext.clear();
            }
        }
    }

    // ... (skip other methods)

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
     * Authenticates the user using the JWT token.
     *
     * @param jwt     the JWT token
     * @param request the HTTP request
     */
    private void authenticateWithJwt(String jwt, HttpServletRequest request) {
        String username = jwtUtil.extractUsername(jwt);
        log.debug("Authenticating user: {}", username);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (jwtUtil.validateToken(jwt, userDetails)) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Set company context from token
            Long companyId = jwtUtil.extractCompanyId(jwt);
            if (companyId != null) {
                com.invoices.security.context.CompanyContext.setCompanyId(companyId);
                log.debug("Set company context to: {}", companyId);
            }

            log.info("Successfully authenticated user: {}", username);
        } else {
            log.warn("JWT token validation failed for user: {}", username);
        }
    }
}

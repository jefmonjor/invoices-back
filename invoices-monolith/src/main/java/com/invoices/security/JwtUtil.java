package com.invoices.security;

import com.invoices.user.exception.InvalidTokenException;
import com.invoices.user.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for generating and validating JWT tokens.
 * Uses HS256 algorithm for signing tokens.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.issuer}")
    private String issuer;

    /**
     * Generates a JWT token for the given user details.
     *
     * @param userDetails the user details
     * @return the generated JWT token
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Add roles to claims
        String roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        claims.put("roles", roles);

        // Add platformRole if present
        userDetails.getAuthorities().stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_PLATFORM_"))
                .findFirst()
                .ifPresent(a -> claims.put("platformRole", a.getAuthority().replace("ROLE_", "")));

        String token = createToken(claims, userDetails.getUsername());
        log.info("Generated JWT token for user: {}", userDetails.getUsername());

        return token;
    }

    /**
     * Generates a JWT token for the given email, roles and companyId.
     *
     * @param email     the user's email
     * @param roles     the user's roles
     * @param companyId the user's current company ID
     * @return the generated JWT token
     */
    public String generateToken(String email, java.util.Set<String> roles, Long companyId) {
        Map<String, Object> claims = new HashMap<>();

        // Add roles to claims
        String rolesString = String.join(",", roles);
        claims.put("roles", rolesString);

        // Note: platformRole should be passed if needed, but for now we rely on
        // UserDetails flow
        // or we could add an overload. For legacy/company flow, platformRole might not
        // be primary.

        if (companyId != null) {
            claims.put("companyId", companyId);
        }

        String token = createToken(claims, email);
        log.info("Generated JWT token for user: {} with companyId: {}", email, companyId);

        return token;
    }

    /**
     * Generates a JWT token for the given email and roles (legacy support).
     *
     * @param email the user's email
     * @param roles the user's roles
     * @return the generated JWT token
     */
    public String generateToken(String email, java.util.Set<String> roles) {
        return generateToken(email, roles, null);
    }

    /**
     * Extracts the companyId from the JWT token.
     *
     * @param token the JWT token
     * @return the companyId or null if not present
     */
    public Long extractCompanyId(String token) {
        return extractClaim(token, claims -> {
            Object companyId = claims.get("companyId");
            if (companyId instanceof Integer) {
                return ((Integer) companyId).longValue();
            } else if (companyId instanceof Long) {
                return (Long) companyId;
            }
            return null;
        });
    }

    /**
     * Creates a JWT token with the specified claims and subject.
     *
     * @param claims  the claims to include in the token
     * @param subject the subject (username/email)
     * @return the generated token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the username from the JWT token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the JWT token.
     *
     * @param token the JWT token
     * @return the expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the JWT token.
     *
     * @param token          the JWT token
     * @param claimsResolver function to resolve the claim
     * @param <T>            the type of the claim
     * @return the claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from the JWT token.
     *
     * @param token the JWT token
     * @return the claims
     * @throws InvalidTokenException if the token is invalid
     * @throws TokenExpiredException if the token has expired
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .requireIssuer(issuer) // Validate issuer to prevent tokens from other systems
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
            throw new TokenExpiredException("JWT token has expired", e);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Unsupported JWT token", e);
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Malformed JWT token", e);
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw new InvalidTokenException("Invalid JWT signature", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw new InvalidTokenException("JWT claims string is empty", e);
        }
    }

    /**
     * Checks if the token has expired.
     *
     * @param token the JWT token
     * @return true if expired, false otherwise
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (TokenExpiredException e) {
            return true;
        }
    }

    /**
     * Validates the JWT token against user details.
     *
     * @param token       the JWT token
     * @param userDetails the user details to validate against
     * @return true if valid, false otherwise
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);

            if (isValid) {
                log.info("JWT token validated successfully for user: {}", username);
            } else {
                log.warn("JWT token validation failed for user: {}", username);
            }

            return isValid;
        } catch (InvalidTokenException | TokenExpiredException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates the JWT token without user details.
     *
     * @param token the JWT token
     * @return true if valid, false otherwise
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            boolean isExpired = isTokenExpired(token);

            if (!isExpired) {
                log.info("JWT token is valid");
            }

            return !isExpired;
        } catch (InvalidTokenException | TokenExpiredException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets the expiration time in milliseconds.
     *
     * @return the expiration time in milliseconds
     */
    public Long getExpirationTime() {
        return expiration;
    }

    /**
     * Gets the signing key from the secret.
     *
     * @return the signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

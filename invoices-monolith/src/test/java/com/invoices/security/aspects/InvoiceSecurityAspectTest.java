package com.invoices.security.aspects;

import com.invoices.security.exceptions.PlatformAdminAccessDeniedException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test for InvoiceSecurityAspect
 * 
 * Verifies that PLATFORM_ADMIN users are blocked from accessing invoice-related
 * methods.
 */
class InvoiceSecurityAspectTest {

    private InvoiceSecurityAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new InvoiceSecurityAspect();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBlockPlatformAdminAccess() {
        // Given: User with PLATFORM_ADMIN role
        authenticateAs(List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN")));

        // Mock JoinPoint
        JoinPoint joinPoint = Mockito.mock(JoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TestMethod()");

        // When/Then: Accessing invoice method should throw exception
        assertThrows(PlatformAdminAccessDeniedException.class, () -> {
            aspect.blockPlatformAdminAccess(joinPoint);
        });
    }

    @Test
    void shouldAllowRegularUserAccess() {
        // Given: User with non-platform-admin roles
        authenticateAs(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")));

        // Mock JoinPoint
        JoinPoint joinPoint = Mockito.mock(JoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TestMethod()");

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> {
            aspect.blockPlatformAdminAccess(joinPoint);
        });
    }

    @Test
    void shouldAllowUnauthenticatedAccess() {
        // Given: No authentication
        SecurityContextHolder.clearContext();

        // Mock JoinPoint
        JoinPoint joinPoint = Mockito.mock(JoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TestMethod()");

        // When/Then: Should not throw exception (will be handled by Spring Security)
        assertDoesNotThrow(() -> {
            aspect.blockPlatformAdminAccess(joinPoint);
        });
    }

    private void authenticateAs(List<GrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("testuser", "password",
                authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

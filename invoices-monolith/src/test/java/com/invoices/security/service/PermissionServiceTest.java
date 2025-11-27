package com.invoices.security.service;

import com.invoices.company.infrastructure.persistence.entities.UserCompany;
import com.invoices.company.infrastructure.persistence.entities.UserCompanyId;
import com.invoices.company.infrastructure.persistence.repositories.UserCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PermissionService
 * Tests role-based access control for multi-company operations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService Tests")
class PermissionServiceTest {

    @Mock
    private UserCompanyRepository userCompanyRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PermissionService permissionService;

    private static final Long USER_ID = 1L;
    private static final Long COMPANY_ID = 100L;
    private static final Long INVOICE_ID = 1000L;

    // ==================== canEditInvoice Tests ====================

    @Test
    @DisplayName("ADMIN can edit any invoice")
    void canEditInvoice_AdminRole_ReturnsTrue() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        mockAdminRole();

        // When
        boolean result = permissionService.canEditInvoice(authentication, INVOICE_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("USER cannot edit invoice")
    void canEditInvoice_UserRole_ReturnsFalse() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        mockUserRole();

        // When
        boolean result = permissionService.canEditInvoice(authentication, INVOICE_ID);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Unauthenticated user cannot edit invoice")
    void canEditInvoice_Unauthenticated_ReturnsFalse() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        boolean result = permissionService.canEditInvoice(authentication, INVOICE_ID);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Null authentication cannot edit invoice")
    void canEditInvoice_NullAuth_ReturnsFalse() {
        // When
        boolean result = permissionService.canEditInvoice(null, INVOICE_ID);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== canDeleteInvoice Tests ====================

    @Test
    @DisplayName("ADMIN can delete any invoice")
    void canDeleteInvoice_AdminRole_ReturnsTrue() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        mockAdminRole();

        // When
        boolean result = permissionService.canDeleteInvoice(authentication, INVOICE_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("USER cannot delete invoice")
    void canDeleteInvoice_UserRole_ReturnsFalse() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        mockUserRole();

        // When
        boolean result = permissionService.canDeleteInvoice(authentication, INVOICE_ID);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== canManageUsers Tests ====================

    @Test
    @DisplayName("ADMIN can manage users in their company")
    void canManageUsers_AdminInCompany_ReturnsTrue() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        // mockAdminRole(); // Unnecessary
        mockUserCompanyAssociation(COMPANY_ID, "ADMIN");

        // When
        boolean result = permissionService.canManageUsers(authentication, COMPANY_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("USER cannot manage users")
    void canManageUsers_UserRole_ReturnsFalse() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        // mockUserRole(); // Unnecessary
        mockUserCompanyAssociation(COMPANY_ID, "USER");

        // When
        boolean result = permissionService.canManageUsers(authentication, COMPANY_ID);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("ADMIN not in company cannot manage users")
    void canManageUsers_AdminNotInCompany_ReturnsFalse() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        // mockAdminRole(); // Unnecessary
        when(userCompanyRepository.findById(any())).thenReturn(Optional.empty());

        // When
        boolean result = permissionService.canManageUsers(authentication, COMPANY_ID);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== canEditCompany Tests ====================

    @Test
    @DisplayName("Global ADMIN can edit any company")
    void canEditCompany_GlobalAdmin_ReturnsTrue() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        mockAdminRole();

        // When
        boolean result = permissionService.canEditCompany(authentication, COMPANY_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Company ADMIN can edit their company")
    void canEditCompany_CompanyAdmin_ReturnsTrue() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        mockUserRole(); // Not global admin
        mockUserCompanyAssociation(COMPANY_ID, "ADMIN");

        // When
        boolean result = permissionService.canEditCompany(authentication, COMPANY_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("USER cannot edit company")
    void canEditCompany_UserRole_ReturnsFalse() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        mockUserRole();
        mockUserCompanyAssociation(COMPANY_ID, "USER");

        // When
        boolean result = permissionService.canEditCompany(authentication, COMPANY_ID);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== canCreateCompany Tests ====================

    @Test
    @DisplayName("ADMIN can create company")
    void canCreateCompany_AdminRole_ReturnsTrue() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        mockAdminRole();

        // When
        boolean result = permissionService.canCreateCompany(authentication);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("USER cannot create company")
    void canCreateCompany_UserRole_ReturnsFalse() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        mockUserRole();

        // When
        boolean result = permissionService.canCreateCompany(authentication);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== isAdminInCompany Tests ====================

    @Test
    @DisplayName("User with ADMIN role in company returns true")
    void isAdminInCompany_AdminRole_ReturnsTrue() {
        // Given
        mockUserCompanyAssociation(COMPANY_ID, "ADMIN");

        // When
        boolean result = permissionService.isAdminInCompany(USER_ID, COMPANY_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("User with USER role in company returns false")
    void isAdminInCompany_UserRole_ReturnsFalse() {
        // Given
        mockUserCompanyAssociation(COMPANY_ID, "USER");

        // When
        boolean result = permissionService.isAdminInCompany(USER_ID, COMPANY_ID);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("User not in company returns false")
    void isAdminInCompany_NotInCompany_ReturnsFalse() {
        // Given
        when(userCompanyRepository.findById(any())).thenReturn(Optional.empty());

        // When
        boolean result = permissionService.isAdminInCompany(USER_ID, COMPANY_ID);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== getUserRoleInCompany Tests ====================

    @Test
    @DisplayName("Returns correct role for user in company")
    void getUserRoleInCompany_UserExists_ReturnsRole() {
        // Given
        mockUserCompanyAssociation(COMPANY_ID, "ADMIN");

        // When
        String role = permissionService.getUserRoleInCompany(USER_ID, COMPANY_ID);

        // Then
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Returns null for user not in company")
    void getUserRoleInCompany_UserNotExists_ReturnsNull() {
        // Given
        when(userCompanyRepository.findById(any())).thenReturn(Optional.empty());

        // When
        String role = permissionService.getUserRoleInCompany(USER_ID, COMPANY_ID);

        // Then
        assertThat(role).isNull();
    }

    // ==================== belongsToCompany Tests ====================

    @Test
    @DisplayName("User belongs to company returns true")
    void belongsToCompany_UserInCompany_ReturnsTrue() {
        // Given
        mockUserCompanyAssociation(COMPANY_ID, "USER");

        // When
        boolean result = permissionService.belongsToCompany(USER_ID, COMPANY_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("User not in company returns false")
    void belongsToCompany_UserNotInCompany_ReturnsFalse() {
        // Given
        when(userCompanyRepository.findById(any())).thenReturn(Optional.empty());

        // When
        boolean result = permissionService.belongsToCompany(USER_ID, COMPANY_ID);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Helper Methods ====================

    private void mockAdminRole() {
        Collection<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(authentication).getAuthorities();
    }

    private void mockUserRole() {
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(authentication).getAuthorities();
    }

    private void mockUserCompanyAssociation(Long companyId, String role) {
        UserCompanyId id = new UserCompanyId(USER_ID, companyId);
        UserCompany userCompany = new UserCompany();
        userCompany.setId(id);
        userCompany.setRole(role);

        when(userCompanyRepository.findById(id)).thenReturn(Optional.of(userCompany));
    }

    @BeforeEach
    void setUp() {
        // Default behavior for authentication name (User ID)
        lenient().when(authentication.getName()).thenReturn(String.valueOf(USER_ID));
    }
}

package com.invoices.user.domain.entities;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * User domain entity (Clean Architecture - pure POJO without framework dependencies)
 * Represents a user in the system with authentication and authorization capabilities.
 */
public class User {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final Long id;
    private final String email;
    private final String password; // Hashed password
    private final String firstName;
    private final String lastName;
    private final Set<String> roles;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime lastLogin;

    /**
     * Full constructor for creating a User entity
     */
    public User(Long id, String email, String password, String firstName, String lastName,
                Set<String> roles, boolean enabled, boolean accountNonExpired,
                boolean accountNonLocked, boolean credentialsNonExpired,
                LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime lastLogin) {
        validateEmail(email);
        validatePassword(password);

        this.id = id;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLogin = lastLogin;
    }

    /**
     * Simplified constructor for creating new users
     */
    public User(String email, String hashedPassword, String firstName, String lastName, Set<String> roles) {
        this(null, email, hashedPassword, firstName, lastName, roles,
             true, true, true, true,
             LocalDateTime.now(), LocalDateTime.now(), null);
    }

    // ==================== VALIDATION METHODS ====================

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
    }

    // ==================== BUSINESS METHODS ====================

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Check if user has admin role
     */
    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN") || roles.contains("ADMIN");
    }

    /**
     * Check if user account is active and valid
     */
    public boolean isAccountValid() {
        return enabled && accountNonExpired && accountNonLocked && credentialsNonExpired;
    }

    /**
     * Create a new User with updated last login time
     */
    public User withLastLogin(LocalDateTime lastLogin) {
        return new User(
            this.id, this.email, this.password, this.firstName, this.lastName,
            this.roles, this.enabled, this.accountNonExpired, this.accountNonLocked,
            this.credentialsNonExpired, this.createdAt, LocalDateTime.now(), lastLogin
        );
    }

    /**
     * Create a new User with updated password
     */
    public User withPassword(String newHashedPassword) {
        validatePassword(newHashedPassword);
        return new User(
            this.id, this.email, newHashedPassword, this.firstName, this.lastName,
            this.roles, this.enabled, this.accountNonExpired, this.accountNonLocked,
            this.credentialsNonExpired, this.createdAt, LocalDateTime.now(), this.lastLogin
        );
    }

    /**
     * Create a new User with disabled account
     */
    public User withDisabled() {
        return new User(
            this.id, this.email, this.password, this.firstName, this.lastName,
            this.roles, false, this.accountNonExpired, this.accountNonLocked,
            this.credentialsNonExpired, this.createdAt, LocalDateTime.now(), this.lastLogin
        );
    }

    /**
     * Create a new User with enabled account
     */
    public User withEnabled() {
        return new User(
            this.id, this.email, this.password, this.firstName, this.lastName,
            this.roles, true, this.accountNonExpired, this.accountNonLocked,
            this.credentialsNonExpired, this.createdAt, LocalDateTime.now(), this.lastLogin
        );
    }

    // ==================== GETTERS ====================

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        if (firstName != null) {
            return firstName;
        }
        if (lastName != null) {
            return lastName;
        }
        return email;
    }

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", roles=" + roles +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                '}';
    }
}

package com.invoices.user.domain.usecases;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.PasswordHasher;
import com.invoices.user.domain.ports.UserRepository;
import com.invoices.user.exception.UserNotFoundException;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Use case for updating an existing user.
 */
public class UpdateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UpdateUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Execute the use case to update a user
     *
     * @param userId        the user ID
     * @param firstName     new first name (null to keep current)
     * @param lastName      new last name (null to keep current)
     * @param plainPassword new password (null to keep current)
     * @param roles         new roles (null to keep current)
     * @param enabled       new enabled status (null to keep current)
     * @return the updated user
     * @throws UserNotFoundException if user doesn't exist
     */
    public User execute(Long userId, String firstName, String lastName,
            String plainPassword, Set<String> roles, Boolean enabled) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found with ID: " + userId));

        // Apply updates only if provided
        String updatedFirstName = firstName != null ? firstName : existingUser.getFirstName();
        String updatedLastName = lastName != null ? lastName : existingUser.getLastName();
        Set<String> updatedRoles = roles != null ? roles : existingUser.getRoles();
        boolean updatedEnabled = enabled != null ? enabled : existingUser.isEnabled();

        // Hash new password if provided
        String updatedPassword = existingUser.getPassword();
        if (plainPassword != null && !plainPassword.trim().isEmpty()) {
            updatedPassword = passwordHasher.hash(plainPassword);
        }

        // Create updated user
        User updatedUser = new User(
                existingUser.getId(),
                existingUser.getEmail(), // Email cannot be changed
                updatedPassword,
                updatedFirstName,
                updatedLastName,
                updatedRoles,
                updatedEnabled,
                existingUser.isAccountNonExpired(),
                existingUser.isAccountNonLocked(),
                existingUser.isCredentialsNonExpired(),
                existingUser.getCreatedAt(),
                LocalDateTime.now(), // Update timestamp
                existingUser.getLastLogin(),
                existingUser.getCurrentCompanyId());

        return userRepository.save(updatedUser);
    }
}

package com.invoices.user.mapper;

import com.invoices.user.dto.CreateUserRequest;
import com.invoices.user.dto.UpdateUserRequest;
import com.invoices.user.dto.UserDTO;
import com.invoices.user.entity.User;

import java.util.HashSet;
import java.util.Set;

/**
 * Mapper class for converting between User entity and DTOs.
 * Uses static methods for simple conversions.
 */
public class UserMapper {

    private UserMapper() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts CreateUserRequest DTO to User entity.
     * Does not hash the password - this must be done by the service layer.
     *
     * @param request the create user request
     * @return the user entity (without password hashing)
     */
    public static User toEntity(CreateUserRequest request) {
        if (request == null) {
            return null;
        }

        Set<String> roles = request.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
            roles.add("ROLE_USER");
        }

        return User.builder()
                .email(request.getEmail())
                .password(request.getPassword()) // Password should be hashed before calling this method
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(roles)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    /**
     * Converts User entity to UserDTO.
     * Excludes sensitive information like password.
     *
     * @param user the user entity
     * @return the user DTO
     */
    public static UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Updates an existing User entity with values from UpdateUserRequest.
     * Only updates non-null fields.
     *
     * @param entity the existing user entity to update
     * @param request the update request with new values
     */
    public static void updateEntityFromRequest(User entity, UpdateUserRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getFirstName() != null) {
            entity.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            entity.setLastName(request.getLastName());
        }

        if (request.getRoles() != null) {
            entity.setRoles(request.getRoles());
        }

        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
    }
}

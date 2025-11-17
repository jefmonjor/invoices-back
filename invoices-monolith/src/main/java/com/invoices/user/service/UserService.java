package com.invoices.user_service.service;

import com.invoices.user_service.dto.CreateUserRequest;
import com.invoices.user_service.dto.UpdateUserRequest;
import com.invoices.user_service.dto.UserDTO;
import com.invoices.user_service.entity.User;
import com.invoices.user_service.exception.UserAlreadyExistsException;
import com.invoices.user_service.exception.UserNotFoundException;
import com.invoices.user_service.mapper.UserMapper;
import com.invoices.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user operations.
 * Handles CRUD operations for users.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new user.
     * Validates that email doesn't already exist and hashes the password.
     *
     * @param request the create user request
     * @return the created user as DTO (without password)
     * @throws UserAlreadyExistsException if email already exists
     */
    public UserDTO createUser(CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        // Validate email doesn't exist
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Attempt to create user with existing email: {}", request.getEmail());
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }

        // Convert DTO to entity
        User user = UserMapper.toEntity(request);

        // Hash password with BCrypt (strength 10)
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(hashedPassword);

        // Save to database
        User savedUser = userRepository.save(user);
        log.info("Successfully created user with id: {} and email: {}", savedUser.getId(), savedUser.getEmail());

        // Return DTO without password
        return UserMapper.toDTO(savedUser);
    }

    /**
     * Retrieves a user by ID.
     *
     * @param id the user ID
     * @return the user DTO
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        log.info("Fetching user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new UserNotFoundException(id);
                });

        log.info("Successfully retrieved user with id: {}", id);
        return UserMapper.toDTO(user);
    }

    /**
     * Retrieves a user by email.
     *
     * @param email the user email
     * @return the user DTO
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UserNotFoundException("email", email);
                });

        log.info("Successfully retrieved user with email: {}", email);
        return UserMapper.toDTO(user);
    }

    /**
     * Updates an existing user.
     * Only updates non-null fields from the request.
     *
     * @param id the user ID to update
     * @param request the update request
     * @return the updated user DTO
     * @throws UserNotFoundException if user not found
     */
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user with id: {}", id);

        // Find existing user
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update - user not found with id: {}", id);
                    return new UserNotFoundException(id);
                });

        // Update only non-null fields
        UserMapper.updateEntityFromRequest(user, request);

        // Save updated user
        User updatedUser = userRepository.save(user);
        log.info("Successfully updated user with id: {}", id);

        return UserMapper.toDTO(updatedUser);
    }

    /**
     * Deletes a user by ID.
     * Performs soft delete by disabling the user account.
     *
     * @param id the user ID to delete
     * @throws UserNotFoundException if user not found
     */
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot delete - user not found with id: {}", id);
                    return new UserNotFoundException(id);
                });

        // Soft delete - disable the user instead of removing from database
        user.setEnabled(false);
        userRepository.save(user);

        log.info("Successfully deleted (disabled) user with id: {}", id);
    }

    /**
     * Permanently deletes a user by ID.
     * Use with caution - this is a hard delete.
     *
     * @param id the user ID to permanently delete
     * @throws UserNotFoundException if user not found
     */
    public void permanentlyDeleteUser(Long id) {
        log.info("Permanently deleting user with id: {}", id);

        if (!userRepository.existsById(id)) {
            log.warn("Cannot delete - user not found with id: {}", id);
            throw new UserNotFoundException(id);
        }

        userRepository.deleteById(id);
        log.info("Successfully permanently deleted user with id: {}", id);
    }

    /**
     * Retrieves all users.
     *
     * @return list of all users as DTOs
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        log.info("Fetching all users");

        List<User> users = userRepository.findAll();
        log.info("Successfully retrieved {} users", users.size());

        return users.stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }
}

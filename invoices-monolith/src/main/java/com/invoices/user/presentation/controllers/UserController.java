package com.invoices.user.presentation.controllers;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.usecases.*;
import com.invoices.user.presentation.dto.CreateUserRequest;
import com.invoices.user.presentation.dto.UpdateUserRequest;
import com.invoices.user.presentation.dto.UserDTO;
import com.invoices.user.presentation.mappers.UserDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for user management operations (Clean Architecture).
 * All endpoints require authentication.
 * Uses Use Cases from domain layer instead of service layer.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Users", description = "Endpoints for user management")
public class UserController {

        private final CreateUserUseCase createUserUseCase;
        private final GetAllUsersUseCase getAllUsersUseCase;
        private final GetUserByIdUseCase getUserByIdUseCase;
        private final GetUserByEmailUseCase getUserByEmailUseCase;
        private final UpdateUserUseCase updateUserUseCase;
        private final DeleteUserUseCase deleteUserUseCase;
        private final UserDtoMapper mapper;

        @GetMapping("/profile")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get current user profile", description = "Retrieves the profile of the currently authenticated user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
        })
        public ResponseEntity<UserDTO> getCurrentUserProfile() {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                log.info("GET /api/users/profile - Fetching profile for user: {}", email);

                User user = getUserByEmailUseCase.execute(email);
                UserDTO userDTO = mapper.toDTO(user);

                log.info("Profile retrieved successfully for user: {}", email);
                return ResponseEntity.ok(userDTO);
        }

        @PutMapping("/profile")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Update current user profile", description = "Updates the profile of the currently authenticated user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
        })
        public ResponseEntity<UserDTO> updateCurrentUserProfile(@Valid @RequestBody UpdateUserRequest request) {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                log.info("PUT /api/users/profile - Updating profile for user: {}", email);

                User user = getUserByEmailUseCase.execute(email);

                User updatedUser = updateUserUseCase.execute(
                                user.getId(),
                                request.getFirstName(),
                                request.getLastName(),
                                request.getPassword(),
                                request.getRoles(), // Allow roles to be updated via profile
                                request.getEnabled());
                UserDTO userDTO = mapper.toDTO(updatedUser);

                log.info("Profile updated successfully for user: {}", email);
                return ResponseEntity.ok(userDTO);
        }

        @GetMapping
        @Operation(summary = "Get all users", description = "Retrieves a list of all users. Available to all authenticated users.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
        })
        public ResponseEntity<List<UserDTO>> getAllUsers() {
                log.info("GET /api/users - Fetching all users");

                List<User> users = getAllUsersUseCase.execute();
                List<UserDTO> userDTOs = users.stream()
                                .map(mapper::toDTO)
                                .collect(Collectors.toList());

                log.info("Retrieved {} users", userDTOs.size());
                return ResponseEntity.ok(userDTOs);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get user by ID", description = "Retrieves a user by their ID. Users can only view their own profile unless they have ADMIN role.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User retrieved successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
        })
        public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
                log.info("GET /api/users/{} - Fetching user by ID", id);

                User user = getUserByIdUseCase.execute(id);

                // Check authorization: must be ADMIN or the user themselves
                checkUserAccess(user.getEmail());

                UserDTO userDTO = mapper.toDTO(user);

                log.info("User {} retrieved successfully", id);
                return ResponseEntity.ok(userDTO);
        }

        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Create a new user", description = "Creates a new user. Requires ADMIN role.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
                        @ApiResponse(responseCode = "409", description = "User with this email already exists", content = @Content)
        })
        public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
                log.info("POST /api/users - Creating new user with email: {}", request.getEmail());

                User createdUser = createUserUseCase.execute(
                                request.getEmail(),
                                request.getPassword(),
                                request.getFirstName(),
                                request.getLastName(),
                                request.getRoles());

                UserDTO userDTO = mapper.toDTO(createdUser);

                log.info("User created successfully with ID: {}", createdUser.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(userDTO);
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update a user", description = "Updates an existing user. Users can only update their own profile unless they have ADMIN role.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
        })
        public ResponseEntity<UserDTO> updateUser(
                        @PathVariable Long id,
                        @Valid @RequestBody UpdateUserRequest request) {
                log.info("PUT /api/users/{} - Updating user", id);

                // Get user first to check authorization
                User existingUser = getUserByIdUseCase.execute(id);
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

                // Check if user can access this profile
                checkUserAccess(existingUser.getEmail());

                // If not admin and trying to modify roles, deny
                if (!isAdmin && request.getRoles() != null) {
                        log.warn("Non-admin user {} attempted to modify roles for user {}",
                                        auth.getName(), id);
                        throw new AccessDeniedException("Only administrators can modify user roles");
                }

                User updatedUser = updateUserUseCase.execute(
                                id,
                                request.getFirstName(),
                                request.getLastName(),
                                request.getPassword(),
                                request.getRoles(),
                                request.getEnabled());

                UserDTO userDTO = mapper.toDTO(updatedUser);

                log.info("User {} updated successfully", id);
                return ResponseEntity.ok(userDTO);
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Delete a user", description = "Deletes a user by ID. Requires ADMIN role.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "User deleted successfully", content = @Content),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
        })
        public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
                log.info("DELETE /api/users/{} - Deleting user", id);

                deleteUserUseCase.execute(id);

                log.info("User {} deleted successfully", id);
                return ResponseEntity.noContent().build();
        }

        /**
         * Check if the current authenticated user can access the specified user's data.
         * Access is granted if:
         * - The user is an ADMIN
         * - The user is accessing their own data
         */
        private void checkUserAccess(String targetUserEmail) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                if (auth == null || !auth.isAuthenticated()) {
                        throw new AccessDeniedException("User not authenticated");
                }

                String currentUserEmail = auth.getName();
                boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

                // Allow if admin or accessing own profile
                if (!isAdmin && !currentUserEmail.equals(targetUserEmail)) {
                        log.warn("User {} attempted to access user {} without permission",
                                        currentUserEmail, targetUserEmail);
                        throw new AccessDeniedException("You don't have permission to access this user's data");
                }
        }
}

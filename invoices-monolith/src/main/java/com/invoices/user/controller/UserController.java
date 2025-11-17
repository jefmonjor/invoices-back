package com.invoices.user_service.controller;

import com.invoices.user_service.dto.CreateUserRequest;
import com.invoices.user_service.dto.UpdateUserRequest;
import com.invoices.user_service.dto.UserDTO;
import com.invoices.user_service.service.UserService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for user management operations.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Slf4j
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Users", description = "Endpoints for user management")
public class UserController {

    private final UserService userService;

    /**
     * Retrieves all users.
     * Only accessible by ADMIN role.
     *
     * @return list of all users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all users",
            description = "Retrieves a list of all users. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions",
                    content = @Content
            )
    })
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.info("GET /api/users - Fetching all users");

        List<UserDTO> users = userService.getAllUsers();

        log.info("Retrieved {} users", users.size());
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieves a user by ID.
     * Accessible by ADMIN or the user themselves.
     *
     * @param id the user ID
     * @return the user DTO
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a user by their ID. Users can only view their own profile unless they have ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Cannot access other users' profiles",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        log.info("GET /api/users/{} - Fetching user", id);

        // Get the user first
        UserDTO user = userService.getUserById(id);

        // Check authorization: must be ADMIN or the user themselves
        checkUserAccess(user.getEmail());

        log.info("Retrieved user with id: {}", id);
        return ResponseEntity.ok(user);
    }

    /**
     * Creates a new user.
     * Only accessible by ADMIN role.
     *
     * @param request the create user request
     * @return the created user DTO
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new user",
            description = "Creates a new user. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User already exists",
                    content = @Content
            )
    })
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/users - Creating new user: {}", request.getEmail());

        UserDTO user = userService.createUser(request);

        log.info("User created with id: {}", user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Updates an existing user.
     * Accessible by ADMIN or the user themselves.
     * Only ADMIN can modify roles.
     *
     * @param id the user ID to update
     * @param request the update request
     * @return the updated user DTO
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update a user",
            description = "Updates a user. Users can update their own profile, but only ADMIN can modify roles."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Cannot update other users or modify roles without ADMIN",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        log.info("PUT /api/users/{} - Updating user", id);

        // Get the user first to check authorization
        UserDTO existingUser = userService.getUserById(id);
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

        UserDTO updatedUser = userService.updateUser(id, request);

        log.info("User updated with id: {}", id);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Deletes a user by ID.
     * Only accessible by ADMIN role.
     * Performs soft delete (disables the user).
     *
     * @param id the user ID to delete
     * @return no content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete a user",
            description = "Soft deletes a user by disabling their account. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "User deleted successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/users/{} - Deleting user", id);

        userService.deleteUser(id);

        log.info("User deleted with id: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Checks if the current authenticated user can access the specified user's data.
     * Access is granted if:
     * - The user is an ADMIN
     * - The user is accessing their own data
     *
     * @param targetUserEmail the email of the user being accessed
     * @throws AccessDeniedException if access is denied
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

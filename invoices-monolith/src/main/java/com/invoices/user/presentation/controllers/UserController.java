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
        private final com.invoices.company.application.services.CompanyManagementService companyManagementService;

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

        /**
         * Alias endpoint for frontend compatibility.
         * GET /api/users/me - same as GET /api/users/profile
         */
        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get current user profile (alias)", description = "Alias for GET /api/users/profile. Retrieves the profile of the currently authenticated user.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
        })
        public ResponseEntity<UserDTO> getMe() {
                return getCurrentUserProfile();
        }

        /**
         * Alias endpoint for frontend compatibility.
         * PUT /api/users/me - same as PUT /api/users/profile
         */
        @PutMapping("/me")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Update current user profile (alias)", description = "Alias for PUT /api/users/profile. Updates the profile of the currently authenticated user.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
        })
        public ResponseEntity<UserDTO> updateMe(@Valid @RequestBody UpdateUserRequest request) {
                return updateCurrentUserProfile(request);
        }

        @GetMapping
        @Operation(summary = "Get all users", description = "Retrieves a paginated list of all users. Available to all authenticated users.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(schema = @Schema(implementation = org.springframework.data.domain.Page.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
        })
        public ResponseEntity<org.springframework.data.domain.Page<UserDTO>> getAllUsers(
                        @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
                log.info("GET /api/users - Fetching all users (page: {}, size: {})", pageable.getPageNumber(),
                                pageable.getPageSize());

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean isPlatformAdmin = auth.getAuthorities()
                                .contains(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
                boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

                // Regular users cannot list users
                if (!isPlatformAdmin && !isAdmin) {
                        throw new AccessDeniedException("Regular users cannot list users");
                }

                org.springframework.data.domain.Page<User> usersPage;

                if (isPlatformAdmin) {
                        // Platform Admin sees ALL users
                        usersPage = getAllUsersUseCase.execute(pageable, null);
                } else {
                        // Company Admin sees only users in their company
                        Long currentCompanyId = com.invoices.security.context.CompanyContext.getCompanyId();
                        usersPage = getAllUsersUseCase.execute(pageable, currentCompanyId);
                }

                org.springframework.data.domain.Page<UserDTO> userDTOsPage = usersPage.map(mapper::toDTO);

                log.info("Retrieved {} users", userDTOsPage.getTotalElements());
                return ResponseEntity.ok(userDTOsPage);
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

                boolean isPlatformAdmin = auth.getAuthorities()
                                .contains(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));

                // If not admin (company or platform) and trying to modify roles, deny
                if (!isAdmin && !isPlatformAdmin && request.getRoles() != null) {
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
         * Get all companies the current user belongs to
         */
        @GetMapping("/me/companies")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get current user's companies", description = "Retrieves all companies the authenticated user belongs to with their roles")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Companies retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
        })
        public ResponseEntity<List<com.invoices.company.presentation.dto.CompanyDto>> getCurrentUserCompanies() {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                log.info("GET /api/users/me/companies - Fetching companies for user: {}", email);

                var companies = companyManagementService.getUserCompanies(email);

                log.info("Retrieved {} companies for user: {}", companies.size(), email);
                return ResponseEntity.ok(companies);
        }

        /**
         * Set default company for the current user
         */
        @PutMapping("/me/companies/{companyId}/set-default")
        @PreAuthorize(" isAuthenticated()")
        @Operation(summary = "Set default company", description = "Sets the specified company as the current user's default/active company")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Default company set successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                        @ApiResponse(responseCode = "403", description = "User does not belong to this company", content = @Content)
        })
        public ResponseEntity<Void> setDefaultCompany(@PathVariable Long companyId) {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                log.info("PUT /api/users/me/companies/{}/set-default - Setting default company for user: {}", companyId,
                                email);

                companyManagementService.switchCompany(email, companyId);

                log.info("Default company set to {} for user: {}", companyId, email);
                return ResponseEntity.ok().build();
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
                boolean isPlatformAdmin = auth.getAuthorities()
                                .contains(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));

                if (!isAdmin && !isPlatformAdmin && !currentUserEmail.equals(targetUserEmail)) {
                        log.warn("User {} attempted to access user {} without permission",
                                        currentUserEmail, targetUserEmail);
                        throw new AccessDeniedException("You don't have permission to access this user's data");
                }
        }
}

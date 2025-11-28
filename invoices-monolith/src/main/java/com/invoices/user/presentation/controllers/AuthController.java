package com.invoices.user.presentation.controllers;

import com.invoices.auth.application.services.PasswordResetService;
import com.invoices.company.application.services.CompanyInvitationService;
import com.invoices.company.application.services.CompanyManagementService;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.security.JwtUtil;
import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.usecases.AuthenticateUserUseCase;
import com.invoices.user.domain.usecases.CreateUserUseCase;
import com.invoices.user.domain.usecases.UpdateUserLastLoginUseCase;
import com.invoices.user.presentation.dto.AuthResponse;
import com.invoices.user.presentation.dto.CreateUserRequest;
import com.invoices.user.presentation.dto.ForgotPasswordRequest;
import com.invoices.user.presentation.dto.LoginRequest;
import com.invoices.user.presentation.dto.ResetPasswordRequest;
import com.invoices.user.presentation.mappers.UserDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication operations (Clean Architecture).
 * Handles user registration and login using domain Use Cases.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user authentication and registration")
public class AuthController {

        private final CreateUserUseCase createUserUseCase;
        private final AuthenticateUserUseCase authenticateUserUseCase;
        private final UpdateUserLastLoginUseCase updateUserLastLoginUseCase;
        private final CompanyManagementService companyManagementService;
        private final CompanyInvitationService companyInvitationService;
        private final PasswordResetService passwordResetService;
        private final JwtUtil jwtUtil;
        private final UserDtoMapper userDtoMapper;

        /**
         * Registers a new user and returns authentication token.
         *
         * @param request the registration request
         * @return authentication response with JWT token
         */
        @PostMapping("/register")
        @Operation(summary = "Register a new user", description = "Creates a new user account and returns an authentication token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
                        @ApiResponse(responseCode = "409", description = "User already exists with this email", content = @Content)
        })
        public ResponseEntity<AuthResponse> register(@Valid @RequestBody CreateUserRequest request) {
                log.info("POST /api/auth/register - Registering new user: {}", request.getEmail());

                // Default role for new users
                java.util.Set<String> roles = new java.util.HashSet<>();
                roles.add("ROLE_USER");

                // Create user using domain use case
                User createdUser = createUserUseCase.execute(
                                request.getEmail(),
                                request.getPassword(),
                                request.getFirstName(),
                                request.getLastName(),
                                roles);

                // Handle Company Registration
                if ("NEW_COMPANY".equals(request.getRegistrationType())) {
                        Company newCompany = new Company(
                                        null,
                                        request.getCompanyName(),
                                        request.getTaxId(),
                                        request.getCompanyAddress(),
                                        null, // city
                                        null, // postalCode
                                        null, // province
                                        request.getCompanyPhone(),
                                        request.getCompanyEmail(),
                                        null // iban
                        );
                        companyManagementService.createCompany(newCompany, createdUser.getId());
                } else if ("JOIN_COMPANY".equals(request.getRegistrationType())) {
                        String tokenOrCode = request.getInvitationToken();
                        if (tokenOrCode == null || tokenOrCode.isEmpty()) {
                                tokenOrCode = request.getInvitationCode();
                        }
                        companyInvitationService.acceptInvitation(tokenOrCode, createdUser.getId());
                }

                // Generate JWT token with email, roles and companyId
                String token = jwtUtil.generateToken(createdUser.getEmail(), createdUser.getRoles(),
                                createdUser.getCurrentCompanyId());

                AuthResponse response = AuthResponse.builder()
                                .token(token)
                                .expiresIn(jwtUtil.getExpirationTime())
                                .user(userDtoMapper.toDTO(createdUser))
                                .build();

                log.info("User registered successfully: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Authenticates a user and returns JWT token.
         *
         * @param request the login request
         * @return authentication response with JWT token
         */
        @PostMapping("/login")
        @Operation(summary = "Login", description = "Authenticates a user and returns a JWT token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
        })
        public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
                log.info("POST /api/auth/login - Login attempt for user: {}", request.getEmail());

                // Authenticate user using domain use case
                User authenticatedUser = authenticateUserUseCase.execute(
                                request.getEmail(),
                                request.getPassword());

                // Update last login timestamp
                updateUserLastLoginUseCase.execute(authenticatedUser.getId());

                // Generate JWT token with email, roles and companyId
                String token = jwtUtil.generateToken(authenticatedUser.getEmail(), authenticatedUser.getRoles(),
                                authenticatedUser.getCurrentCompanyId());

                AuthResponse response = AuthResponse.builder()
                                .token(token)
                                .expiresIn(jwtUtil.getExpirationTime())
                                .user(userDtoMapper.toDTO(authenticatedUser))
                                .build();

                log.info("User logged in successfully: {}", request.getEmail());
                return ResponseEntity.ok(response);
        }

        /**
         * Initiates the password reset process.
         *
         * @param request the forgot password request containing email
         * @return response entity
         */
        @PostMapping("/forgot-password")
        @Operation(summary = "Request password reset", description = "Sends a password reset email if the account exists")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Email sent (or simulated if user not found)", content = @Content),
                        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
        })
        public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
                log.info("POST /api/auth/forgot-password - Request for: {}", request.getEmail());
                passwordResetService.initiatePasswordReset(request.getEmail());
                // Always return 200 OK to prevent email enumeration
                return ResponseEntity.ok().build();
        }

        /**
         * Resets the password using a token.
         *
         * @param request the reset password request containing token and new password
         * @return response entity
         */
        @PostMapping("/reset-password")
        @Operation(summary = "Reset password", description = "Resets the password using a valid token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Password reset successfully", content = @Content),
                        @ApiResponse(responseCode = "400", description = "Invalid token or password", content = @Content)
        })
        public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
                log.info("POST /api/auth/reset-password - Reset attempt with token");
                passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
                return ResponseEntity.ok().build();
        }

        /**
         * Verifies if a reset token is valid.
         *
         * @param token the token to verify
         * @return response entity with validity status
         */
        @GetMapping("/verify-reset-token/{token}")
        @Operation(summary = "Verify reset token", description = "Checks if a password reset token is valid")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Token verification result", content = @Content)
        })
        public ResponseEntity<java.util.Map<String, Boolean>> verifyResetToken(@PathVariable java.util.UUID token) {
                log.info("GET /api/auth/verify-reset-token/{}", token);
                boolean isValid = passwordResetService.isTokenValid(token);
                return ResponseEntity.ok(java.util.Map.of("valid", isValid));
        }
}

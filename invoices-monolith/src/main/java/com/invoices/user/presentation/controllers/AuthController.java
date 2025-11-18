package com.invoices.user.presentation.controllers;

import com.invoices.security.JwtUtil;
import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.usecases.AuthenticateUserUseCase;
import com.invoices.user.domain.usecases.CreateUserUseCase;
import com.invoices.user.domain.usecases.UpdateUserLastLoginUseCase;
import com.invoices.user.presentation.dto.AuthResponse;
import com.invoices.user.presentation.dto.CreateUserRequest;
import com.invoices.user.presentation.dto.LoginRequest;
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
    private final JwtUtil jwtUtil;

    /**
     * Registers a new user and returns authentication token.
     *
     * @param request the registration request
     * @return authentication response with JWT token
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and returns an authentication token"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User already exists with this email",
                    content = @Content
            )
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
                roles
        );

        // Generate JWT token
        String token = jwtUtil.generateToken(createdUser.getEmail());

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .email(createdUser.getEmail())
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
    @Operation(
            summary = "Login",
            description = "Authenticates a user and returns a JWT token"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content
            )
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - Login attempt for user: {}", request.getEmail());

        // Authenticate user using domain use case
        User authenticatedUser = authenticateUserUseCase.execute(
                request.getEmail(),
                request.getPassword()
        );

        // Update last login timestamp
        updateUserLastLoginUseCase.execute(authenticatedUser.getId());

        // Generate JWT token
        String token = jwtUtil.generateToken(authenticatedUser.getEmail());

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .email(authenticatedUser.getEmail())
                .build();

        log.info("User logged in successfully: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }
}

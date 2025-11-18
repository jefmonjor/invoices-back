package com.invoices.user.service;

import com.invoices.user.dto.AuthResponse;
import com.invoices.user.dto.CreateUserRequest;
import com.invoices.user.dto.LoginRequest;
import com.invoices.user.dto.UserDTO;
import com.invoices.user.entity.User;
import com.invoices.user.exception.InvalidCredentialsException;
import com.invoices.user.repository.UserRepository;
import com.invoices.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for handling authentication operations.
 * Manages login and registration with JWT token generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * Authenticates a user and generates a JWT token.
     *
     * @param request the login request with email and password
     * @return authentication response with JWT token and user info
     * @throws InvalidCredentialsException if credentials are invalid
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());

        try {
            // Authenticate user with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Get authenticated user details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Generate JWT token
            String token = jwtUtil.generateToken(userDetails);

            // Get user entity to update last login
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Update last login timestamp
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Get user DTO
            UserDTO userDTO = userService.getUserByEmail(request.getEmail());

            log.info("User {} logged in successfully", request.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .expiresIn(jwtExpiration)
                    .user(userDTO)
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {} - Invalid credentials", request.getEmail());
            throw new InvalidCredentialsException();
        } catch (Exception e) {
            log.error("Error during login for user: {}", request.getEmail(), e);
            throw new InvalidCredentialsException("Authentication failed", e);
        }
    }

    /**
     * Registers a new user and automatically logs them in.
     *
     * @param request the registration request
     * @return authentication response with JWT token and user info
     */
    @Transactional
    public AuthResponse register(CreateUserRequest request) {
        log.info("Registration attempt for user: {}", request.getEmail());

        // Create the user (this will hash the password and validate email uniqueness)
        UserDTO userDTO = userService.createUser(request);

        log.info("User {} registered successfully, proceeding to auto-login", request.getEmail());

        // Auto-login: create login request and authenticate
        LoginRequest loginRequest = LoginRequest.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        return login(loginRequest);
    }
}

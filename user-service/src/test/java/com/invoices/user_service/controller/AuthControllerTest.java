package com.invoices.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoices.user_service.dto.AuthResponse;
import com.invoices.user_service.dto.CreateUserRequest;
import com.invoices.user_service.dto.LoginRequest;
import com.invoices.user_service.dto.UserDTO;
import com.invoices.user_service.exception.InvalidCredentialsException;
import com.invoices.user_service.exception.UserAlreadyExistsException;
import com.invoices.user_service.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests authentication endpoints with MockMvc.
 *
 * Uses @WebMvcTest to load only web layer.
 * Mocks AuthService dependency.
 * Tests login, registration, validation, and error handling.
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private LoginRequest loginRequest;
    private CreateUserRequest registerRequest;
    private AuthResponse authResponse;
    private UserDTO userDTO;

    private static final Long JWT_EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        registerRequest = CreateUserRequest.builder()
                .email("newuser@example.com")
                .password("Password123!")
                .firstName("Jane")
                .lastName("Smith")
                .roles(Set.of("ROLE_USER"))
                .build();

        userDTO = UserDTO.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .roles(Set.of("ROLE_USER"))
                .enabled(true)
                .build();

        authResponse = AuthResponse.builder()
                .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.test")
                .type("Bearer")
                .expiresIn(JWT_EXPIRATION)
                .user(userDTO)
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/login - Login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        @WithAnonymousUser
        void shouldLoginSuccessfully() throws Exception {
            // Arrange
            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.type", is("Bearer")))
                    .andExpect(jsonPath("$.expiresIn", is(JWT_EXPIRATION.intValue())))
                    .andExpect(jsonPath("$.user", notNullValue()))
                    .andExpect(jsonPath("$.user.email", is(userDTO.getEmail())))
                    .andExpect(jsonPath("$.user.firstName", is(userDTO.getFirstName())))
                    .andExpect(jsonPath("$.user.lastName", is(userDTO.getLastName())));

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 401 for invalid credentials")
        @WithAnonymousUser
        void shouldReturn401ForInvalidCredentials() throws Exception {
            // Arrange
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new InvalidCredentialsException());

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for missing email")
        @WithAnonymousUser
        void shouldReturn400ForMissingEmail() throws Exception {
            // Arrange
            LoginRequest invalidRequest = LoginRequest.builder()
                    .email(null)
                    .password("Password123!")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for missing password")
        @WithAnonymousUser
        void shouldReturn400ForMissingPassword() throws Exception {
            // Arrange
            LoginRequest invalidRequest = LoginRequest.builder()
                    .email("test@example.com")
                    .password(null)
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid email format")
        @WithAnonymousUser
        void shouldReturn400ForInvalidEmailFormat() throws Exception {
            // Arrange
            LoginRequest invalidRequest = LoginRequest.builder()
                    .email("invalid-email")
                    .password("Password123!")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for empty request body")
        @WithAnonymousUser
        void shouldReturn400ForEmptyRequestBody() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should handle case-sensitive email")
        @WithAnonymousUser
        void shouldHandleCaseSensitiveEmail() throws Exception {
            // Arrange
            LoginRequest upperCaseRequest = LoginRequest.builder()
                    .email("TEST@EXAMPLE.COM")
                    .password("Password123!")
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(upperCaseRequest)))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(authService).login(any(LoginRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register - Registration")
    class RegistrationTests {

        @Test
        @DisplayName("Should register user successfully")
        @WithAnonymousUser
        void shouldRegisterSuccessfully() throws Exception {
            // Arrange
            UserDTO newUserDTO = UserDTO.builder()
                    .id(2L)
                    .email(registerRequest.getEmail())
                    .firstName(registerRequest.getFirstName())
                    .lastName(registerRequest.getLastName())
                    .roles(registerRequest.getRoles())
                    .enabled(true)
                    .build();

            AuthResponse registerResponse = AuthResponse.builder()
                    .token("new-user-jwt-token")
                    .type("Bearer")
                    .expiresIn(JWT_EXPIRATION)
                    .user(newUserDTO)
                    .build();

            when(authService.register(any(CreateUserRequest.class))).thenReturn(registerResponse);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.type", is("Bearer")))
                    .andExpect(jsonPath("$.user", notNullValue()))
                    .andExpect(jsonPath("$.user.email", is(registerRequest.getEmail())))
                    .andExpect(jsonPath("$.user.firstName", is(registerRequest.getFirstName())))
                    .andExpect(jsonPath("$.user.lastName", is(registerRequest.getLastName())));

            verify(authService).register(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 409 when user already exists")
        @WithAnonymousUser
        void shouldReturn409WhenUserAlreadyExists() throws Exception {
            // Arrange
            when(authService.register(any(CreateUserRequest.class)))
                    .thenThrow(new UserAlreadyExistsException(registerRequest.getEmail()));

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict());

            verify(authService).register(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        @WithAnonymousUser
        void shouldReturn400ForMissingRequiredFields() throws Exception {
            // Arrange - missing password and firstName
            CreateUserRequest invalidRequest = CreateUserRequest.builder()
                    .email("incomplete@example.com")
                    .lastName("Smith")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid email format")
        @WithAnonymousUser
        void shouldReturn400ForInvalidEmailFormat() throws Exception {
            // Arrange
            CreateUserRequest invalidRequest = CreateUserRequest.builder()
                    .email("invalid-email")
                    .password("Password123!")
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for weak password")
        @WithAnonymousUser
        void shouldReturn400ForWeakPassword() throws Exception {
            // Arrange
            CreateUserRequest invalidRequest = CreateUserRequest.builder()
                    .email("newuser@example.com")
                    .password("weak")
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Should accept registration with default roles")
        @WithAnonymousUser
        void shouldAcceptRegistrationWithDefaultRoles() throws Exception {
            // Arrange
            CreateUserRequest requestWithoutRoles = CreateUserRequest.builder()
                    .email("noroles@example.com")
                    .password("Password123!")
                    .firstName("No")
                    .lastName("Roles")
                    .build();

            UserDTO newUserDTO = UserDTO.builder()
                    .id(3L)
                    .email(requestWithoutRoles.getEmail())
                    .firstName(requestWithoutRoles.getFirstName())
                    .lastName(requestWithoutRoles.getLastName())
                    .roles(Set.of("ROLE_USER"))
                    .enabled(true)
                    .build();

            AuthResponse response = AuthResponse.builder()
                    .token("token")
                    .type("Bearer")
                    .expiresIn(JWT_EXPIRATION)
                    .user(newUserDTO)
                    .build();

            when(authService.register(any(CreateUserRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithoutRoles)))
                    .andDo(print())
                    .andExpect(status().isCreated());

            verify(authService).register(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for empty request body")
        @WithAnonymousUser
        void shouldReturn400ForEmptyRequestBody() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any(CreateUserRequest.class));
        }
    }
}

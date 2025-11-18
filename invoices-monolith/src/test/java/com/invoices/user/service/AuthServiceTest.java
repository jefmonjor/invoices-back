package com.invoices.user.service;

import com.invoices.user.dto.AuthResponse;
import com.invoices.user.dto.CreateUserRequest;
import com.invoices.user.dto.LoginRequest;
import com.invoices.user.dto.UserDTO;
import com.invoices.user.entity.User;
import com.invoices.user.exception.InvalidCredentialsException;
import com.invoices.user.repository.UserRepository;
import com.invoices.user.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests authentication, registration, and JWT token generation.
 *
 * Uses Mockito to mock dependencies.
 * Follows AAA pattern: Arrange-Act-Assert.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private CreateUserRequest createUserRequest;
    private UserDTO userDTO;
    private UserDetails userDetails;
    private static final Long JWT_EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        // Set JWT expiration via reflection
        ReflectionTestUtils.setField(authService, "jwtExpiration", JWT_EXPIRATION);

        // Common test data
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("hashedPassword123")
                .firstName("John")
                .lastName("Doe")
                .roles(Set.of("ROLE_USER"))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(LocalDateTime.now())
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        createUserRequest = CreateUserRequest.builder()
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

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("hashedPassword123")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully and return JWT token")
        void shouldLoginSuccessfully() {
            // Arrange
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token-123");
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userService.getUserByEmail(loginRequest.getEmail())).thenReturn(userDTO);

            // Act
            AuthResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token-123");
            assertThat(response.getType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(JWT_EXPIRATION);
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo(userDTO.getEmail());

            // Verify authentication was attempted
            ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(authCaptor.capture());
            assertThat(authCaptor.getValue().getPrincipal()).isEqualTo(loginRequest.getEmail());
            assertThat(authCaptor.getValue().getCredentials()).isEqualTo(loginRequest.getPassword());

            // Verify last login was updated
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getLastLogin()).isNotNull();

            verify(jwtUtil).generateToken(userDetails);
            verify(userService).getUserByEmail(loginRequest.getEmail());
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException for wrong password")
        void shouldThrowExceptionForWrongPassword() {
            // Arrange
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil, never()).generateToken(any(UserDetails.class));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            // Arrange
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("User not found"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil, never()).generateToken(any(UserDetails.class));
        }

        @Test
        @DisplayName("Should update last login timestamp on successful login")
        void shouldUpdateLastLoginTimestamp() {
            // Arrange
            LocalDateTime beforeLogin = LocalDateTime.now().minusMinutes(5);
            testUser.setLastLogin(beforeLogin);

            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token-123");
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userService.getUserByEmail(loginRequest.getEmail())).thenReturn(userDTO);

            // Act
            authService.login(loginRequest);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getLastLogin()).isAfter(beforeLogin);
        }
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register user and auto-login successfully")
        void shouldRegisterAndAutoLogin() {
            // Arrange
            UserDTO newUserDTO = UserDTO.builder()
                    .id(2L)
                    .email(createUserRequest.getEmail())
                    .firstName(createUserRequest.getFirstName())
                    .lastName(createUserRequest.getLastName())
                    .roles(createUserRequest.getRoles())
                    .enabled(true)
                    .build();

            User newUser = User.builder()
                    .id(2L)
                    .email(createUserRequest.getEmail())
                    .password("hashedPassword")
                    .firstName(createUserRequest.getFirstName())
                    .lastName(createUserRequest.getLastName())
                    .roles(createUserRequest.getRoles())
                    .enabled(true)
                    .build();

            UserDetails newUserDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(createUserRequest.getEmail())
                    .password("hashedPassword")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // Mock user creation
            when(userService.createUser(createUserRequest)).thenReturn(newUserDTO);

            // Mock auto-login process
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(newUserDetails);
            when(jwtUtil.generateToken(newUserDetails)).thenReturn("new-user-jwt-token");
            when(userRepository.findByEmail(createUserRequest.getEmail())).thenReturn(Optional.of(newUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userService.getUserByEmail(createUserRequest.getEmail())).thenReturn(newUserDTO);

            // Act
            AuthResponse response = authService.register(createUserRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("new-user-jwt-token");
            assertThat(response.getType()).isEqualTo("Bearer");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo(createUserRequest.getEmail());

            // Verify user was created
            verify(userService).createUser(createUserRequest);

            // Verify auto-login was performed
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil).generateToken(newUserDetails);
        }

        @Test
        @DisplayName("Should propagate UserAlreadyExistsException from userService")
        void shouldPropagateUserAlreadyExistsException() {
            // Arrange
            when(userService.createUser(createUserRequest))
                    .thenThrow(new com.invoices.user.exception.UserAlreadyExistsException(
                            createUserRequest.getEmail()));

            // Act & Assert
            assertThatThrownBy(() -> authService.register(createUserRequest))
                    .isInstanceOf(com.invoices.user.exception.UserAlreadyExistsException.class);

            verify(userService).createUser(createUserRequest);
            verify(authenticationManager, never()).authenticate(any());
            verify(jwtUtil, never()).generateToken(any(UserDetails.class));
        }

        @Test
        @DisplayName("Should use same password for create and auto-login")
        void shouldUseSamePasswordForCreateAndAutoLogin() {
            // Arrange
            UserDTO newUserDTO = UserDTO.builder()
                    .id(2L)
                    .email(createUserRequest.getEmail())
                    .firstName(createUserRequest.getFirstName())
                    .lastName(createUserRequest.getLastName())
                    .roles(createUserRequest.getRoles())
                    .enabled(true)
                    .build();

            User newUser = User.builder()
                    .id(2L)
                    .email(createUserRequest.getEmail())
                    .password("hashedPassword")
                    .firstName(createUserRequest.getFirstName())
                    .lastName(createUserRequest.getLastName())
                    .roles(createUserRequest.getRoles())
                    .enabled(true)
                    .build();

            UserDetails newUserDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(createUserRequest.getEmail())
                    .password("hashedPassword")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            when(userService.createUser(createUserRequest)).thenReturn(newUserDTO);
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(newUserDetails);
            when(jwtUtil.generateToken(newUserDetails)).thenReturn("token");
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(newUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userService.getUserByEmail(anyString())).thenReturn(newUserDTO);

            // Act
            authService.register(createUserRequest);

            // Assert - verify authentication used the original password
            ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(authCaptor.capture());
            assertThat(authCaptor.getValue().getCredentials()).isEqualTo(createUserRequest.getPassword());
        }
    }
}

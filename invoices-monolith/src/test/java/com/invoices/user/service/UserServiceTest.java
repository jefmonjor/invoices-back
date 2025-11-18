package com.invoices.user.service;

import com.invoices.user.dto.CreateUserRequest;
import com.invoices.user.dto.UpdateUserRequest;
import com.invoices.user.dto.UserDTO;
import com.invoices.user.entity.User;
import com.invoices.user.exception.UserAlreadyExistsException;
import com.invoices.user.exception.UserNotFoundException;
import com.invoices.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
 * Unit tests for UserService.
 * Tests all CRUD operations and business logic.
 *
 * Uses Mockito to mock dependencies (UserRepository, PasswordEncoder).
 * Follows AAA pattern: Arrange-Act-Assert.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        // Common test data setup
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
                .updatedAt(LocalDateTime.now())
                .build();

        createUserRequest = CreateUserRequest.builder()
                .email("newuser@example.com")
                .password("Password123!")
                .firstName("Jane")
                .lastName("Smith")
                .roles(Set.of("ROLE_USER"))
                .build();

        updateUserRequest = UpdateUserRequest.builder()
                .firstName("UpdatedName")
                .lastName("UpdatedLastName")
                .build();
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully with hashed password")
        void shouldCreateUserSuccessfully() {
            // Arrange
            when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(createUserRequest.getPassword())).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            UserDTO result = userService.createUser(createUserRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(createUserRequest.getEmail());
            assertThat(result.getFirstName()).isEqualTo(createUserRequest.getFirstName());
            assertThat(result.getLastName()).isEqualTo(createUserRequest.getLastName());
            assertThat(result.getRoles()).containsExactlyInAnyOrder("ROLE_USER");

            // Verify password was encoded
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("hashedPassword");

            verify(userRepository).existsByEmail(createUserRequest.getEmail());
            verify(passwordEncoder).encode(createUserRequest.getPassword());
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Arrange
            when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.createUser(createUserRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining(createUserRequest.getEmail());

            verify(userRepository).existsByEmail(createUserRequest.getEmail());
            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Should create user with default roles when no roles provided")
        void shouldCreateUserWithDefaultRoles() {
            // Arrange
            CreateUserRequest requestWithoutRoles = CreateUserRequest.builder()
                    .email("noroles@example.com")
                    .password("Password123!")
                    .firstName("No")
                    .lastName("Roles")
                    .build();

            when(userRepository.existsByEmail(requestWithoutRoles.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(3L);
                return user;
            });

            // Act
            UserDTO result = userService.createUser(requestWithoutRoles);

            // Assert
            assertThat(result).isNotNull();
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @DisplayName("Should get user by ID successfully")
        void shouldGetUserByIdSuccessfully() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // Act
            UserDTO result = userService.getUserById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUser.getId());
            assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(result.getFirstName()).isEqualTo(testUser.getFirstName());
            assertThat(result.getLastName()).isEqualTo(testUser.getLastName());

            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user ID not found")
        void shouldThrowExceptionWhenUserIdNotFound() {
            // Arrange
            Long nonExistentId = 999L;
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository).findById(nonExistentId);
        }

        @Test
        @DisplayName("Should get user by email successfully")
        void shouldGetUserByEmailSuccessfully() {
            // Arrange
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

            // Act
            UserDTO result = userService.getUserByEmail(testUser.getEmail());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(result.getId()).isEqualTo(testUser.getId());

            verify(userRepository).findByEmail(testUser.getEmail());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when email not found")
        void shouldThrowExceptionWhenEmailNotFound() {
            // Arrange
            String nonExistentEmail = "notfound@example.com";
            when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserByEmail(nonExistentEmail))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository).findByEmail(nonExistentEmail);
        }

        @Test
        @DisplayName("Should get all users successfully")
        void shouldGetAllUsersSuccessfully() {
            // Arrange
            User user2 = User.builder()
                    .id(2L)
                    .email("user2@example.com")
                    .password("hashedPassword")
                    .firstName("Alice")
                    .lastName("Johnson")
                    .roles(Set.of("ROLE_USER"))
                    .enabled(true)
                    .build();

            when(userRepository.findAll()).thenReturn(List.of(testUser, user2));

            // Act
            List<UserDTO> result = userService.getAllUsers();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(UserDTO::getEmail)
                    .containsExactlyInAnyOrder("test@example.com", "user2@example.com");

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void shouldReturnEmptyListWhenNoUsers() {
            // Arrange
            when(userRepository.findAll()).thenReturn(List.of());

            // Act
            List<UserDTO> result = userService.getAllUsers();

            // Assert
            assertThat(result).isEmpty();
            verify(userRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            UserDTO result = userService.updateUser(1L, updateUserRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo(updateUserRequest.getFirstName());
            assertThat(result.getLastName()).isEqualTo(updateUserRequest.getLastName());

            verify(userRepository).findById(1L);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when updating non-existent user")
        void shouldThrowExceptionWhenUpdatingNonExistentUser() {
            // Arrange
            Long nonExistentId = 999L;
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.updateUser(nonExistentId, updateUserRequest))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository).findById(nonExistentId);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should update only non-null fields")
        void shouldUpdateOnlyNonNullFields() {
            // Arrange
            UpdateUserRequest partialUpdate = UpdateUserRequest.builder()
                    .firstName("OnlyFirstName")
                    .build();

            String originalLastName = testUser.getLastName();
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            UserDTO result = userService.updateUser(1L, partialUpdate);

            // Assert
            assertThat(result.getFirstName()).isEqualTo("OnlyFirstName");
            assertThat(result.getLastName()).isEqualTo(originalLastName); // Should remain unchanged

            verify(userRepository).save(testUser);
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should perform soft delete by disabling user")
        void shouldPerformSoftDelete() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            userService.deleteUser(1L);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEnabled()).isFalse();

            verify(userRepository).findById(1L);
            verify(userRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when deleting non-existent user")
        void shouldThrowExceptionWhenDeletingNonExistentUser() {
            // Arrange
            Long nonExistentId = 999L;
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.deleteUser(nonExistentId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository).findById(nonExistentId);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should permanently delete user")
        void shouldPermanentlyDeleteUser() {
            // Arrange
            when(userRepository.existsById(1L)).thenReturn(true);
            doNothing().when(userRepository).deleteById(1L);

            // Act
            userService.permanentlyDeleteUser(1L);

            // Assert
            verify(userRepository).existsById(1L);
            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when permanently deleting non-existent user")
        void shouldThrowExceptionWhenPermanentlyDeletingNonExistentUser() {
            // Arrange
            Long nonExistentId = 999L;
            when(userRepository.existsById(nonExistentId)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> userService.permanentlyDeleteUser(nonExistentId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository).existsById(nonExistentId);
            verify(userRepository, never()).deleteById(anyLong());
        }
    }
}

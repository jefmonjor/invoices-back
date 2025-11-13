package com.invoices.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoices.user_service.dto.CreateUserRequest;
import com.invoices.user_service.dto.UpdateUserRequest;
import com.invoices.user_service.dto.UserDTO;
import com.invoices.user_service.exception.UserAlreadyExistsException;
import com.invoices.user_service.exception.UserNotFoundException;
import com.invoices.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController.
 * Tests REST endpoints with MockMvc.
 *
 * Uses @WebMvcTest to load only web layer.
 * Mocks UserService dependency.
 * Tests authentication, authorization, validation, and error handling.
 */
@WebMvcTest(UserController.class)
@DisplayName("UserController Integration Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserDTO testUserDTO;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        testUserDTO = UserDTO.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .roles(Set.of("ROLE_USER"))
                .enabled(true)
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
    @DisplayName("GET /api/users - Get All Users")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return all users when user is ADMIN")
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        void shouldReturnAllUsersForAdmin() throws Exception {
            // Arrange
            UserDTO user2 = UserDTO.builder()
                    .id(2L)
                    .email("user2@example.com")
                    .firstName("Alice")
                    .lastName("Johnson")
                    .roles(Set.of("ROLE_USER"))
                    .enabled(true)
                    .build();

            when(userService.getAllUsers()).thenReturn(List.of(testUserDTO, user2));

            // Act & Assert
            mockMvc.perform(get("/api/users"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].email", is("test@example.com")))
                    .andExpect(jsonPath("$[1].email", is("user2@example.com")));

            verify(userService).getAllUsers();
        }

        @Test
        @DisplayName("Should return 403 when non-admin user tries to get all users")
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        void shouldReturn403ForNonAdmin() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users"))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(userService, never()).getAllUsers();
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated user tries to get all users")
        void shouldReturn401ForUnauthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());

            verify(userService, never()).getAllUsers();
        }

        @Test
        @DisplayName("Should return empty array when no users exist")
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        void shouldReturnEmptyArrayWhenNoUsers() throws Exception {
            // Arrange
            when(userService.getAllUsers()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/users"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(userService).getAllUsers();
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id} - Get User By ID")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when ADMIN requests any user")
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        void shouldReturnUserForAdmin() throws Exception {
            // Arrange
            when(userService.getUserById(1L)).thenReturn(testUserDTO);

            // Act & Assert
            mockMvc.perform(get("/api/users/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.email", is("test@example.com")))
                    .andExpect(jsonPath("$.firstName", is("John")))
                    .andExpect(jsonPath("$.lastName", is("Doe")));

            verify(userService).getUserById(1L);
        }

        @Test
        @DisplayName("Should return user when user requests their own data")
        @WithMockUser(username = "test@example.com", roles = {"USER"})
        void shouldReturnUserWhenRequestingOwnData() throws Exception {
            // Arrange
            when(userService.getUserById(1L)).thenReturn(testUserDTO);

            // Act & Assert
            mockMvc.perform(get("/api/users/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.email", is("test@example.com")));

            verify(userService).getUserById(1L);
        }

        @Test
        @DisplayName("Should return 403 when user tries to access another user's data")
        @WithMockUser(username = "other@example.com", roles = {"USER"})
        void shouldReturn403WhenAccessingOtherUserData() throws Exception {
            // Arrange
            when(userService.getUserById(1L)).thenReturn(testUserDTO);

            // Act & Assert
            mockMvc.perform(get("/api/users/1"))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(userService).getUserById(1L);
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Arrange
            when(userService.getUserById(999L)).thenThrow(new UserNotFoundException(999L));

            // Act & Assert
            mockMvc.perform(get("/api/users/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(userService).getUserById(999L);
        }
    }

    @Nested
    @DisplayName("POST /api/users - Create User")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully when ADMIN")
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        void shouldCreateUserSuccessfully() throws Exception {
            // Arrange
            UserDTO createdUser = UserDTO.builder()
                    .id(2L)
                    .email(createUserRequest.getEmail())
                    .firstName(createUserRequest.getFirstName())
                    .lastName(createUserRequest.getLastName())
                    .roles(createUserRequest.getRoles())
                    .enabled(true)
                    .build();

            when(userService.createUser(any(CreateUserRequest.class))).thenReturn(createdUser);

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createUserRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.email", is(createUserRequest.getEmail())))
                    .andExpect(jsonPath("$.firstName", is(createUserRequest.getFirstName())));

            verify(userService).createUser(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 403 when non-admin tries to create user")
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        void shouldReturn403ForNonAdmin() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createUserRequest)))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(userService, never()).createUser(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        void shouldReturn400WhenValidationFails() throws Exception {
            // Arrange - invalid email
            CreateUserRequest invalidRequest = CreateUserRequest.builder()
                    .email("invalid-email")
                    .password("short")
                    .firstName("")
                    .lastName("")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(userService, never()).createUser(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 409 when user already exists")
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        void shouldReturn409WhenUserAlreadyExists() throws Exception {
            // Arrange
            when(userService.createUser(any(CreateUserRequest.class)))
                    .thenThrow(new UserAlreadyExistsException(createUserRequest.getEmail()));

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createUserRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict());

            verify(userService).createUser(any(CreateUserRequest.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{id} - Update User")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user when ADMIN")
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        void shouldUpdateUserAsAdmin() throws Exception {
            // Arrange
            UserDTO updatedUser = UserDTO.builder()
                    .id(1L)
                    .email(testUserDTO.getEmail())
                    .firstName(updateUserRequest.getFirstName())
                    .lastName(updateUserRequest.getLastName())
                    .roles(testUserDTO.getRoles())
                    .enabled(true)
                    .build();

            when(userService.getUserById(1L)).thenReturn(testUserDTO);
            when(userService.updateUser(eq(1L), any(UpdateUserRequest.class))).thenReturn(updatedUser);

            // Act & Assert
            mockMvc.perform(put("/api/users/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.firstName", is(updateUserRequest.getFirstName())))
                    .andExpect(jsonPath("$.lastName", is(updateUserRequest.getLastName())));

            verify(userService).updateUser(eq(1L), any(UpdateUserRequest.class));
        }

        @Test
        @DisplayName("Should update own user data")
        @WithMockUser(username = "test@example.com", roles = {"USER"})
        void shouldUpdateOwnUserData() throws Exception {
            // Arrange
            UserDTO updatedUser = UserDTO.builder()
                    .id(1L)
                    .email(testUserDTO.getEmail())
                    .firstName(updateUserRequest.getFirstName())
                    .lastName(updateUserRequest.getLastName())
                    .roles(testUserDTO.getRoles())
                    .enabled(true)
                    .build();

            when(userService.getUserById(1L)).thenReturn(testUserDTO);
            when(userService.updateUser(eq(1L), any(UpdateUserRequest.class))).thenReturn(updatedUser);

            // Act & Assert
            mockMvc.perform(put("/api/users/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserRequest)))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(userService).updateUser(eq(1L), any(UpdateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 403 when non-admin tries to modify roles")
        @WithMockUser(username = "test@example.com", roles = {"USER"})
        void shouldReturn403WhenNonAdminTriesToModifyRoles() throws Exception {
            // Arrange
            UpdateUserRequest requestWithRoles = UpdateUserRequest.builder()
                    .firstName("UpdatedName")
                    .roles(Set.of("ROLE_ADMIN"))
                    .build();

            when(userService.getUserById(1L)).thenReturn(testUserDTO);

            // Act & Assert
            mockMvc.perform(put("/api/users/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithRoles)))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(userService, never()).updateUser(any(), any());
        }

        @Test
        @DisplayName("Should return 403 when user tries to update another user")
        @WithMockUser(username = "other@example.com", roles = {"USER"})
        void shouldReturn403WhenUpdatingOtherUser() throws Exception {
            // Arrange
            when(userService.getUserById(1L)).thenReturn(testUserDTO);

            // Act & Assert
            mockMvc.perform(put("/api/users/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserRequest)))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(userService, never()).updateUser(any(), any());
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent user")
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
            // Arrange
            when(userService.getUserById(999L)).thenThrow(new UserNotFoundException(999L));

            // Act & Assert
            mockMvc.perform(put("/api/users/999")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserRequest)))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(userService, never()).updateUser(any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/{id} - Delete User")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user when ADMIN")
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        void shouldDeleteUserAsAdmin() throws Exception {
            // Arrange
            doNothing().when(userService).deleteUser(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/users/1")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            verify(userService).deleteUser(1L);
        }

        @Test
        @DisplayName("Should return 403 when non-admin tries to delete user")
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        void shouldReturn403ForNonAdmin() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/users/1")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(userService, never()).deleteUser(any());
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent user")
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
            // Arrange
            doThrow(new UserNotFoundException(999L)).when(userService).deleteUser(999L);

            // Act & Assert
            mockMvc.perform(delete("/api/users/999")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(userService).deleteUser(999L);
        }
    }
}

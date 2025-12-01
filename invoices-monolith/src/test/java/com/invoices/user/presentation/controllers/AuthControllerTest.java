package com.invoices.user.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoices.auth.application.services.PasswordResetService;
import com.invoices.company.application.services.CompanyInvitationService;
import com.invoices.company.application.services.CompanyManagementService;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.security.JwtUtil;
import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.usecases.AuthenticateUserUseCase;
import com.invoices.user.domain.usecases.CreateUserUseCase;
import com.invoices.user.domain.usecases.UpdateUserLastLoginUseCase;
import com.invoices.user.presentation.dto.CreateUserRequest;
import com.invoices.user.presentation.dto.LoginRequest;
import com.invoices.user.presentation.dto.UserDTO;
import com.invoices.user.presentation.mappers.UserDtoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(controllers = AuthController.class, properties = {
                "cors.allowed-origins=http://localhost:3000",
                "cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS",
                "cors.allowed-headers=*",
                "cors.allow-credentials=true"
})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private CreateUserUseCase createUserUseCase;

        @MockBean
        private AuthenticateUserUseCase authenticateUserUseCase;

        @MockBean
        private UpdateUserLastLoginUseCase updateUserLastLoginUseCase;

        @MockBean
        private CompanyManagementService companyManagementService;

        @MockBean
        private CompanyInvitationService companyInvitationService;

        @MockBean
        private PasswordResetService passwordResetService;

        @MockBean
        private JwtUtil jwtUtil;

        @MockBean
        private UserDtoMapper userDtoMapper;

        @Test
        void register_ShouldRegisterUserAndCreateCompany_WhenRegistrationTypeIsNewCompany() throws Exception {
                // Arrange
                CreateUserRequest request = new CreateUserRequest();
                request.setEmail("test@example.com");
                request.setPassword("password123");
                request.setFirstName("Test");
                request.setLastName("User");
                request.setRegistrationType("NEW_COMPANY");
                request.setCompanyName("Test Company");
                request.setTaxId("12345678Z");
                request.setCompanyAddress("Test Address");
                request.setCompanyPhone("123456789");
                request.setCompanyEmail("company@example.com");

                User createdUser = new User(1L, "test@example.com", "hashedPassword", "Test", "User",
                                Set.of("ROLE_USER"), true, true, true, true,
                                null, null, null, 1L, null);

                UserDTO userDTO = new UserDTO();
                userDTO.setId(1L);
                userDTO.setEmail("test@example.com");

                when(createUserUseCase.execute(any(), any(), any(), any(), any())).thenReturn(createdUser);
                when(jwtUtil.generateToken(any(), any(), any())).thenReturn("jwt-token");
                when(jwtUtil.getExpirationTime()).thenReturn(3600L);
                when(userDtoMapper.toDTO(any())).thenReturn(userDTO);

                // Act & Assert
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.token").value("jwt-token"))
                                .andExpect(jsonPath("$.user.email").value("test@example.com"));

                verify(createUserUseCase).execute(eq("test@example.com"), eq("password123"), eq("Test"), eq("User"),
                                any());
                verify(companyManagementService).createCompany(any(Company.class), eq(1L));
        }

        @Test
        void register_ShouldRegisterUserAndAcceptInvitation_WhenRegistrationTypeIsJoinCompany() throws Exception {
                // Arrange
                CreateUserRequest request = new CreateUserRequest();
                request.setEmail("test@example.com");
                request.setPassword("password123");
                request.setFirstName("Test");
                request.setLastName("User");
                request.setRegistrationType("JOIN_COMPANY");
                request.setInvitationCode("INV-CODE");

                User createdUser = new User(1L, "test@example.com", "hashedPassword", "Test", "User",
                                Set.of("ROLE_USER"), true, true, true, true,
                                null, null, null, 1L, null);

                UserDTO userDTO = new UserDTO();
                userDTO.setId(1L);
                userDTO.setEmail("test@example.com");

                when(createUserUseCase.execute(any(), any(), any(), any(), any())).thenReturn(createdUser);
                when(jwtUtil.generateToken(any(), any(), any())).thenReturn("jwt-token");
                when(jwtUtil.getExpirationTime()).thenReturn(3600L);
                when(userDtoMapper.toDTO(any())).thenReturn(userDTO);

                // Act & Assert
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.token").value("jwt-token"));

                verify(companyInvitationService).acceptInvitation(eq("INV-CODE"), eq(1L));
        }

        @Test
        void login_ShouldReturnTokenAndUser_WhenCredentialsAreValid() throws Exception {
                // Arrange
                LoginRequest request = new LoginRequest();
                request.setEmail("test@example.com");
                request.setPassword("password123");

                User authenticatedUser = new User(1L, "test@example.com", "hashedPassword", "Test", "User",
                                Set.of("ROLE_USER"), true, true, true, true,
                                null, null, null, 1L, null);

                UserDTO userDTO = new UserDTO();
                userDTO.setId(1L);
                userDTO.setEmail("test@example.com");

                when(authenticateUserUseCase.execute(any(), any())).thenReturn(authenticatedUser);
                when(jwtUtil.generateToken(any(), any(), any())).thenReturn("jwt-token");
                when(jwtUtil.getExpirationTime()).thenReturn(3600L);
                when(userDtoMapper.toDTO(any())).thenReturn(userDTO);

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("jwt-token"));

                verify(authenticateUserUseCase).execute(eq("test@example.com"), eq("password123"));
                verify(updateUserLastLoginUseCase).execute(eq(1L));
        }
}

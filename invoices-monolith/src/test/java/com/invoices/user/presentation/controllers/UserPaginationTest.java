package com.invoices.user.presentation.controllers;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.usecases.*;
import com.invoices.user.presentation.dto.UserDTO;
import com.invoices.user.presentation.mappers.UserDtoMapper;
import com.invoices.company.application.services.CompanyManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateUserUseCase createUserUseCase;
    @MockBean
    private GetAllUsersUseCase getAllUsersUseCase;
    @MockBean
    private GetUserByIdUseCase getUserByIdUseCase;
    @MockBean
    private GetUserByEmailUseCase getUserByEmailUseCase;
    @MockBean
    private UpdateUserUseCase updateUserUseCase;
    @MockBean
    private DeleteUserUseCase deleteUserUseCase;
    @MockBean
    private UserDtoMapper mapper;
    @MockBean
    private CompanyManagementService companyManagementService;
    @MockBean
    private com.invoices.security.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @MockBean
    private com.invoices.security.JwtUtil jwtUtil;

    @Test
    @WithMockUser
    public void testGetAllUsersPagination() throws Exception {
        // Arrange
        User user = new User(
                1L,
                "test@example.com",
                "password",
                "Test",
                "User",
                new HashSet<>(Collections.singletonList("ROLE_USER")),
                true, true, true, true,
                null, null, null, null, null);

        Page<User> userPage = new PageImpl<>(Collections.singletonList(user));

        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setEmail("test@example.com");

        when(getAllUsersUseCase.execute(any(Pageable.class))).thenReturn(userPage);
        when(mapper.toDTO(any(User.class))).thenReturn(userDTO);

        // Act & Assert
        mockMvc.perform(get("/api/users")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }
}

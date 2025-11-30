package com.invoices.user.domain.usecases;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetAllUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private GetAllUsersUseCase getAllUsersUseCase;

    @BeforeEach
    void setUp() {
        getAllUsersUseCase = new GetAllUsersUseCase(userRepository);
    }

    @Test
    void execute_ShouldReturnAllUsers_WhenCompanyIdIsNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        User user = new User("test@example.com", "password", "Test", "User", Collections.emptySet());
        Page<User> expectedPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(expectedPage);

        // Act
        Page<User> result = getAllUsersUseCase.execute(pageable, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).findAllByCompanyId(anyLong(), any());
    }

    @Test
    void execute_ShouldReturnCompanyUsers_WhenCompanyIdIsProvided() {
        // Arrange
        Long companyId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        User user = new User("test@example.com", "password", "Test", "User", Collections.emptySet());
        Page<User> expectedPage = new PageImpl<>(List.of(user));

        when(userRepository.findAllByCompanyId(companyId, pageable)).thenReturn(expectedPage);

        // Act
        Page<User> result = getAllUsersUseCase.execute(pageable, companyId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAllByCompanyId(companyId, pageable);
        verify(userRepository, never()).findAll(pageable);
    }

    @Test
    void execute_LegacyMethod_ShouldCallFindAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> expectedPage = new PageImpl<>(Collections.emptyList());

        when(userRepository.findAll(pageable)).thenReturn(expectedPage);

        // Act
        Page<User> result = getAllUsersUseCase.execute(pageable);

        // Assert
        assertNotNull(result);
        verify(userRepository).findAll(pageable);
    }
}

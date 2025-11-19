package com.invoices.user.infrastructure.config;

import com.invoices.user.domain.ports.PasswordHasher;
import com.invoices.user.domain.ports.UserRepository;
import com.invoices.user.domain.usecases.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for User Use Cases.
 * This configuration wire use cases with their dependencies (ports).
 * Following the Clean Architecture dependency injection pattern.
 */
@Configuration
public class UserUseCaseConfiguration {

    @Bean
    public CreateUserUseCase createUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher) {
        return new CreateUserUseCase(userRepository, passwordHasher);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher) {
        return new AuthenticateUserUseCase(userRepository, passwordHasher);
    }

    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(UserRepository userRepository) {
        return new GetUserByIdUseCase(userRepository);
    }

    @Bean
    public GetAllUsersUseCase getAllUsersUseCase(UserRepository userRepository) {
        return new GetAllUsersUseCase(userRepository);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher) {
        return new UpdateUserUseCase(userRepository, passwordHasher);
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(UserRepository userRepository) {
        return new DeleteUserUseCase(userRepository);
    }

    @Bean
    public UpdateUserLastLoginUseCase updateUserLastLoginUseCase(
            UserRepository userRepository) {
        return new UpdateUserLastLoginUseCase(userRepository);
    }
}

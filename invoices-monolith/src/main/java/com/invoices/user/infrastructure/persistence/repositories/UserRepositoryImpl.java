package com.invoices.user.infrastructure.persistence.repositories;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;
import com.invoices.user.infrastructure.persistence.entities.UserJpaEntity;
import com.invoices.user.infrastructure.persistence.mappers.UserJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of UserRepository port using JPA.
 * This adapter connects the domain layer with Spring Data JPA infrastructure.
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final UserJpaMapper mapper;

    public UserRepositoryImpl(JpaUserRepository jpaUserRepository, UserJpaMapper mapper) {
        this.jpaUserRepository = jpaUserRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id)
                .map(mapper::toDomainEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(mapper::toDomainEntity);
    }

    @Override
    public List<User> findAll() {
        return jpaUserRepository.findAll().stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public User save(User user) {
        UserJpaEntity jpaEntity = mapper.toJpaEntity(user);
        UserJpaEntity savedEntity = jpaUserRepository.save(jpaEntity);
        return mapper.toDomainEntity(savedEntity);
    }

    @Override
    public void deleteById(Long id) {
        jpaUserRepository.deleteById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaUserRepository.existsById(id);
    }
}

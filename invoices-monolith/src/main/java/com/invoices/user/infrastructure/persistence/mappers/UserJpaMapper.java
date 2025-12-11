package com.invoices.user.infrastructure.persistence.mappers;

import com.invoices.user.domain.entities.User;
import com.invoices.user.infrastructure.persistence.entities.UserJpaEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * Mapper to convert between domain User and JPA UserJpaEntity.
 * This isolation keeps the domain independent of JPA annotations.
 */
@Component
public class UserJpaMapper {

    /**
     * Convert domain User to JPA entity
     *
     * @param domainUser the domain user
     * @return JPA entity
     */
    public UserJpaEntity toJpaEntity(User domainUser) {
        if (domainUser == null) {
            return null;
        }

        UserJpaEntity jpaEntity = new UserJpaEntity();
        jpaEntity.setId(domainUser.getId());
        jpaEntity.setEmail(domainUser.getEmail());
        jpaEntity.setPassword(domainUser.getPassword());
        jpaEntity.setFirstName(domainUser.getFirstName());
        jpaEntity.setLastName(domainUser.getLastName());
        jpaEntity.setRoles(new HashSet<>(domainUser.getRoles()));
        jpaEntity.setEnabled(domainUser.isEnabled());
        jpaEntity.setAccountNonExpired(domainUser.isAccountNonExpired());
        jpaEntity.setAccountNonLocked(domainUser.isAccountNonLocked());
        jpaEntity.setCredentialsNonExpired(domainUser.isCredentialsNonExpired());
        jpaEntity.setCurrentCompanyId(domainUser.getCurrentCompanyId());
        jpaEntity.setCreatedAt(domainUser.getCreatedAt());
        jpaEntity.setUpdatedAt(domainUser.getUpdatedAt());
        jpaEntity.setLastLogin(domainUser.getLastLogin());
        jpaEntity.setPlatformRole(domainUser.getPlatformRole());

        return jpaEntity;
    }

    /**
     * Convert JPA entity to domain User
     *
     * @param jpaEntity the JPA entity
     * @return domain user
     */
    public User toDomainEntity(UserJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        return new User(
                jpaEntity.getId(),
                jpaEntity.getEmail(),
                jpaEntity.getPassword(),
                jpaEntity.getFirstName(),
                jpaEntity.getLastName(),
                jpaEntity.getRoles(),
                Boolean.TRUE.equals(jpaEntity.getEnabled()),
                Boolean.TRUE.equals(jpaEntity.getAccountNonExpired()),
                Boolean.TRUE.equals(jpaEntity.getAccountNonLocked()),
                Boolean.TRUE.equals(jpaEntity.getCredentialsNonExpired()),
                jpaEntity.getCreatedAt(),
                jpaEntity.getUpdatedAt(),
                jpaEntity.getLastLogin(),
                jpaEntity.getCurrentCompanyId(),
                jpaEntity.getPlatformRole());
    }
}

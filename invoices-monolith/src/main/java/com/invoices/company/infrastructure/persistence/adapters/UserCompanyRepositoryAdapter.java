package com.invoices.company.infrastructure.persistence.adapters;

import com.invoices.company.domain.entities.UserCompany;
import com.invoices.company.domain.entities.UserCompanyId;
import com.invoices.company.domain.ports.UserCompanyRepository;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing domain UserCompanyRepository port.
 * Bridges domain layer (which expects domain entities) and infrastructure layer
 * (JPA entities).
 * Converts between domain and JPA entity representations.
 */
@Repository("domainUserCompanyRepository")
@RequiredArgsConstructor
public class UserCompanyRepositoryAdapter implements UserCompanyRepository {

    private final com.invoices.company.infrastructure.persistence.repositories.UserCompanyRepository jpaRepository;
    private final com.invoices.user.infrastructure.persistence.repositories.JpaUserRepository jpaUserRepository;
    private final com.invoices.invoice.infrastructure.persistence.repositories.JpaCompanyRepository jpaCompanyRepository;

    @Override
    public Optional<UserCompany> findById(UserCompanyId id) {
        com.invoices.company.infrastructure.persistence.entities.UserCompanyId jpaId = new com.invoices.company.infrastructure.persistence.entities.UserCompanyId(
                id.getUserId(), id.getCompanyId());
        return jpaRepository.findById(jpaId).map(this::toDomain);
    }

    @Override
    public List<UserCompany> findByIdUserIdWithCompanyFetch(Long userId) {
        return jpaRepository.findByIdUserIdWithCompanyFetch(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<UserCompany> findByIdUserId(Long userId) {
        return jpaRepository.findByIdUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<UserCompany> findByIdCompanyIdWithUserFetch(Long companyId) {
        return jpaRepository.findByIdCompanyIdWithUserFetch(companyId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<UserCompany> findByIdCompanyId(Long companyId) {
        return jpaRepository.findByIdCompanyId(companyId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByIdCompanyIdAndRole(Long companyId, String role) {
        return jpaRepository.countByIdCompanyIdAndRole(companyId, role);
    }

    @Override
    public long countByIdCompanyId(Long companyId) {
        return jpaRepository.countByIdCompanyId(companyId);
    }

    @Override
    public UserCompany save(UserCompany userCompany) {
        com.invoices.company.infrastructure.persistence.entities.UserCompany jpaEntity = toJpa(userCompany);
        com.invoices.company.infrastructure.persistence.entities.UserCompany saved = jpaRepository.save(jpaEntity);
        return toDomain(saved);
    }

    @Override
    public void delete(UserCompany userCompany) {
        com.invoices.company.infrastructure.persistence.entities.UserCompany jpaEntity = toJpa(userCompany);
        jpaRepository.delete(jpaEntity);
    }

    @Override
    public void deleteByIdCompanyId(Long companyId) {
        jpaRepository.deleteByIdCompanyId(companyId);
    }

    @Override
    public void deleteByIdUserId(Long userId) {
        jpaRepository.deleteByIdUserId(userId);
    }

    @Override
    public void deleteAll(List<UserCompany> userCompanies) {
        List<com.invoices.company.infrastructure.persistence.entities.UserCompany> jpaEntities = userCompanies.stream()
                .map(this::toJpa)
                .toList();
        jpaRepository.deleteAll(jpaEntities);
    }

    // Entity conversion methods
    private UserCompany toDomain(com.invoices.company.infrastructure.persistence.entities.UserCompany jpaEntity) {
        UserCompanyId id = new UserCompanyId(
                jpaEntity.getId().getUserId(),
                jpaEntity.getId().getCompanyId());
        UserCompany domain = new UserCompany(id, jpaEntity.getRole());
        // Note: Company and User relationships are loaded separately when needed
        if (jpaEntity.getCompany() != null) {
            // Could map company here if needed
        }
        if (jpaEntity.getUser() != null) {
            // Could map user here if needed
        }
        return domain;
    }

    private com.invoices.company.infrastructure.persistence.entities.UserCompany toJpa(UserCompany domain) {
        com.invoices.company.infrastructure.persistence.entities.UserCompanyId jpaId = new com.invoices.company.infrastructure.persistence.entities.UserCompanyId(
                domain.getId().getUserId(),
                domain.getId().getCompanyId());

        com.invoices.company.infrastructure.persistence.entities.UserCompany entity = new com.invoices.company.infrastructure.persistence.entities.UserCompany(
                jpaId, domain.getRole());

        // Set references to satisfy @MapsId
        // We use getReferenceById to avoid extra DB queries - we just need the proxy to
        // link
        entity.setUser(jpaUserRepository.getReferenceById(domain.getId().getUserId()));
        entity.setCompany(jpaCompanyRepository.getReferenceById(domain.getId().getCompanyId()));

        return entity;
    }
}

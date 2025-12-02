package com.invoices.company.infrastructure.persistence.adapters;

import com.invoices.company.domain.entities.UserCompany;
import com.invoices.company.domain.entities.UserCompanyId;
import com.invoices.company.domain.ports.UserCompanyRepository;
import com.invoices.company.infrastructure.persistence.repositories.UserCompanyRepository as JpaUserCompanyRepository;
import com.invoices.company.infrastructure.persistence.entities.UserCompany as JpaUserCompany;
import com.invoices.company.infrastructure.persistence.entities.UserCompanyId as JpaUserCompanyId;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing domain UserCompanyRepository port.
 * Bridges domain layer (which expects domain entities) and infrastructure layer (JPA entities).
 * Converts between domain and JPA entity representations.
 */
@Repository("domainUserCompanyRepository")
@RequiredArgsConstructor
public class UserCompanyRepositoryAdapter implements UserCompanyRepository {

    private final JpaUserCompanyRepository jpaRepository;

    @Override
    public Optional<UserCompany> findById(UserCompanyId id) {
        JpaUserCompanyId jpaId = new JpaUserCompanyId(id.getUserId(), id.getCompanyId());
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
        JpaUserCompany jpaEntity = toJpa(userCompany);
        JpaUserCompany saved = jpaRepository.save(jpaEntity);
        return toDomain(saved);
    }

    @Override
    public void delete(UserCompany userCompany) {
        JpaUserCompany jpaEntity = toJpa(userCompany);
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

    // Entity conversion methods
    private UserCompany toDomain(JpaUserCompany jpaEntity) {
        UserCompanyId id = new UserCompanyId(
                jpaEntity.getId().getUserId(),
                jpaEntity.getId().getCompanyId()
        );
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

    private JpaUserCompany toJpa(UserCompany domain) {
        JpaUserCompanyId jpaId = new JpaUserCompanyId(
                domain.getId().getUserId(),
                domain.getId().getCompanyId()
        );
        return new JpaUserCompany(jpaId, domain.getRole());
    }
}

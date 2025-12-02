package com.invoices.company.infrastructure.persistence.adapters;

import com.invoices.company.domain.entities.CompanyInvitation;
import com.invoices.company.domain.ports.CompanyInvitationRepository;
import com.invoices.company.infrastructure.persistence.repositories.CompanyInvitationRepository as JpaCompanyInvitationRepository;
import com.invoices.company.infrastructure.persistence.entities.CompanyInvitation as JpaCompanyInvitation;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * Adapter implementing domain CompanyInvitationRepository port.
 * Bridges domain layer (which expects domain entities) and infrastructure layer (JPA entities).
 * Converts between domain and JPA entity representations.
 */
@Repository("domainCompanyInvitationRepository")
@RequiredArgsConstructor
public class CompanyInvitationRepositoryAdapter implements CompanyInvitationRepository {

    private final JpaCompanyInvitationRepository jpaRepository;

    @Override
    public Optional<CompanyInvitation> findByToken(String token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public CompanyInvitation save(CompanyInvitation invitation) {
        JpaCompanyInvitation jpaEntity = toJpa(invitation);
        JpaCompanyInvitation saved = jpaRepository.save(jpaEntity);
        return toDomain(saved);
    }

    // Entity conversion methods
    private CompanyInvitation toDomain(JpaCompanyInvitation jpaEntity) {
        CompanyInvitation domain = new CompanyInvitation(
                jpaEntity.getCompanyId(),
                jpaEntity.getEmail(),
                jpaEntity.getToken(),
                jpaEntity.getRole(),
                jpaEntity.getExpiresAt()
        );
        domain.setId(jpaEntity.getId());
        domain.setStatus(jpaEntity.getStatus());
        domain.setCreatedAt(jpaEntity.getCreatedAt());
        return domain;
    }

    private JpaCompanyInvitation toJpa(CompanyInvitation domain) {
        JpaCompanyInvitation jpaEntity = new JpaCompanyInvitation(
                domain.getCompanyId(),
                domain.getEmail(),
                domain.getToken(),
                domain.getRole(),
                domain.getExpiresAt()
        );
        jpaEntity.setId(domain.getId());
        jpaEntity.setStatus(domain.getStatus());
        jpaEntity.setCreatedAt(domain.getCreatedAt());
        return jpaEntity;
    }
}

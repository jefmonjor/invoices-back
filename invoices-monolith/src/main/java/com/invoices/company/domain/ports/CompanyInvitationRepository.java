package com.invoices.company.domain.ports;

import com.invoices.company.domain.entities.CompanyInvitation;

import java.util.Optional;

/**
 * Port for accessing CompanyInvitation data.
 * This is a domain interface that will be implemented by infrastructure layer.
 * Keeps domain layer independent of database implementation.
 */
public interface CompanyInvitationRepository {

    /**
     * Find an invitation by token.
     *
     * @param token the invitation token
     * @return Optional containing the invitation if found
     */
    Optional<CompanyInvitation> findByToken(String token);

    /**
     * Save an invitation.
     *
     * @param invitation the invitation to save
     * @return the saved invitation
     */
    CompanyInvitation save(CompanyInvitation invitation);
}

package com.invoices.company.application.services;

import com.invoices.company.infrastructure.persistence.entities.CompanyInvitation;
import com.invoices.company.infrastructure.persistence.repositories.CompanyInvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompanyInvitationService {

    private final CompanyInvitationRepository invitationRepository;
    private final CompanyManagementService companyManagementService;

    public CompanyInvitationService(CompanyInvitationRepository invitationRepository,
            CompanyManagementService companyManagementService) {
        this.invitationRepository = invitationRepository;
        this.companyManagementService = companyManagementService;
    }

    @Transactional
    public CompanyInvitation createInvitation(Long companyId, String email, String role) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7); // 7 days expiration

        CompanyInvitation invitation = new CompanyInvitation(companyId, email, token, role, expiresAt);
        return invitationRepository.save(invitation);
    }

    @Transactional
    public void acceptInvitation(String token, Long userId) {
        Optional<CompanyInvitation> invitationOpt = invitationRepository.findByToken(token);
        if (invitationOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid invitation token");
        }

        CompanyInvitation invitation = invitationOpt.get();
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new IllegalArgumentException("Invitation is not pending");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
            throw new IllegalArgumentException("Invitation expired");
        }

        // Add user to company
        companyManagementService.addUserToCompany(userId, invitation.getCompanyId(), invitation.getRole());

        // Update invitation status
        invitation.setStatus("ACCEPTED");
        invitationRepository.save(invitation);
    }
}

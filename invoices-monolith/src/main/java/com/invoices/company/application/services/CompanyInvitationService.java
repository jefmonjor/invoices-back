package com.invoices.company.application.services;

import com.invoices.company.domain.entities.CompanyInvitation;
import com.invoices.company.domain.ports.CompanyInvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompanyInvitationService {

    private final CompanyInvitationRepository invitationRepository;
    private final CompanyManagementService companyManagementService;
    private final com.invoices.invoice.domain.ports.CompanyRepository companyRepository;

    public CompanyInvitationService(CompanyInvitationRepository invitationRepository,
            CompanyManagementService companyManagementService,
            com.invoices.invoice.domain.ports.CompanyRepository companyRepository) {
        this.invitationRepository = invitationRepository;
        this.companyManagementService = companyManagementService;
        this.companyRepository = companyRepository;
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
        // For generic codes, we might not want to mark as ACCEPTED immediately if it's
        // reusable?
        // But the current implementation assumes one-time use per token row.
        // If we want reusable codes, we need a different mechanism or don't set to
        // ACCEPTED for generic codes.
        // For now, let's assume one-time use or that generic codes are handled
        // differently.
        // Given the "GENERIC_CODE" email, maybe we should check that.

        if (!"GENERIC_CODE".equals(invitation.getEmail())) {
            invitation.setStatus("ACCEPTED");
            invitationRepository.save(invitation);
        }
        // If it IS generic code, do we expire it? Or keep it valid until expiration
        // time?
        // Usually generic codes are reusable until expiration.
    }

    @Transactional
    public String createInvitation(Long companyId, String username, int expiresInHours) {
        // Generate a simple code for manual entry (e.g., 8 chars alphanumeric)
        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expiresInHours);

        // We need the user ID of the creator. Assuming username is email.
        // For now, we'll just store the code. The entity might need adjustment if we
        // want to store creator ID.
        // The existing createInvitation method uses email (invitee email).
        // The new requirement is generating a code for ANYONE to join.

        // Let's create a new invitation record.
        // Note: The existing entity seems to expect an email. If we want a generic
        // code, we might need to adjust the entity or use a placeholder.
        // Let's use a placeholder email or null if allowed.

        CompanyInvitation invitation = new CompanyInvitation(companyId, "GENERIC_CODE", code, "USER", expiresAt);
        invitationRepository.save(invitation);

        return code;
    }

    public com.invoices.invoice.domain.entities.Company validateInvitation(String code) {
        Optional<CompanyInvitation> invitationOpt = invitationRepository.findByToken(code);
        if (invitationOpt.isEmpty()) {
            throw new RuntimeException("Invalid invitation code");
        }

        CompanyInvitation invitation = invitationOpt.get();
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invitation expired");
        }

        return companyRepository.findById(invitation.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));
    }
}

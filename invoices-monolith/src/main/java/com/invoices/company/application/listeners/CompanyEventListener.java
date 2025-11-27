package com.invoices.company.application.listeners;

import com.invoices.company.domain.events.UserJoinedCompanyEvent;
import com.invoices.company.infrastructure.persistence.entities.UserCompany;
import com.invoices.company.infrastructure.persistence.repositories.UserCompanyRepository;
import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Listens for UserJoinedCompanyEvent and sends email notifications to company
 * ADMINs.
 * 
 * TODO: Implement actual email sending service
 * For now, just logs the notification.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyEventListener {

    private final UserCompanyRepository userCompanyRepository;
    private final UserRepository userRepository;
    // TODO: Inject EmailService when available
    // private final EmailService emailService;

    @EventListener
    public void handleUserJoinedCompany(UserJoinedCompanyEvent event) {
        log.info("User {} joined company {}", event.getUserEmail(), event.getCompanyId());

        // Get all ADMIN users in the company
        List<UserCompany> admins = userCompanyRepository.findByIdCompanyId(event.getCompanyId())
                .stream()
                .filter(uc -> "ADMIN".equals(uc.getRole()))
                .collect(Collectors.toList());

        // Get full user details for email
        List<User> adminUsers = admins.stream()
                .map(uc -> userRepository.findById(uc.getId().getUserId()))
                .filter(optional -> optional.isPresent())
                .map(optional -> optional.get())
                .collect(Collectors.toList());

        // Send notification to each ADMIN
        for (User admin : adminUsers) {
            sendNotificationEmail(admin, event);
        }
    }

    private void sendNotificationEmail(User admin, UserJoinedCompanyEvent event) {
        // TODO: Replace with actual email service
        log.info("Sending email to ADMIN: {}", admin.getEmail());
        log.info("Subject: New user joined your company");
        log.info("Message: {} ({}) has joined your company",
                event.getUserName(), event.getUserEmail());

        /*
         * TODO: Implement with EmailService
         * 
         * emailService.sendEmail(EmailMessage.builder()
         * .to(admin.getEmail())
         * .subject("New user joined your company")
         * .body("User " + event.getUserName() + " (" + event.getUserEmail() +
         * ") has joined your company.")
         * .build());
         */
    }
}

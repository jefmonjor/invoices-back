package com.invoices.verifactu.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Asynchronous email service for certificate-related notifications.
 * Uses @Async to prevent blocking the main thread during email sending.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${verifactu.email.from:noreply@invoices.com}")
    private String fromEmail;

    @Value("${verifactu.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    /**
     * Sends an asynchronous email notification about an expiring certificate.
     * 
     * @param toEmail             Recipient email (company admin)
     * @param companyName         Name of the company
     * @param certificateSubject  Subject from the X509 certificate
     * @param certificateIssuer   Issuer from the X509 certificate
     * @param expirationDate      Certificate expiration date
     * @param daysUntilExpiration Days remaining until expiration
     */
    @Async("taskExecutor")
    public void sendCertificateExpiringEmail(
            String toEmail,
            String companyName,
            String certificateSubject,
            String certificateIssuer,
            LocalDate expirationDate,
            long daysUntilExpiration) {

        if (!emailEnabled) {
            log.debug("Email is disabled, skipping certificate expiration notification");
            return;
        }

        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Cannot send certificate expiration email: no recipient email provided");
            return;
        }

        try {
            log.info("Sending certificate expiration email to {} for company {}", toEmail, companyName);

            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("companyName", companyName);
            context.setVariable("certificateSubject", formatSubject(certificateSubject));
            context.setVariable("certificateIssuer", formatIssuer(certificateIssuer));
            context.setVariable("expirationDate", expirationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("daysUntilExpiration", daysUntilExpiration);
            context.setVariable("renewUrl", appBaseUrl + "/settings/certificate");

            // Render HTML template
            String htmlContent = templateEngine.process("certificate-expiring", context);

            // Create and send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(buildSubject(companyName, daysUntilExpiration));
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Certificate expiration email sent successfully to {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send certificate expiration email to {}: {}", toEmail, e.getMessage(), e);
            // Don't throw - async method should not propagate exceptions
        }
    }

    private String buildSubject(String companyName, long daysUntilExpiration) {
        if (daysUntilExpiration <= 7) {
            return String.format("⚠️ URGENTE: Tu certificado VeriFactu caduca en %d días - %s",
                    daysUntilExpiration, companyName);
        } else if (daysUntilExpiration <= 0) {
            return String.format("🚨 CRÍTICO: Tu certificado VeriFactu ha CADUCADO - %s", companyName);
        } else {
            return String.format("📋 Recordatorio: Tu certificado VeriFactu caduca en %d días - %s",
                    daysUntilExpiration, companyName);
        }
    }

    private String formatSubject(String subject) {
        if (subject == null)
            return "-";
        // Extract CN= from X500 format
        if (subject.contains("CN=")) {
            int start = subject.indexOf("CN=") + 3;
            int end = subject.indexOf(",", start);
            return end > start ? subject.substring(start, end) : subject.substring(start);
        }
        return subject;
    }

    private String formatIssuer(String issuer) {
        if (issuer == null)
            return "-";
        // Extract CN= from X500 format
        if (issuer.contains("CN=")) {
            int start = issuer.indexOf("CN=") + 3;
            int end = issuer.indexOf(",", start);
            return end > start ? issuer.substring(start, end) : issuer.substring(start);
        }
        return issuer;
    }
}

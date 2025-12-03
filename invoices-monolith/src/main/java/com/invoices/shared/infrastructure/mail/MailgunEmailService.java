package com.invoices.shared.infrastructure.mail;

import com.invoices.shared.domain.ports.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class MailgunEmailService implements EmailService {

    private final RestClient restClient;
    private final SpringTemplateEngine templateEngine;

    @Value("${mailgun.domain}")
    private String mailgunDomain;

    @Value("${mailgun.api-key}")
    private String mailgunApiKey;

    @Value("${verifactu.email.from:noreply@invoices.com}")
    private String fromEmail;

    @Override
    public void sendEmail(String to, String subject, String body) {
        sendSimpleEmail(to, subject, body);
    }

    public void sendSimpleEmail(String to, String subject, String body) {
        log.info("Sending simple email to: {}", to);

        try {
            restClient.post()
                    .uri("https://api.mailgun.net/v3/" + mailgunDomain + "/messages")
                    .headers(headers -> headers.setBasicAuth("api", mailgunApiKey))
                    .body(new LinkedMultiValueMap<String, String>() {
                        {
                            add("from", fromEmail);
                            add("to", to);
                            add("subject", subject);
                            add("text", body);
                        }
                    })
                    .retrieve()
                    .body(String.class);

            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        log.info("Sending HTML email to: {} using template: {}", to, templateName);

        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlBody = templateEngine.process(templateName, context);

            restClient.post()
                    .uri("https://api.mailgun.net/v3/" + mailgunDomain + "/messages")
                    .headers(headers -> headers.setBasicAuth("api", mailgunApiKey))
                    .body(new LinkedMultiValueMap<String, String>() {
                        {
                            add("from", fromEmail);
                            add("to", to);
                            add("subject", subject);
                            add("html", htmlBody);
                        }
                    })
                    .retrieve()
                    .body(String.class);

            log.info("HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Error sending HTML email to {}: {}", to, e.getMessage(), e);
        }
    }
}

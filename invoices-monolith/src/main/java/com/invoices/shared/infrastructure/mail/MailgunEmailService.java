package com.invoices.shared.infrastructure.mail;

import com.invoices.shared.domain.ports.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class MailgunEmailService implements EmailService {

    private final WebClient webClient;
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
        log.info("Sending email to: {}", to);

        webClient.post()
                .uri("https://api.mailgun.net/v3/" + mailgunDomain + "/messages")
                .headers(headers -> headers.setBasicAuth("api", mailgunApiKey))
                .body(BodyInserters.fromFormData("from", fromEmail)
                        .with("to", to)
                        .with("subject", subject)
                        .with("text", body))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Email sent successfully: {}", response))
                .doOnError(error -> log.error("Error sending email", error))
                .subscribe();
    }

    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        log.info("Sending HTML email to: {} using template: {}", to, templateName);

        Context context = new Context();
        context.setVariables(variables);
        String htmlBody = templateEngine.process(templateName, context);

        webClient.post()
                .uri("https://api.mailgun.net/v3/" + mailgunDomain + "/messages")
                .headers(headers -> headers.setBasicAuth("api", mailgunApiKey))
                .body(BodyInserters.fromFormData("from", fromEmail)
                        .with("to", to)
                        .with("subject", subject)
                        .with("html", htmlBody))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("HTML Email sent successfully: {}", response))
                .doOnError(error -> log.error("Error sending HTML email", error))
                .subscribe();
    }
}

package com.invoices.shared.infrastructure.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class MailgunEmailService {

    private final WebClient webClient;

    @Value("${mailgun.domain}")
    private String domain;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${verifactu.email.from:noreply@invoices.com}")
    private String defaultFromEmail;

    public MailgunEmailService(WebClient mailgunWebClient) {
        this.webClient = mailgunWebClient;
    }

    /**
     * Sends a password reset email using Mailgun API.
     *
     * @param toEmail The recipient's email address.
     * @param token   The password reset token.
     * @return A Mono<Void> that completes when the email is sent.
     */
    public Mono<Void> sendPasswordReset(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("from", "Invoices App <" + defaultFromEmail + ">");
        form.add("to", toEmail);
        form.add("subject", "Restablece tu contraseña");

        String html = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">"
                + "<h2>Restablecimiento de Contraseña</h2>"
                + "<p>Has solicitado restablecer tu contraseña en Invoices App.</p>"
                + "<p>Para continuar, haz clic en el siguiente botón:</p>"
                + "<p><a href=\"" + resetLink
                + "\" style=\"background-color: #4F46E5; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;\">Restablecer Contraseña</a></p>"
                + "<p>O copia y pega el siguiente enlace en tu navegador:</p>"
                + "<p>" + resetLink + "</p>"
                + "<p>Este enlace expirará en 1 hora.</p>"
                + "<p>Si no solicitaste esto, puedes ignorar este mensaje.</p>"
                + "</div>";

        form.add("html", html);

        String endpoint = "/" + domain + "/messages";

        log.info("Sending password reset email to {} via Mailgun domain {}", toEmail, domain);

        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .onStatus(HttpStatus::isError, resp -> resp.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("Mailgun API error: {}", body);
                            return Mono.error(new RuntimeException("Mailgun error: " + body));
                        }))
                .bodyToMono(String.class)
                .doOnNext(response -> log.debug("Mailgun response: {}", response))
                .then();
    }
}

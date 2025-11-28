package com.invoices.shared.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class MailgunConfig {

    @Value("${mailgun.api-key}")
    private String mailgunApiKey;

    @Value("${mailgun.api-base}")
    private String mailgunApiBase;

    @Bean
    public WebClient mailgunWebClient() {
        return WebClient.builder()
                .baseUrl(mailgunApiBase)
                .defaultHeaders(headers -> headers.setBasicAuth("api", mailgunApiKey))
                .build();
    }
}

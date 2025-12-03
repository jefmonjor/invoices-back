package com.invoices.shared.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MailgunConfig {

    @Value("${mailgun.api-key}")
    private String mailgunApiKey;

    @Value("${mailgun.api-base}")
    private String mailgunApiBase;

    @Value("${mailgun.connection-timeout-ms:5000}")
    private long connectionTimeoutMs;

    @Value("${mailgun.read-timeout-ms:10000}")
    private long readTimeoutMs;

    @Bean
    public RestClient mailgunRestClient() {
        return RestClient.builder()
                .baseUrl(mailgunApiBase)
                .defaultHeaders(headers -> headers.setBasicAuth("api", mailgunApiKey))
                .build();
    }
}

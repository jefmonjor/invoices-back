package com.invoices.shared.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import java.util.concurrent.TimeUnit;

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
    public WebClient mailgunWebClient() {
        // Configure HTTP client with timeouts
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(java.time.Duration.ofMillis(readTimeoutMs))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS));
                });

        return WebClient.builder()
                .baseUrl(mailgunApiBase)
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .defaultHeaders(headers -> headers.setBasicAuth("api", mailgunApiKey))
                .build();
    }
}

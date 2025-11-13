package com.invoices.invoice_service.config;

import feign.Logger;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Feign Client
 */
@Configuration
@EnableFeignClients(basePackages = "com.invoices.invoice_service.client")
@Slf4j
public class FeignConfig {

    /**
     * Configura el nivel de logging para Feign
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Interceptor para agregar headers comunes a todas las peticiones Feign
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            log.debug("Feign request: {} {}", requestTemplate.method(), requestTemplate.url());
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("Accept", "application/json");
        };
    }
}

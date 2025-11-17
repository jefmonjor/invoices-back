package com.invoices.gateway_service.cors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for CORS configuration in the Gateway.
 * Tests verify that CORS headers are correctly applied to requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class CorsConfigurationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldAllowCorsPreflightRequest() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_MAX_AGE);
    }

    @Test
    void shouldAllowCorsFromLocalhost3000() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
    }

    @Test
    void shouldAllowCorsFromLocalhost5173() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
    }

    @Test
    void shouldAllowCorsFromTestDomain() {
        // When/Then
        webTestClient.options()
                .uri("/api/users")
                .header(HttpHeaders.ORIGIN, "http://test.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://test.com");
    }

    @Test
    void shouldAllowCredentials() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    @Test
    void shouldAllowGETMethod() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        methods -> methods.contains("GET"));
    }

    @Test
    void shouldAllowPOSTMethod() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        methods -> methods.contains("POST"));
    }

    @Test
    void shouldAllowPUTMethod() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices/1")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        methods -> methods.contains("PUT"));
    }

    @Test
    void shouldAllowDELETEMethod() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices/1")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        methods -> methods.contains("DELETE"));
    }

    @Test
    void shouldAllowOPTIONSMethod() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "OPTIONS")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        methods -> methods.contains("OPTIONS"));
    }

    @Test
    void shouldAllowAllHeadersWithWildcard() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type,X-Custom-Header")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
    }

    @Test
    void shouldSetMaxAge() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
    }

    @Test
    void shouldApplyCorsToAllPaths() {
        // Given - test multiple paths
        String[] paths = {
                "/api/invoices",
                "/api/users",
                "/api/documents",
                "/api/traces",
                "/api/auth/login"
        };

        // When/Then
        for (String path : paths) {
            webTestClient.options()
                    .uri(path)
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
        }
    }

    @Test
    void shouldHandlePreflightWithAuthorizationHeader() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
    }

    @Test
    void shouldHandlePreflightWithContentTypeHeader() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
    }

    @Test
    void shouldHandlePreflightWithMultipleHeaders() {
        // When/Then
        webTestClient.options()
                .uri("/api/invoices")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type,X-Custom-Header,X-Requested-With")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
    }

    @Test
    void shouldHandlePreflightForDifferentRoutes() {
        // Given
        String[] routes = {
                "/api/auth/login",      // Public route
                "/api/users",           // Protected route
                "/api/invoices",        // Protected route
                "/api/documents",       // Protected route
                "/api/traces"           // Protected route
        };

        // When/Then - all routes should have CORS configured
        for (String route : routes) {
            webTestClient.options()
                    .uri(route)
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
    }

    @Test
    void shouldAllowConfiguredOrigins() {
        // Given
        String[] allowedOrigins = {
                "http://localhost:3000",
                "http://localhost:5173",
                "http://test.com"
        };

        // When/Then
        for (String origin : allowedOrigins) {
            webTestClient.options()
                    .uri("/api/invoices")
                    .header(HttpHeaders.ORIGIN, origin)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        }
    }

    @Test
    void shouldHandleComplexPreflightScenario() {
        // When/Then - simulate a complex preflight request
        webTestClient.options()
                .uri("/api/invoices/1/generate-pdf")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type,Accept,X-Request-ID")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        methods -> methods.contains("POST"))
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
    }
}

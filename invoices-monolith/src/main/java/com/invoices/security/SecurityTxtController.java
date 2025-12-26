package com.invoices.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for serving the security.txt file.
 * This follows the RFC 9116 standard for security vulnerability disclosure.
 * 
 * @see <a href="https://securitytxt.org/">Security.txt Standard</a>
 */
@RestController
public class SecurityTxtController {

    @Value("${app.security.contact-email:security@example.com}")
    private String contactEmail;

    @Value("${app.base-url:https://localhost:8080}")
    private String baseUrl;

    @GetMapping(value = "/.well-known/security.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> securityTxt() {
        String content = """
                # Security Policy
                # If you discover a security vulnerability, please report it responsibly.

                Contact: mailto:%s
                Preferred-Languages: es, en
                Canonical: %s/.well-known/security.txt
                """.formatted(contactEmail, baseUrl);

        return ResponseEntity.ok(content);
    }
}

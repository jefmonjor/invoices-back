package com.invoices.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration for the monolithic application.
 * Configures JWT-based authentication with stateless sessions.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final UserDetailsService userDetailsService;

        @Value("${cors.allowed-origins}")
        private String[] allowedOrigins;

        @Value("${cors.allowed-methods}")
        private String[] allowedMethods;

        @Value("${cors.allowed-headers}")
        private String[] allowedHeaders;

        @Value("${cors.allow-credentials}")
        private boolean allowCredentials;

        /**
         * Configures the security filter chain.
         *
         * @param http the HttpSecurity to configure
         * @return the configured SecurityFilterChain
         * @throws Exception if an error occurs during configuration
         */
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                log.info("Configuring security filter chain");

                http
                                // Disable CSRF as we're using JWT (stateless)
                                .csrf(AbstractHttpConfigurer::disable)

                                // Enable CORS with custom configuration
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // Configure security headers
                                .headers(headers -> headers
                                                // Prevent clickjacking attacks
                                                .frameOptions(frame -> frame.deny())

                                                // Prevent MIME type sniffing
                                                .contentTypeOptions(contentType -> contentType.disable())

                                                // Enable XSS protection
                                                .xssProtection(xss -> xss
                                                                .headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))

                                                // Force HTTPS in production (Strict-Transport-Security)
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000) // 1 year
                                                )

                                                // Content Security Policy to prevent XSS and data injection attacks
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives("default-src 'self'; " +
                                                                                "script-src 'self' 'unsafe-inline'; " +
                                                                                "style-src 'self' 'unsafe-inline'; " +
                                                                                "img-src 'self' data:; " +
                                                                                "font-src 'self' data:; " +
                                                                                "connect-src 'self'"))

                                                // Referrer policy
                                                .referrerPolicy(referrer -> referrer
                                                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))

                                                // Permissions policy (formerly Feature-Policy)
                                                .permissionsPolicy(permissions -> permissions
                                                                .policy("geolocation=(), microphone=(), camera=()")))

                                // Configure authorization rules
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints - authentication not required
                                                .requestMatchers(
                                                                "/api/auth/**",
                                                                "/health/simple", // Simple health check for Railway
                                                                "/health/ready", // Lightweight readiness probe
                                                                "/actuator/health",
                                                                "/actuator/health/**", // Allow readiness/liveness
                                                                                       // probes
                                                                "/actuator/info",
                                                                "/v3/api-docs/**",
                                                                "/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()

                                                // Protected endpoints - require authentication
                                                .requestMatchers("/api/users/**").authenticated()
                                                .requestMatchers("/api/invoices/**").authenticated()
                                                .requestMatchers("/api/companies/**").authenticated()
                                                .requestMatchers("/api/clients/**").authenticated()
                                                .requestMatchers("/api/documents/**").authenticated()
                                                .requestMatchers("/api/audit/**").authenticated()

                                                // All other requests require authentication
                                                .anyRequest().authenticated())

                                // Stateless session management (no session stored on server)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Set authentication provider
                                .authenticationProvider(authenticationProvider())

                                // Add JWT filter before username/password authentication filter
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                log.info("Security filter chain configured successfully");
                return http.build();
        }

        /**
         * Configures the authentication provider.
         *
         * @return the configured AuthenticationProvider
         */
        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                log.debug("Authentication provider configured");
                return authProvider;
        }

        /**
         * Creates a BCrypt password encoder bean.
         * Strength 12 provides better security against brute force attacks
         * while maintaining acceptable performance.
         *
         * @return the password encoder
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                log.debug("Creating BCrypt password encoder with strength 12");
                return new BCryptPasswordEncoder(12);
        }

        /**
         * Exposes the authentication manager as a bean.
         *
         * @param config the authentication configuration
         * @return the authentication manager
         * @throws Exception if an error occurs
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                log.debug("Creating authentication manager");
                return config.getAuthenticationManager();
        }

        /**
         * Configures CORS (Cross-Origin Resource Sharing) settings.
         * This is required for Spring Security to properly handle CORS preflight
         * requests.
         * Uses setAllowedOriginPatterns to support wildcard patterns like
         * https://*.vercel.app
         *
         * @return the CORS configuration source
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                log.info("Configuring CORS in Spring Security for origin patterns: {}",
                                String.join(", ", allowedOrigins));

                CorsConfiguration configuration = new CorsConfiguration();
                // Use setAllowedOriginPatterns instead of setAllowedOrigins to support
                // wildcards
                configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
                configuration.setAllowedMethods(Arrays.asList(allowedMethods));
                configuration.setAllowedHeaders(Arrays.asList(allowedHeaders));
                configuration.setAllowCredentials(allowCredentials);
                configuration.setExposedHeaders(
                                Arrays.asList("Authorization", "X-Rate-Limit-Remaining", "X-Rate-Limit-Reset"));
                configuration.setMaxAge(3600L); // 1 hour

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                log.info("CORS configured - Allow credentials: {}, Methods: {}", allowCredentials,
                                String.join(", ", allowedMethods));
                return source;
        }
}

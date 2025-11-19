package com.eaglebank.api.security;

import com.eaglebank.api.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration class to customize Spring Security behavior.
 * This setup permits all requests and specifically disables frame protection
 * for the H2 console, allowing it to load correctly in the browser.
 */
@Configuration
@EnableWebSecurity // Ensures Spring Security configuration is enabled
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;


    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Defines the PasswordEncoder using the secure BCrypt algorithm.
     * This bean is used by the AuthenticationManager to validate login passwords against stored hashes.
     */
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        // IMPORTANT: Using BCryptPasswordEncoder for secure hashing
//        return new BCryptPasswordEncoder();
//    }

    /**
     * Defines the PasswordEncoder (using NoOp for simplicity in this demo).
     * In production, always use BCryptPasswordEncoder or similar strong algorithm.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // WARNING: {noop} is ONLY for demonstration. Use BCrypt or similar in production.
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Bean for the custom JWT filter.
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * Defines the security filter chain to configure authorization rules.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 1. Disable CSRF (standard for stateless REST APIs)
                .csrf(csrf -> csrf.disable())

                // 2. IMPORTANT: Configure session management to be stateless (JWT requirement)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. Configure authorization for HTTP requests
                .authorizeHttpRequests(authorize -> authorize

                        // FIX: Explicitly allow POST requests to the login endpoint to avoid 403 Forbidden issues.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // NEW FIX: Allow POST requests to the user creation/registration endpoint.
                        // This lets unauthenticated users register, triggering validation checks (400) instead of security blocks (403).
                        .requestMatchers(HttpMethod.POST, "/v1/users").permitAll()

                        // Allow public access to the authentication endpoint and H2 console
                        .requestMatchers("/h2-console/**").permitAll()
                        // Secure all User CRUD APIs - requires authentication
                        .requestMatchers("/v1/users/**").authenticated() //TODO:Add other endpoints later
                        // Require authentication for all other requests by default
                        .anyRequest().authenticated()
                )

                // 4. IMPORTANT FIX FOR H2 CONSOLE: Disable frame options to allow the console to render
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                )

                // 5. Add the custom JWT filter before Spring's standard authentication filter
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
package com.eaglebank.api.controller;

import org.springframework.http.ResponseEntity;
import com.eaglebank.api.beans.LoginRequest;
import com.eaglebank.api.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to handle user authentication and JWT token generation.
 * Endpoint: /api/auth
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Endpoint to authenticate user credentials and generate a JWT token.
     * Accessible at: POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<String> authenticateUser(@RequestBody LoginRequest loginRequest) {

        // 1. Authenticate the user using the credentials provided
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // 2. Set the authentication object in the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Generate a JWT token for the authenticated user
        String jwt = tokenProvider.generateToken(authentication);

        // 4. Return the token to the client
        return ResponseEntity.ok(jwt);
    }
}

package com.eaglebank.api.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 *
 * These tests exercise token generation, validation and username extraction.
 *
 * NOTE:
 * The real JwtTokenProvider in the project reads secret/expiration from properties.
 * Here we initialize the instance and set its private fields using ReflectionTestUtils
 * to make behavior deterministic for tests.
 */
public class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        provider = new JwtTokenProvider();

        // Use a predictable raw secret, then Base64-encode it before setting it into the provider.
        // This mirrors common JwtTokenProvider implementations that expect a Base64-encoded secret.
        String rawSecret = "ThisIsAMuchLongerAndMoreSecureSecretKeyForJWTAuthenticationInSpring357ThatExceedsThe256BitMinimumRequirement";
        String base64Secret = Base64.getEncoder().encodeToString(rawSecret.getBytes());

        ReflectionTestUtils.setField(provider, "jwtSecret", base64Secret);
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 3600000L); // 1 hour
    }

    @Test
    void generateAndValidateToken_returnsValidTokenAndContainsSubject() {
        // JwtTokenProvider.generateToken expects authentication.getPrincipal() to be a UserDetails object.
        User principal = new User("user@example.com", "password", List.of());
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String token = provider.generateToken(auth);

        assertNotNull(token);
        assertTrue(provider.validateToken(token), "Generated token should validate");

        // Extract the Base64-encoded secret from the provider and build the same signing key the provider uses.
        String base64Secret = (String) ReflectionTestUtils.getField(provider, "jwtSecret");
        var signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));

        String subject = Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        assertEquals("user@example.com", subject);
    }

    @Test
    void validateToken_withTamperedToken_returnsFalse_or_throwsJwtException() {
        // Provide UserDetails principal to match provider expectations
        User principal = new User("user2@example.com", "password", List.of());
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String token = provider.generateToken(auth);

        // Tamper token by altering a character (simple approach)
        String tampered = token.substring(0, token.length() - 1) + (token.charAt(token.length()-1) == 'a' ? 'b' : 'a');

        // Accept either a boolean false return or a JwtException being thrown depending on provider implementation.
        try {
            boolean valid = provider.validateToken(tampered);
            assertFalse(valid, "Tampered token should not validate and provider.validateToken should return false");
        } catch (RuntimeException e) {
            // Provider may throw JwtException (or a subclass like SignatureException) for invalid tokens.
            assertTrue(e instanceof JwtException, "Expected a JwtException (or subclass) when validating tampered token");
        }
    }

    @Test
    void parse_invalidToken_throwsException() {
        String base64Secret = (String) ReflectionTestUtils.getField(provider, "jwtSecret");
        var signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));

        assertThrows(RuntimeException.class, () -> {
            // JJWT throws JwtException (a RuntimeException) for invalid tokens
            Jwts.parser()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws("not-a-valid-token");
        });
    }
}
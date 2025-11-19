package com.eaglebank.api.security;

import com.eaglebank.api.service.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 *
 * These tests verify the filter's behavior for:
 *  - valid bearer token -> sets Authentication in SecurityContext
 *  - missing Authorization header -> does not set Authentication
 *  - invalid token (validateToken returns false) -> does not set Authentication
 *  - token provider throws -> filter catches and processing continues
 */
public class JwtAuthenticationFilterTest {

    private JwtTokenProvider tokenProvider;
    private CustomUserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(JwtTokenProvider.class);
        userDetailsService = mock(CustomUserDetailsService.class);

        filter = new JwtAuthenticationFilter();
        // inject mocked dependencies into the filter's autowired fields
        ReflectionTestUtils.setField(filter, "tokenProvider", tokenProvider);
        ReflectionTestUtils.setField(filter, "customUserDetailsService", userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        Mockito.reset(tokenProvider, userDetailsService);
    }

    @Test
    void doFilterInternal_validToken_setsAuthentication() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String header = "Bearer " + token;
        String username = "user@example.com";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", header);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromJwt(token)).thenReturn(username);

        UserDetails userDetails = new User(username, "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be set");
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());

        verify(tokenProvider, times(1)).validateToken(token);
        verify(tokenProvider, times(1)).getUsernameFromJwt(token);
        verify(userDetailsService, times(1)).loadUserByUsername(username);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_missingAuthorizationHeader_doesNotSetAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "No authentication should be set");
        verify(tokenProvider, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_invalidToken_validateReturnsFalse_noAuthentication() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(tokenProvider.validateToken(token)).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication must not be set for invalid token");
        verify(tokenProvider, times(1)).validateToken(token);
        verify(tokenProvider, never()).getUsernameFromJwt(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_tokenProviderThrows_exceptionIsCaught_andProcessingContinues() throws ServletException, IOException {
        String token = "some.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(tokenProvider.validateToken(token)).thenThrow(new JwtException("boom"));

        // Should not propagate; filter should catch and allow chain to continue
        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should not be set when token provider throws");
        verify(tokenProvider, times(1)).validateToken(token);
        verify(chain, times(1)).doFilter(request, response);
    }
}
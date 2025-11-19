package com.eaglebank.api.service;

import com.eaglebank.api.beans.Account;
import com.eaglebank.api.beans.User;
import com.eaglebank.api.exception.ResourceNotFoundException;
import com.eaglebank.api.repository.AccountRepository;
import com.eaglebank.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomUserDetailsService
 */
@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setName("Test User");
    }

    @Test
    void loadUserByUsername_knownOtherUser_returnsUserDetails() {
        // otheruser@example.com is handled by hardcoded logic in service
        UserDetails details = service.loadUserByUsername("otheruser@example.com");

        assertNotNull(details);
        assertEquals("otheruser@example.com", details.getUsername());
        assertNotNull(details.getPassword());
        assertFalse(details.getAuthorities().isEmpty());
    }

    @Test
    void loadUserByUsername_knownUser_returnsUserDetails() {
        UserDetails details = service.loadUserByUsername("user@example.com");

        assertNotNull(details);
        assertEquals("user@example.com", details.getUsername());
        assertNotNull(details.getPassword());
        assertFalse(details.getAuthorities().isEmpty());
    }

    @Test
    void loadUserByUsername_unknown_throwsUsernameNotFoundException() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("unknown@example.com"));
    }

    @Test
    void deleteUserWithoutBankAccount_userExistsAndNoAccounts_deletesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.findAllByUserId(1L)).thenReturn(List.of());

        service.deleteUserWithoutBankAccount(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUserWithoutBankAccount_userExistsWithAccounts_throwsIllegalStateException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Account acc = new Account();
        acc.setId(10L);
        acc.setUserId(1L);
        when(accountRepository.findAllByUserId(1L)).thenReturn(List.of(acc));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.deleteUserWithoutBankAccount(1L));
        assertTrue(ex.getMessage().contains("Cannot delete user with ID"));
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteUserWithoutBankAccount_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteUserWithoutBankAccount(999L));
        verify(userRepository, never()).deleteById(anyLong());
    }
}

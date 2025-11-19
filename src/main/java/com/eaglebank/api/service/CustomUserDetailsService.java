package com.eaglebank.api.service;

import com.eaglebank.api.beans.Account;
import com.eaglebank.api.beans.User;
import com.eaglebank.api.exception.ResourceNotFoundException;
import com.eaglebank.api.repository.AccountRepository;
import com.eaglebank.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 * Responsible for loading user-specific data (username, password, roles).
 * * NOTE: For this simple demonstration, a hardcoded user "admin" is used.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    /**
     * Inject the UserRepository to fetch user details from the database.
     */
    public CustomUserDetailsService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Loads the UserDetails object using a hardcoded lookup based on the email address.
     * The 'username' parameter is expected to be the user's email address.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // --- Hardcoded users and credentials ---
        final String OTHER_USER_EMAIL = "otheruser@example.com";
        final String USER_EMAIL = "user@example.com";
        // Passwords must be prefixed with {noop} since NoOpPasswordEncoder is configured
        final String OTHER_USER_PASSWORD = "{noop}password";
        final String USER_PASSWORD = "{noop}guest";

        if (OTHER_USER_EMAIL.equals(username)) {
            // Admin user
            return new org.springframework.security.core.userdetails.User(
                    OTHER_USER_EMAIL,
                    OTHER_USER_PASSWORD,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        } else if (USER_EMAIL.equals(username)) {
            // Standard user
            return new org.springframework.security.core.userdetails.User(
                    USER_EMAIL,
                    USER_PASSWORD,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }

        throw new UsernameNotFoundException("User not found with email: " + username);
    }

    /**
     * Deletes a user by their ID only if they do not have any associated bank accounts.
     * @param userId The ID of the user to delete.
     * @throws ResourceNotFoundException if the user does not exist.
     * @throws IllegalStateException if the user has associated bank accounts.
     */
    public void deleteUserWithoutBankAccount(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        logger.info("Attempting to delete user with ID {}.", userId);
        if (userOptional.isPresent()) {
            logger.info("User with ID {} found.", userId);
            User user = userOptional.get();
            List<Account> accounts = accountRepository.findAllByUserId(user.getId());
            if (accounts.isEmpty()) {
                userRepository.deleteById(userId);
                logger.info("User with ID {} deleted successfully.", userId);
            } else {
                //Throw Conflict status code and error message
                logger.warn("Cannot delete user with ID {}: User has associated bank accounts.", userId);
                throw new IllegalStateException("Cannot delete user with ID {}: User has associated bank accounts.");
            }
        } else {
            logger.warn("User with ID {} not found.", userId);
            throw new ResourceNotFoundException("User with ID " + userId + " not found.");
        }
    }
}

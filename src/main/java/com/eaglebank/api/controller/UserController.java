package com.eaglebank.api.controller;

import com.eaglebank.api.beans.User;
import com.eaglebank.api.exception.UserNotFoundException;
import com.eaglebank.api.repository.UserRepository;

import com.eaglebank.api.service.CustomUserDetailsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller to manage User resources.
 * All endpoints are prefixed with /v1/users.
 */
@RestController
@RequestMapping("/v1/users")
public class UserController {
    Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * Constructor injection of the UserRepository.
     */
    public UserController(UserRepository userRepository, CustomUserDetailsService customUserDetailsService) {
        this.userRepository = userRepository;
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Helper method to enforce self-access or ROLE_ADMIN authorization.
     * Throws AccessDeniedException if the authenticated user is neither the requested user nor an admin.
     * @param requestedId The ID from the path variable.
     * @param authentication The Spring Security Authentication object.
     * @throws UsernameNotFoundException If the authenticated user is not found in the database.
     * @throws AccessDeniedException If the authorization check fails.
     */
    private void enforceSelfOrAdminAccess(Long requestedId, Authentication authentication) {
        // 1. Get the email of the currently logged-in user (the principal name from the JWT)
        String loggedInEmail = authentication.getName();

        // 2. Find the authenticated user entity to get their internal ID
        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in DB: " + loggedInEmail));

        // 3. Check for authorization
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isSelf = loggedInUser.getId().equals(requestedId);

        if (!(isSelf || isAdmin)) {
            // Unauthorized: Access denied (maps to HTTP 403 Forbidden)
            throw new AccessDeniedException("You are not authorized to access/modify the details of user ID: " + requestedId);
        }
    }
    // --- READ Operations ---

    /**
     * GET /api/users
     * Retrieves all users from the database.
     */
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * GET /api/users/{id}
     * Retrieves a single user by their ID.
     * Enforces security: A user can only fetch their own details, unless they have ROLE_ADMIN.
     * @param id The ID of the user to retrieve.
     * @param authentication The Spring Security Authentication object automatically provided.
     * @return ResponseEntity with the User object or 404 NOT FOUND.
     * @throws AccessDeniedException if the user attempts to access another user's details without admin privileges.
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id, Authentication authentication) {

        // 1. Attempt to find the user first. If not found, throw UserNotFoundException
        // to be caught by the global handler (returning 404 with JSON body).
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        // 2. If the user exists, now enforce the authorization check.
        // This will correctly throw a 403 if the user is unauthorized to view this existing resource.
        enforceSelfOrAdminAccess(id, authentication);

        // 3. If authorized, return the user details (200 OK).
        return ResponseEntity.ok(user);
    }

    // --- CREATE Operation ---

    /**
     * POST /api/users
     * Creates a new user in the database.
     * @param user The User object sent in the request body (automatically mapped from JSON).
     * The @Valid annotation triggers bean validation (email, phone, etc.).
     * @return The newly created User object with its assigned ID.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Set response status to 201 CREATED
    public User createUser(@Valid @RequestBody User user) { // ADDED @Valid
        return userRepository.save(user);
    }

    // --- UPDATE Operation ---

    /**
     * PUT /api/users/{id}
     * Updates an existing user specified by the ID.
     * Enforces security: A user can only update their own details, unless they have ROLE_ADMIN.
     * @param id The ID of the user to update.
     * @param userDetails The updated user details (mapped from JSON).
     * @param authentication The Spring Security Authentication object automatically provided.
     * @return ResponseEntity with the updated User object or 404 NOT FOUND.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails, Authentication authentication) {
        // 1. Check for existence: If not found, throws UserNotFoundException (handled by advice as 404).
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        // 2. Enforce authorization check: If found but unauthorized, throws AccessDeniedException (handled by advice as 403).
        enforceSelfOrAdminAccess(id, authentication);

        // 3. If successful so far (Exists AND Authorized), perform update.
        // Update all fields of the existing user object
        existingUser.setName(userDetails.getName());
        existingUser.setAddress(userDetails.getAddress());
        existingUser.setPhoneNumber(userDetails.getPhoneNumber());
        existingUser.setEmail(userDetails.getEmail());

        // Save and return the updated entity
        User updatedUser = userRepository.save(existingUser);
        return ResponseEntity.ok(updatedUser);
    }

    // --- DELETE Operation ---
    //Given a user has successfully authenticated
    //When the user makes a `DELETE` request to the `/v1/users/{userId}` endpoint And they do not have a bank account
    //Then the system deletes their user record from the database
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteUserWithoutBankAccount(@PathVariable Long id, Authentication authentication) {

        // 1. Check for existence: If not found, throws UserNotFoundException (handled by advice as 404).
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        // Enforce authorization check
        enforceSelfOrAdminAccess(id, authentication);

        customUserDetailsService.deleteUserWithoutBankAccount(id);
        return ResponseEntity.noContent().<Void>build();
    }
}
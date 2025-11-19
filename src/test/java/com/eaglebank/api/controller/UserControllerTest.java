package com.eaglebank.api.controller;

import com.eaglebank.api.beans.Address;
import com.eaglebank.api.beans.User;
import com.eaglebank.api.exception.UserNotFoundException;
import com.eaglebank.api.repository.UserRepository;
import com.eaglebank.api.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;


@WebMvcTest(UserController.class)
@WithMockUser(username = "testuser", roles = {"USER"})
public class UserControllerTest {

    //Write test cases for UserController class
    @MockitoBean UserRepository userRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private User user2;

    private Address user1Address;

    @BeforeEach
    public void setUp() {

        user1Address = new Address("10 Downing St", "London", "ss", "dd", "gg", "hh");
        user1 = new User("Sourabh1", user1Address, "1234567890", "user1@example.com");
        user1.setId(1L);

        Address user2Address = new Address("7 Dove House", "London", "ss", "dd", "gg", "hh");
        user2 = new User("Sunny", user2Address, "1234567890", "user2@example.com");
        user2.setId(2L);
    }

    // --- Helper method for mocking the enforceSelfOrAdminAccess logic ---
    private void mockAuthUser(User authenticatedUser) {
        when(userRepository.findByEmail(authenticatedUser.getEmail())).thenReturn(Optional.of(authenticatedUser));
    }

    @Test
    public void testCreateUser_ValidRequest() throws Exception {
        // Use the same mock Address setup for the request body
        Address newAddress = new Address("10 Downing St", "London", "ss", "dd", "gg", "hh");
        // Create new User using the new constructor
        User newUser = new User("Sourabh2", newAddress, "1234567890", "user@example.com");

        // Setup the saved User with an ID (simulate the database save)
        User savedUser = new User("Sourabh2", newAddress, "1234567890", "user@example.com");
        savedUser.setId(3L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                        // Note: The ObjectMapper handles serializing the User object, including the Address field.
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated()) // Expect 201 CREATED
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.email").value("user@example.com"));

        verify(userRepository, times(1)).save(any(User.class));

    }

    @Test
    public void testCreateUser_RequiredDataMissing() throws Exception {
        // Use the same mock Address setup for the request body
        Address newAddress = new Address("10 Downing St", "London", "ss", "dd", "gg", "hh");
        // Create new User using the new constructor with missing required data (e.g., null name)
        User newUser = new User(null, newAddress, "1234567890", "user@example.com");

        mockMvc.perform(post("/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                        .andExpect(status().isBadRequest()); // Expect 400 Bad Request
        verify(userRepository, times(0)).save(any(User.class));

    }

    @Test
    void getUserById_whenSelfAccessingOwnUser_shouldReturnUser() throws Exception {
        // 1. Setup Self Authentication
        mockAuthUser(user1);

        // 2. Setup target user retrieval
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));

        // 3. Perform request as User 1 (ID 1) accessing User 1 (ID 1)
        mockMvc.perform(get("/v1/users/{id}", user1.getId())
                        .with(user("user1@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sourabh1"));

        verify(userRepository, times(1)).findById(user1.getId());
        verify(userRepository, times(1)).findByEmail(user1.getEmail());
    }

    @Test
    void getUserById_whenUnauthorizedAccess_shouldReturnForbidden() throws Exception {
        // 1. Setup Unauthorized User (User 2)
        mockAuthUser(user2); // Authenticated user is Sunny (ID 2)

        // 2. Setup target user retrieval (User 1)
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));

        // 3. Perform request as User 2 (ID 2) accessing User 1 (ID 1)
        mockMvc.perform(get("/v1/users/{id}", user1.getId())
                        .with(user("user2@example.com").authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden()); // Expect HTTP 403

        verify(userRepository, times(1)).findById(user1.getId());
        verify(userRepository, times(1)).findByEmail(user2.getEmail());
    }

    @Test
    void getUserById_whenUserNotFound_shouldReturnNotFound() throws Exception {
        Long nonExistentId = 999L;
        mockAuthUser(user1);
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/users/{id}", nonExistentId))
                .andExpect(status().isNotFound()); // Expect HTTP 404

        verify(userRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void updateUser_whenSelfUpdating_shouldReturnUpdatedUser() throws Exception {
        // 1. Setup Self Authentication
        mockAuthUser(user1);

        // 2. Setup target user retrieval
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));

        // 3. Setup updated user details
        User updateDetails = new User("SourabhModified", user1Address, "1234567890", "user1@example.com");

        // 4. Mock the save operation
        User updatedUser = user1;
        updatedUser.setAddress(updateDetails.getAddress());
        updatedUser.setName("SourabhModified");
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // 5. Perform request as User 1 updating User 1
        mockMvc.perform(patch("/v1/users/{id}", user1.getId())
                        .with(user("user1@example.com").authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isOk())
                // Verify the name was updated
                .andExpect(jsonPath("$.name").value("SourabhModified"));

        verify(userRepository, times(1)).findById(user1.getId());
        verify(userRepository, times(1)).findByEmail(user1.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_whenUnauthorizedUpdating_shouldReturnForbidden() throws Exception {
        // 1. Setup Unauthorized User (User 2)
        mockAuthUser(user2);

        // 2. Setup target user retrieval (User 1)
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));

        // 3. Perform request as User 2 (ID 2) attempting to update User 1 (ID 1)
        mockMvc.perform(patch("/v1/users/{id}", user1.getId())
                        .with(user("user2@example.com").authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden()); // Expect HTTP 403

        verify(userRepository, times(1)).findById(user1.getId());
        verify(userRepository, times(1)).findByEmail(user2.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_whenUserNotFound_shouldReturnNotFound() throws Exception {
        Long nonExistentId = 999L;

        // 1. Setup Admin Authentication
        mockAuthUser(user1);

        // 2. Setup target user retrieval to return empty
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // 3. Perform request as Admin updating non-existent user
        mockMvc.perform(patch("/v1/users/{id}", nonExistentId)
                        .with(user("user1@example.com").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound()); // Expect HTTP 404

        verify(userRepository, times(1)).findById(nonExistentId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_whenSelfDeleting_shouldReturnNoContent() throws Exception {
        // 1. Setup Self Authentication
        mockAuthUser(user1);
        // 2. Setup target user retrieval (User 1)
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        // 2. Mock the service call
        doNothing().when(customUserDetailsService).deleteUserWithoutBankAccount(user1.getId());

        // 3. Perform request as User 1 deleting User 1
        mockMvc.perform(delete("/v1/users/{id}", user1.getId())
                        .with(user("user1@example.com").authorities(new SimpleGrantedAuthority("ROLE_USER"))).with(csrf()))
                        .andExpect(status().isNoContent()); // Expect 204 No Content

        verify(userRepository, times(1)).findByEmail(user1.getEmail());
        verify(customUserDetailsService, times(1)).deleteUserWithoutBankAccount(user1.getId());
    }

    @Test
    void deleteUser_whenUnauthorizedDeleting_shouldReturnForbidden() throws Exception {
        // 1. Setup Unauthorized User (User 2)
        mockAuthUser(user2);
        // 2. Setup target user retrieval (User 1)
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        // 2. Perform request as User 2 (ID 2) attempting to delete User 1 (ID 1)
        mockMvc.perform(delete("/v1/users/{id}", user1.getId())
                        .with(user("user2@example.com").authorities(new SimpleGrantedAuthority("ROLE_USER"))).with(csrf()))
                .andExpect(status().isForbidden()); // Expect HTTP 403

        verify(userRepository, times(1)).findByEmail(user2.getEmail());
        verify(customUserDetailsService, never()).deleteUserWithoutBankAccount(any());
    }

    @Test
    void deleteUser_whenUserDoesNotExist_shouldReturnNotFound() throws Exception {
        Long nonExistentId = 999L;
        // 1. Setup Admin Authentication (for permission to delete anyone)
        mockAuthUser(user1);
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        // 2. Mock the CustomUserDetailsService to throw UserNotFoundException
        //    when attempting to delete the non-existent user.
        doThrow(new UserNotFoundException("User not found for deletion with ID: " + nonExistentId))
                .when(customUserDetailsService).deleteUserWithoutBankAccount(nonExistentId);

        // 3. Perform request as Admin deleting non-existent user
        mockMvc.perform(delete("/v1/users/{id}", nonExistentId)
                        .with(user("user1@example.com").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))).with(csrf()))
                .andExpect(status().isNotFound()); // Expect 404 NOT FOUND

        verify(userRepository, times(1)).findById(nonExistentId);
    }
}

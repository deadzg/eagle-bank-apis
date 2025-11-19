package com.eaglebank.api.beans;

/**
 * Data Transfer Object (DTO) for handling login requests.
 * Used to receive email and password from the client during authentication.
*/
public class LoginRequest {
    private String email; // Changed from username
    private String password;

    // --- Getters and Setters ---
    public String getEmail() { // Updated getter
        return email;
    }

    public String getPassword() {
        return password;
    }
}
package com.eaglebank.api.beans;

import com.eaglebank.api.helper.CaseInsensitiveAccountTypeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for the POST /v1/accounts request body.
 * Contains the user-provided details for the new account.
 */
public class CreateAccountRequest {

    @NotBlank(message = "Account name is required.")
    private String name;

    @NotNull(message = "Account type is required.")
    @JsonDeserialize(using = CaseInsensitiveAccountTypeDeserializer.class) // Apply custom deserializer
    private AccountType accountType; // Using the AccountType enum

    // Getters and Setters

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
}

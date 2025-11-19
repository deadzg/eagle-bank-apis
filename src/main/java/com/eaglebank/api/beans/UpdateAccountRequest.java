package com.eaglebank.api.beans;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for PATCH requests to update an existing bank account.
 * Requires both name and accountType for the update in this implementation.
 */
public record UpdateAccountRequest(
        @NotBlank(message = "Account name is required for update.")
        @Size(max = 100, message = "Account name must not exceed 100 characters.")
        String name,

        @NotBlank(message = "Account type is required.")
        // Added PERSONAL to the list of valid account types
        @Pattern(regexp = "SAVINGS|CHECKING|BUSINESS|PERSONAL", message = "Account type must be one of SAVINGS, CHECKING, BUSINESS, or PERSONAL.")
        String accountType
) {}

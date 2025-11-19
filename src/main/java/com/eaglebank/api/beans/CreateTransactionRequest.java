package com.eaglebank.api.beans;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Data Transfer Object for creating a new transaction.
 * Includes validation for amount, currency, and transaction type.
 */
public record CreateTransactionRequest(
        @NotNull(message = "Amount is required.")
        @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than zero.")
        @DecimalMax(value = "10000.00", inclusive = true, message = "Amount cannot exceed 10,000.00.") BigDecimal amount,

        @NotBlank(message = "Currency is required.")
        @Pattern(regexp = "GBP", message = "Only GBP currency is currently supported.")
        String currency,

        @NotBlank(message = "Transaction type is required.")
        @Pattern(regexp = "deposit|withdrawal", message = "Type must be 'deposit' or 'withdrawal'.")
        String type,

        String reference
) {}

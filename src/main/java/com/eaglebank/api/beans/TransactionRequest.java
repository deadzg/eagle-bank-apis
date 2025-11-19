package com.eaglebank.api.beans;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for creating a new transaction.
 * Maps to the required OpenAPI schema for transaction requests, including validation.
*/
public class TransactionRequest {

    // Required, Min 0.00, Max 10000.00
    @NotNull(message = "Amount is required.")
    @DecimalMin(value = "0.00", inclusive = true, message = "Amount must be at least 0.00.")
    @DecimalMax(value = "10000.00", inclusive = true, message = "Amount cannot exceed 10000.00.")
    private BigDecimal amount;

    // Required and restricted to "GBP"
    @NotNull(message = "Currency is required.")
    @Pattern(regexp = "GBP", message = "Currency must be 'GBP'.")
    private String currency;

    // Required and restricted to "deposit" or "withdrawal"
    @NotNull(message = "Transaction type is required.")
    @Pattern(regexp = "deposit|withdrawal", message = "Type must be 'deposit' or 'withdrawal'.")
    private String type;

    // Optional reference field
    private String reference;

    // Getters and Setters (omitted for brevity in this response, but fully implemented in the file)

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Helper method to safely convert the String type to the TransactionType enum.
     */
    public TransactionType toTransactionType() {
        return TransactionType.valueOf(this.type);
    }
}
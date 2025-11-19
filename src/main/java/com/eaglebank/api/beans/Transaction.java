package com.eaglebank.api.beans;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a transaction (e.g., deposit, withdrawal).
 */
public class Transaction {
    private String id; // Matches the required 'tan-' format
    private Long accountId; // The account this transaction belongs to
    private Long userId; // For quick lookup/security
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private String reference;
    private Instant createdTimestamp;

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Instant getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(Instant createdTimestamp) { this.createdTimestamp = createdTimestamp; }
}

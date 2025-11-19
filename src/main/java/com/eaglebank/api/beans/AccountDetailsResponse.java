package com.eaglebank.api.beans;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for the response body of a successful account creation or retrieval.
 * Includes all required account details.
 */
public class AccountDetailsResponse {

    private String accountNumber;
    private String sortCode;
    private String name;
    private AccountType accountType;
    private BigDecimal balance;
    private String currency;
    private Instant createdTimestamp;
    private Instant updatedTimestamp;

    // Static factory method to create DTO from Entity
    public static AccountDetailsResponse fromEntity(Account account) {
        AccountDetailsResponse dto = new AccountDetailsResponse();
        dto.setAccountNumber(account.getAccountNumber());
        dto.setSortCode(account.getSortCode());
        dto.setName(account.getName());
        dto.setAccountType(account.getAccountType());
        dto.setBalance(account.getBalance());
        dto.setCurrency(account.getCurrency());
        dto.setCreatedTimestamp(account.getCreatedTimestamp());
        dto.setUpdatedTimestamp(account.getUpdatedTimestamp());
        return dto;
    }

    // Getters and Setters

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getSortCode() { return sortCode; }
    public void setSortCode(String sortCode) { this.sortCode = sortCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Instant getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(Instant createdTimestamp) { this.createdTimestamp = createdTimestamp; }

    public Instant getUpdatedTimestamp() { return updatedTimestamp; }
    public void setUpdatedTimestamp(Instant updatedTimestamp) { this.updatedTimestamp = updatedTimestamp; }
}

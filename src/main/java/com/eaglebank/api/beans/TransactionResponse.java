package com.eaglebank.api.beans;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object for API responses containing transaction details.
 */
public record TransactionResponse(
        String id,
        BigDecimal amount,
        String currency,
        TransactionType type,
        String reference,
        Long userId,
        Instant createdTimestamp
) {
    public static TransactionResponse fromEntity(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getReference(),
                transaction.getUserId(),
                transaction.getCreatedTimestamp()
        );
    }
}

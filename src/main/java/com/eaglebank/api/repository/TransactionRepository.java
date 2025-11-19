package com.eaglebank.api.repository;


import com.eaglebank.api.beans.Transaction;
import org.springframework.stereotype.Repository;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dedicated, in-memory mock implementation for the TransactionRepository interface.
 * Handles persistence logic for Transaction entities.
 */
@Repository
public class TransactionRepository  {
    private final List<Transaction> transactions = new ArrayList<>();

    public Transaction save(Transaction transaction) {
        // Assign a unique String ID starting with 'tan-' if new or empty
        if (transaction.getId() == null || transaction.getId().isEmpty()) {
            // Generate a unique ID: "tan-" + UUID (with hyphens removed)
            String uniqueId = "tan-" + UUID.randomUUID().toString().replace("-", "");
            transaction.setId(uniqueId);
        }

        // In a real app, we'd check if the ID exists for an update.
        // For this simple in-memory implementation, we remove the existing entry
        // with the same ID and add the new one (effectively an upsert).
        this.transactions.removeIf(t -> t.getId().equals(transaction.getId()));
        this.transactions.add(transaction);

        return transaction;
    }

    /**
     * Finds all transactions for a specific internal account ID.
     * @param accountId The internal ID of the account.
     * @return A list of transactions, sorted by date descending.
     */

    public List<Transaction> findByAccountId(Long accountId) {
        List<Transaction> result = this.transactions.stream()
                .filter(t -> t.getAccountId().equals(accountId))
                .sorted((t1, t2) -> t2.getCreatedTimestamp().compareTo(t1.getCreatedTimestamp())) // Sort descending by date
                .collect(Collectors.toList());

        return Collections.unmodifiableList(result);
    }

    public Transaction findByIdAndAccountId(String transactionId,Long accountId) {
        Transaction transaction = this.transactions.stream().filter(t-> t.getId().equals(transactionId)).findFirst().orElse(null);

        if (transaction != null && transaction.getAccountId().equals(accountId)) {
            return transaction;
        }
        return null;
    }

    public List<Transaction> findAllByAccountId(Long accountId) {
        return transactions.stream()
                .filter(t -> t.getAccountId().equals(accountId))
                .collect(Collectors.toList());
    }
}
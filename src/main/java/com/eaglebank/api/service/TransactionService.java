package com.eaglebank.api.service;

import com.eaglebank.api.beans.Account;
import com.eaglebank.api.beans.Transaction;
import com.eaglebank.api.beans.TransactionRequest;
import com.eaglebank.api.beans.TransactionType;
import com.eaglebank.api.exception.ExceptionControllerAdvice;
import com.eaglebank.api.exception.ResourceNotFoundException;
import com.eaglebank.api.repository.AccountRepository;
import com.eaglebank.api.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service class for handling transaction-related operations.
 */
@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Retrieves the transaction history for a specified account, ensuring authorization
     * and existence checks pass.
     *
     * @param accountNumber The account number to retrieve history for.
     * @param userId The ID of the authenticated user.
     * @return A list of transactions for the account.
     * @throws ResourceNotFoundException if the account does not exist (404).
     * @throws ExceptionControllerAdvice.ForbiddenOperationException if the account does not belong to the user (403).
     */
    public List<Transaction> getTransactionHistory(String accountNumber, Long userId) {
        // 1. Find Account by accountNumber (Throws 404 Not Found if non-existent)
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        // 2. Check Authorization (Throws 403 Forbidden if not owned by user)
        if (!account.getUserId().equals(userId)) {
            throw new ExceptionControllerAdvice.ForbiddenOperationException("Access denied. Account does not belong to user.");
        }

        // 3. Fetch and return transaction history using the internal ID
        return transactionRepository.findByAccountId(account.getId());
    }

    /**
     * Processes a financial transaction request (deposit or withdrawal).
     * * @param accountNumber The account number (used as path ID) of the target account.
     * @param userId The ID of the authenticated user (for authorization).
     * @param request The validated transaction data.
     * @return The resulting Transaction record.
     * @throws RuntimeException if authorization fails, account is not found, or funds are insufficient.
     */
    @Transactional
    public Transaction processTransaction(String accountNumber, Long userId, TransactionRequest request) {

        // --- 1. Authorization & Existence Check (using Account Number) ---
        // Verify account exists and belongs to the authenticated user.
        Optional<Account> account = accountRepository.findByAccountNumberAndUserId(accountNumber, userId);
        if (account.isEmpty()) {
            // Throws a RuntimeException, which the controller will translate to an appropriate error response.
            throw new RuntimeException("Account not found or unauthorized for account number: " + accountNumber);
        }

        BigDecimal transactionAmount = request.getAmount();
        BigDecimal currentBalance = account.get().getBalance();
        BigDecimal newBalance;
        TransactionType type = request.toTransactionType();

        // --- 2. Transaction Logic (Deposit/Withdrawal) ---
        if (type == TransactionType.withdrawal) {

            // --- 3. Funds Check for Withdrawal ---
            if (currentBalance.compareTo(transactionAmount) < 0) {
                // Throws a RuntimeException for insufficient funds.
                throw new RuntimeException("Insufficient funds in account number: " + accountNumber + ". Current balance: " + currentBalance);
            }

            // Calculate new balance: current - amount
            newBalance = currentBalance.subtract(transactionAmount);

        } else if (type == TransactionType.deposit) {

            // Calculate new balance: current + amount
            newBalance = currentBalance.add(transactionAmount);

        } else {
            throw new RuntimeException("Invalid transaction type: " + type);
        }

        // --- 4. Update Account Balance ---
        accountRepository.updateBalance(account.get().getId(), newBalance);

        // --- 5. Register Transaction Record ---
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.get().getId());
        transaction.setCreatedTimestamp(Instant.now());
        transaction.setAmount(transactionAmount);
        transaction.setCurrency(request.getCurrency());
        transaction.setType(type);
        transaction.setReference(request.getReference());

        return transactionRepository.save(transaction);
    }

    /**
     * Retrieves a single transaction detail for a given account and transaction ID.
     * @param accountNumber The target account number.
     * @param transactionId The ID of the transaction to fetch.
     * @param userId The ID of the authenticated user (for authorization).
     * @return The Transaction record.
     * @throws ResourceNotFoundException if the account/transaction is not found or unauthorized.
     */
    public Transaction getTransactionDetails(String accountNumber, String transactionId, Long userId) {

        // 1. Authorization Check: Account must exist AND belong to the authenticated user.
        Optional<Account> account = accountRepository.findByAccountNumberAndUserId(accountNumber, userId);
        Optional<Account> accountByNumber = accountRepository.findByAccountNumber(accountNumber);

        if (account.isEmpty()) {
            if(accountByNumber.isPresent()) {
                // Account exists but does not belong to user - Throw 403 Forbidden
                throw new ExceptionControllerAdvice.ForbiddenOperationException("Access denied. Account does not belong to user.");
            }
            // Throw 404 if account is missing or unauthorized
            throw new ResourceNotFoundException("Account not found or access denied for account number: " + accountNumber);
        }

        // 2. Resource Check: Transaction must exist AND belong to the specified account.
        Transaction transaction = transactionRepository.findByIdAndAccountId(transactionId, account.get().getId());

        if (transaction == null) {
            // Throw 404 if transaction is missing or not associated with this account
            throw new ResourceNotFoundException("Transaction ID not found for account number: " + accountNumber);
        }

        if(transaction.getAccountId() != account.get().getId()) {
            // Transaction does not belong to the specified account - Throw 404 Forbidden
            throw new ResourceNotFoundException("Transaction ID not found for account number: " + accountNumber);
        }

        return transaction;
    }
}

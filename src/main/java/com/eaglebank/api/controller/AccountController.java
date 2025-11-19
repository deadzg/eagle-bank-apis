package com.eaglebank.api.controller;

import com.eaglebank.api.beans.Account;
import com.eaglebank.api.beans.AccountDetailsResponse;
import com.eaglebank.api.beans.AccountType;
import com.eaglebank.api.beans.CreateAccountRequest;
import com.eaglebank.api.beans.CreateTransactionRequest;
import com.eaglebank.api.beans.Transaction;
import com.eaglebank.api.beans.TransactionRequest;
import com.eaglebank.api.beans.TransactionResponse;
import com.eaglebank.api.beans.TransactionType;
import com.eaglebank.api.beans.UpdateAccountRequest;
import com.eaglebank.api.beans.User;
import com.eaglebank.api.exception.ResourceNotFoundException;
import com.eaglebank.api.repository.AccountRepository;
import com.eaglebank.api.repository.TransactionRepository;
import com.eaglebank.api.repository.UserRepository;
import com.eaglebank.api.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * REST Controller to manage Bank Account resources.
 * Handles the creation of new bank accounts for authenticated users.
 */
@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private static final Random RANDOM = new Random();

    public AccountController(AccountRepository accountRepository, UserRepository userRepository, TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }


    // --- Helper Methods (Account Generation) ---

    /**
     * Helper method to get the ID of the authenticated user.
     * @param authentication The Spring Security Authentication object.
     * @return The ID of the authenticated user.
     * @throws UsernameNotFoundException if the authenticated principal is not found in the UserRepository.
     */
    private Long getAuthenticatedUserId(Authentication authentication) {
        String loggedInEmail = authentication.getName();
        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in DB: " + loggedInEmail));
        return loggedInUser.getId();
    }
    /**
     * Helper method to generate a unique 8-digit account number (simulated).
     */
    private String generateUniqueAccountNumber() {
        // In a real system, this would involve a complex, transactional sequence
        // to ensure uniqueness and compliance. Here, we simulate a unique 8-digit number.
        // For simplicity and brevity, this mock generation does not guarantee true uniqueness
        // and skips retry logic, but simulates the format.
        return String.format("%08d", RANDOM.nextInt(100000000));
    }

    /**
     * Helper method to generate a unique 6-digit sort code (simulated).
     */
    private String generateSortCode() {
        // Simulates a 6-digit sort code, formatted as XX-XX-XX in banking systems
        return String.format("%02d-%02d-%02d", RANDOM.nextInt(100), RANDOM.nextInt(100), RANDOM.nextInt(100));
    }

    /**
     * POST /v1/accounts
     * Creates a new bank account for the authenticated user.
     * @param request The request DTO containing the account name and type.
     * @param authentication The Spring Security Authentication object (contains the principal/username).
     * @return ResponseEntity with the created AccountDetailsResponse and HTTP 201 Created.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AccountDetailsResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication) {

        // 1. Get the authenticated user's ID
        String loggedInEmail = authentication.getName();
        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in DB: " + loggedInEmail));

        Long userId = loggedInUser.getId();

        // 2. Prepare the new Account entity
        Instant now = Instant.now();
        Account newAccount = new Account();

        // Link to user
        newAccount.setUserId(userId);

        // Set user-provided details
        newAccount.setName(request.getName());
        newAccount.setAccountType(request.getAccountType());

        // Set simulated banking details
        newAccount.setAccountNumber(generateUniqueAccountNumber());
        newAccount.setSortCode(generateSortCode());
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setCurrency("GBP"); // Default currency

        // Set timestamps
        newAccount.setCreatedTimestamp(now);
        newAccount.setUpdatedTimestamp(now);

        // 3. Save the account
        Account savedAccount = accountRepository.save(newAccount);

        // 4. Map the saved entity to the response DTO and return
        AccountDetailsResponse responseDto = AccountDetailsResponse.fromEntity(savedAccount);

        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * GET /v1/accounts
     * Retrieves all bank accounts associated with the authenticated user.
     * @param authentication The Spring Security Authentication object (contains the principal/username).
     * @return A list of AccountDetailsResponse DTOs.
     */
    @GetMapping
    public List<AccountDetailsResponse> getAccounts(Authentication authentication) {
        // 1. Get the authenticated user's ID
        String loggedInEmail = authentication.getName();
        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in DB: " + loggedInEmail));

        Long userId = loggedInUser.getId();

        // 2. Fetch all accounts for that user ID
        List<Account> userAccounts = accountRepository.findAllByUserId(userId);

        // 3. Map the list of Account entities to a list of AccountDetailsResponse DTOs
        return userAccounts.stream()
                .map(AccountDetailsResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * GET /v1/accounts/{accountId}
     * Retrieves a single bank account associated with the authenticated user, or throws
     * an exception if the account is not found or not owned by the user.
     * @param accountId The ID of the account to retrieve.
     * @param authentication The Spring Security Authentication object.
     * @return The AccountDetailsResponse DTO.
     */
    @GetMapping("/{accountId}")
    public AccountDetailsResponse getAccountByNumber(@PathVariable("accountId") String accountNumber, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        // 1. Attempt to find the account by its account number AND the authenticated user's ID
        return accountRepository.findByAccountNumberAndUserId(accountNumber, userId)
                .map(AccountDetailsResponse::fromEntity) // If found and owned, map and return 200 OK
                .orElseGet(() -> {
                    // If the secure lookup failed, determine the appropriate error code.

                    // 2. Check if the account exists at all (regardless of owner)
                    if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
                        // The account exists, but the user ID didn't match the account owner.
                        // This is an Access Violation, resulting in a 403 Forbidden.
                        throw new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access to account number " + accountNumber + " is forbidden. Account exists but belongs to a different user."
                        );
                    } else {
                        // The account does not exist in the database at all.
                        // This is a Not Found error (404).
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Account with number " + accountNumber + " not found."
                        );
                    }
                });
    }

    /**
     * PATCH /v1/accounts/{accountId}
     * Updates an existing bank account for the authenticated user using the account number.
     * 1. Returns 404 Not Found if accountNumber doesn't exist.
     * 2. Returns 403 Forbidden if account is not associated with the userId.
     * 3. Updates name and accountType if authorized.
     */
    @PatchMapping("/{accountId}")
    public AccountDetailsResponse patchAccount(
            // Changed path variable from accountId (Long) to accountNumber (String)
            @PathVariable("accountId") String accountNumber,
            @Valid @RequestBody UpdateAccountRequest request,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);

        // 1. Find the account by Account Number and User ID
        Optional<Account> accountOptional = accountRepository.findByAccountNumberAndUserId(accountNumber, userId);

        if (accountOptional.isEmpty()) {
            // Account not found for this user. Determine 403 (Forbidden) or 404 (Not Found).
            if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
                // Account exists but belongs to someone else
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access to account number " + accountNumber + " is forbidden. Account exists but belongs to a different user."
                );
            } else {
                // Account does not exist at all
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account with number " + accountNumber + " not found."
                );
            }
        }

        // 2. Authorization passed, apply updates
        Account existingAccount = accountOptional.get();
        existingAccount.setName(request.name());
        // Convert request string to enum and set on the entity
        AccountType type = AccountType.valueOf(request.accountType());
        existingAccount.setAccountType(type);
        existingAccount.setUpdatedTimestamp(Instant.now());

        // 3. Save and return the updated DTO
        Account updatedAccount = accountRepository.save(existingAccount);

        return AccountDetailsResponse.fromEntity(updatedAccount);
    }

    /**
     * DELETE /v1/accounts/{accountId}
     * Deletes an existing bank account for the authenticated user using the account number.
     * Implements 404/403 security checks.
     */
    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 is standard for successful deletion
    public ResponseEntity<Void> deleteAccount(
            @PathVariable("accountId") String accountNumber,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);

        // 1. Find the account by Account Number and User ID
        Optional<Account> accountOptional = accountRepository.findByAccountNumberAndUserId(accountNumber, userId);

        if (accountOptional.isEmpty()) {
            // Account not found for this user. Determine 403 (Forbidden) or 404 (Not Found).
            if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
                // Account exists but belongs to someone else
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access to account number " + accountNumber + " is forbidden. Account exists but belongs to a different user."
                );
            } else {
                // Account does not exist at all
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account with number " + accountNumber + " not found."
                );
            }
        }

        // 2. Authorization passed, delete the account
        Account accountToDelete = accountOptional.get();
        accountRepository.delete(accountToDelete);

        // 3. Return 204 No Content
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint to create a new transaction (currently supporting only Withdrawal).
     * POST /v1/accounts/{accountNumber}/transactions
     * * The service layer handles:
     * - Existence (404 Not Found)
     * - Authorization (403 Forbidden)
     * - Funds check (422 Unprocessable Entity)
     * The @Valid annotation handles data validation (400 Bad Request).
     */
    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<Transaction> createTransaction(
            @PathVariable("accountId") String accountNumber, // Changed to String accountNumber
            @Valid @RequestBody TransactionRequest request,Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        // Only handle explicit Withdrawal requests for now

            Transaction transaction = transactionService.processTransaction(
                    accountNumber, // Pass account number
                   userId,
                    request
            );
            return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }


    /**
     * Endpoint to retrieve the transaction history for a specific account.
     * GET /v1/accounts/{accountNumber}/transactions
     * Handles 404 Not Found and 403 Forbidden via TransactionService.
     */
    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<List<Transaction>> getTransactionHistory(
            @PathVariable("accountId") String accountNumber,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        List<Transaction> history = transactionService.getTransactionHistory(
                accountNumber,
                userId
        );
        return ResponseEntity.ok(history);
    }

    /**
     * Handles GET /v1/accounts/{accountId}/transactions/{transactionId}
     * Retrieves details for a specific transaction, enforcing user ownership and transaction association.
     */
    @GetMapping("/{accountId}/transactions/{transactionId}")
    public ResponseEntity<Transaction> getTransactionDetails(
            @PathVariable("accountId") String accountNumber,
            @PathVariable("transactionId") String transactionId,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        Transaction transaction = transactionService.getTransactionDetails(
                accountNumber,
                transactionId,
                userId
        );
        return ResponseEntity.ok(transaction);
    }
}

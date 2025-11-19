package com.eaglebank.api.repository;

import com.eaglebank.api.beans.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for the Account entity, providing standard CRUD operations.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Custom method to check if an account number already exists
    Optional<Account> findByAccountNumber(String accountNumber);

    // Custom method to find all accounts associated with a specific user ID
    List<Account> findAllByUserId(Long userId);

    /**
     * Finds a specific account using its unique account number and the owning user's ID.
     * This is critical for security and ownership verification.
     */
    Optional<Account> findByAccountNumberAndUserId(String accountNumber, Long userId);

    /**
     * Updates the balance of an account identified by its primary key (ID).
     * * The @Modifying annotation is crucial, telling Spring Data JPA that this query
     * will alter the database state (UPDATE, DELETE). It returns the number of
     * rows affected.
     * * @param accountId The ID of the account to update (primary key).
     * @param newBalance The new balance value.
     * @return The number of entities updated.
     */
    @Modifying
    @Query("UPDATE Account a SET a.balance = :newBalance WHERE a.id = :accountId")
    int updateBalance(@Param("accountId") Long accountId, @Param("newBalance") BigDecimal newBalance);
}

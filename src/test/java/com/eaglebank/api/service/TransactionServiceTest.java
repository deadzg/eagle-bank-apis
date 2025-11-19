package com.eaglebank.api.service;

import com.eaglebank.api.beans.Account;
import com.eaglebank.api.beans.Transaction;
import com.eaglebank.api.beans.TransactionRequest;
import com.eaglebank.api.beans.TransactionType;
import com.eaglebank.api.exception.ExceptionControllerAdvice;
import com.eaglebank.api.exception.ResourceNotFoundException;
import com.eaglebank.api.repository.AccountRepository;
import com.eaglebank.api.repository.TransactionRepository;
import com.eaglebank.api.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionService
 */
@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(10L);
        account.setUserId(1L);
        account.setAccountNumber("ACC123");
        account.setBalance(BigDecimal.valueOf(200));
        account.setCurrency("GBP");
        Instant now = Instant.now();
        account.setCreatedTimestamp(now);
        account.setUpdatedTimestamp(now);
    }

    @Test
    void getTransactionHistory_accountExistsAndOwned_returnsHistory() {
        Transaction tx = new Transaction();
        tx.setId("tx-1");
        tx.setAccountId(account.getId());

        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountId(account.getId())).thenReturn(List.of(tx));

        List<Transaction> history = transactionService.getTransactionHistory("ACC123", 1L);

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("tx-1", history.get(0).getId());
    }

    @Test
    void getTransactionHistory_accountExistsButNotOwned_throwsForbidden() {
        Account other = new Account();
        other.setId(20L);
        other.setUserId(999L);
        other.setAccountNumber("ACC123");

        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(other));

        assertThrows(ExceptionControllerAdvice.ForbiddenOperationException.class,
                () -> transactionService.getTransactionHistory("ACC123", 1L));
    }

    @Test
    void getTransactionHistory_accountNotFound_throwsNotFound() {
        when(accountRepository.findByAccountNumber("MISSING")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.getTransactionHistory("MISSING", 1L));
    }

    @Test
    void processTransaction_withdrawal_success_updatesBalanceAndSavesTransaction() {
        TransactionRequest request = mock(TransactionRequest.class);
        when(request.getAmount()).thenReturn(BigDecimal.valueOf(100));
        when(request.toTransactionType()).thenReturn(TransactionType.withdrawal);
        when(request.getCurrency()).thenReturn("GBP");
        when(request.getReference()).thenReturn("ref-1");

        when(accountRepository.findByAccountNumberAndUserId("ACC123", 1L)).thenReturn(Optional.of(account));

        Transaction saved = new Transaction();
        saved.setId("tx-100");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        Transaction result = transactionService.processTransaction("ACC123", 1L, request);

        assertNotNull(result);
        assertEquals("tx-100", result.getId());

        // verify updateBalance called with new balance = 100 (200 - 100)
        ArgumentCaptor<BigDecimal> captor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(accountRepository, times(1)).updateBalance(eq(account.getId()), captor.capture());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(captor.getValue()));
    }

    @Test
    void processTransaction_withdrawal_insufficientFunds_throwsRuntime() {
        TransactionRequest request = mock(TransactionRequest.class);
        when(request.getAmount()).thenReturn(BigDecimal.valueOf(500)); // more than balance
        when(request.toTransactionType()).thenReturn(TransactionType.withdrawal);

        when(accountRepository.findByAccountNumberAndUserId("ACC123", 1L)).thenReturn(Optional.of(account));

        assertThrows(RuntimeException.class, () -> transactionService.processTransaction("ACC123", 1L, request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void processTransaction_deposit_success_updatesBalanceAndSavesTransaction() {
        TransactionRequest request = mock(TransactionRequest.class);
        when(request.getAmount()).thenReturn(BigDecimal.valueOf(50));
        when(request.toTransactionType()).thenReturn(TransactionType.deposit);
        when(request.getCurrency()).thenReturn("GBP");
        when(request.getReference()).thenReturn("dep-1");

        when(accountRepository.findByAccountNumberAndUserId("ACC123", 1L)).thenReturn(Optional.of(account));

        Transaction saved = new Transaction();
        saved.setId("tx-dep");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        Transaction result = transactionService.processTransaction("ACC123", 1L, request);

        assertNotNull(result);
        assertEquals("tx-dep", result.getId());

        // verify updateBalance called with new balance = 250 (200 + 50)
        ArgumentCaptor<BigDecimal> captor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(accountRepository, times(1)).updateBalance(eq(account.getId()), captor.capture());
        assertEquals(0, BigDecimal.valueOf(250).compareTo(captor.getValue()));
    }

    @Test
    void getTransactionDetails_success_returnsTransaction() {
        Transaction tx = new Transaction();
        tx.setId("tx-42");
        tx.setAccountId(account.getId());

        when(accountRepository.findByAccountNumberAndUserId("ACC123", 1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdAndAccountId("tx-42", account.getId())).thenReturn(tx);

        Transaction result = transactionService.getTransactionDetails("ACC123", "tx-42", 1L);

        assertNotNull(result);
        assertEquals("tx-42", result.getId());
    }

    @Test
    void getTransactionDetails_accountExistsButNotOwned_throwsForbidden() {
        when(accountRepository.findByAccountNumberAndUserId("ACC123", 1L)).thenReturn(Optional.empty());
        Account other = new Account();
        other.setAccountNumber("ACC123");
        other.setUserId(999L);
        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(other));

        assertThrows(ExceptionControllerAdvice.ForbiddenOperationException.class,
                () -> transactionService.getTransactionDetails("ACC123", "tx-1", 1L));
    }

    @Test
    void getTransactionDetails_accountNotFound_throwsNotFound() {
        when(accountRepository.findByAccountNumberAndUserId("MISSING", 1L)).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("MISSING")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.getTransactionDetails("MISSING", "tx-1", 1L));
    }

    @Test
    void getTransactionDetails_transactionMissing_throwsNotFound() {
        when(accountRepository.findByAccountNumberAndUserId("ACC123", 1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdAndAccountId("missing-tx", account.getId())).thenReturn(null);

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.getTransactionDetails("ACC123", "missing-tx", 1L));
    }
}
package com.eaglebank.api.controller;

import com.eaglebank.api.beans.Account;
import com.eaglebank.api.beans.AccountType;
import com.eaglebank.api.beans.Transaction;
import com.eaglebank.api.beans.User;
import com.eaglebank.api.repository.AccountRepository;
import com.eaglebank.api.repository.UserRepository;
import com.eaglebank.api.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(AccountController.class)
@WithMockUser(username = "testuser", roles = {"USER"})
public class AccountControllerTest {

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TransactionService transactionService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private Account account1;

    @BeforeEach
    public void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@example.com");
        user1.setName("User One");

        account1 = new Account();
        account1.setId(11L);
        account1.setUserId(user1.getId());
        account1.setAccountNumber("00001234");
        account1.setSortCode("12-34-56");
        account1.setName("Primary");
        account1.setAccountType(AccountType.CHECKING);
        account1.setBalance(BigDecimal.valueOf(1000));
        account1.setCurrency("GBP");
        Instant now = Instant.now();
        account1.setCreatedTimestamp(now);
        account1.setUpdatedTimestamp(now);
    }

    // --- Helper method for mocking the enforceSelfOrAdminAccess logic ---
    private void mockAuthUser(User authenticatedUser) {
        when(userRepository.findByEmail(authenticatedUser.getEmail())).thenReturn(Optional.of(authenticatedUser));
    }

    @Test
    public void testCreateAccount_ValidRequest() throws Exception {
        mockAuthUser(user1);

        Account savedAccount = new Account();
        savedAccount.setId(22L);
        savedAccount.setUserId(user1.getId());
        savedAccount.setAccountNumber("87654321");
        savedAccount.setSortCode("01-02-03");
        savedAccount.setName("My Savings");
        savedAccount.setAccountType(AccountType.SAVINGS);
        savedAccount.setBalance(BigDecimal.ZERO);
        savedAccount.setCurrency("GBP");
        Instant now = Instant.now();
        savedAccount.setCreatedTimestamp(now);
        savedAccount.setUpdatedTimestamp(now);

        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        String requestJson = "{\"name\":\"My Savings\",\"accountType\":\"SAVINGS\"}";

        mockMvc.perform(post("/v1/accounts")
                        .with(user(user1.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("87654321"))
                .andExpect(jsonPath("$.name").value("My Savings"));

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(userRepository, times(1)).findByEmail(user1.getEmail());
    }

    @Test
    public void testCreateAccount_MissingName_BadRequest() throws Exception {
        mockAuthUser(user1);

        // Missing name field
        String requestJson = "{\"accountType\":\"SAVINGS\"}";

        mockMvc.perform(post("/v1/accounts")
                        .with(user(user1.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    public void testGetAccounts_ReturnsList() throws Exception {
        mockAuthUser(user1);

        when(accountRepository.findAllByUserId(user1.getId()))
                .thenReturn(List.of(account1));

        mockMvc.perform(get("/v1/accounts")
                        .with(user(user1.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Primary"))
                .andExpect(jsonPath("$[0].accountNumber").value("00001234"));

        verify(accountRepository, times(1)).findAllByUserId(user1.getId());
        verify(userRepository, times(1)).findByEmail(user1.getEmail());
    }

    @Test
    public void testGetAccountByNumber_Success() throws Exception {
        mockAuthUser(user1);

        when(accountRepository.findByAccountNumberAndUserId(account1.getAccountNumber(), user1.getId()))
                .thenReturn(Optional.of(account1));

        mockMvc.perform(get("/v1/accounts/{accountId}", account1.getAccountNumber())
                        .with(user(user1.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(account1.getAccountNumber()))
                .andExpect(jsonPath("$.name").value("Primary"));

        verify(accountRepository, times(1)).findByAccountNumberAndUserId(account1.getAccountNumber(), user1.getId());
    }

    @Test
    public void testGetAccountByNumber_ForbiddenWhenOwnedByAnother() throws Exception {
        mockAuthUser(user1);

        // Simulate lookup by account number and userId returns empty (not owned)
        when(accountRepository.findByAccountNumberAndUserId(account1.getAccountNumber(), user1.getId()))
                .thenReturn(Optional.empty());

        // But the account exists belonging to someone else
        Account other = new Account();
        other.setAccountNumber(account1.getAccountNumber());
        other.setUserId(999L);
        when(accountRepository.findByAccountNumber(account1.getAccountNumber()))
                .thenReturn(Optional.of(other));

        mockMvc.perform(get("/v1/accounts/{accountId}", account1.getAccountNumber())
                        .with(user(user1.getEmail())))
                .andExpect(status().isForbidden());

        verify(accountRepository, times(1)).findByAccountNumberAndUserId(account1.getAccountNumber(), user1.getId());
        verify(accountRepository, times(1)).findByAccountNumber(account1.getAccountNumber());
    }

    @Test
    public void testGetAccountByNumber_NotFound() throws Exception {
        mockAuthUser(user1);

        when(accountRepository.findByAccountNumberAndUserId("NONEXIST", user1.getId()))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("NONEXIST")).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/accounts/{accountId}", "NONEXIST")
                        .with(user(user1.getEmail())))
                .andExpect(status().isNotFound());

        verify(accountRepository, times(1)).findByAccountNumber("NONEXIST");
    }

    @Test
    public void testPatchAccount_Success() throws Exception {
        mockAuthUser(user1);

        when(accountRepository.findByAccountNumberAndUserId(account1.getAccountNumber(), user1.getId()))
                .thenReturn(Optional.of(account1));

        Account updated = new Account();
        updated.setId(account1.getId());
        updated.setUserId(user1.getId());
        updated.setAccountNumber(account1.getAccountNumber());
        updated.setSortCode(account1.getSortCode());
        updated.setName("Updated Name");
        updated.setAccountType(AccountType.SAVINGS);
        updated.setBalance(account1.getBalance());
        updated.setCurrency(account1.getCurrency());
        Instant now = Instant.now();
        updated.setCreatedTimestamp(account1.getCreatedTimestamp());
        updated.setUpdatedTimestamp(now);

        when(accountRepository.save(any(Account.class))).thenReturn(updated);

        String requestJson = "{\"name\":\"Updated Name\",\"accountType\":\"SAVINGS\"}";

        mockMvc.perform(patch("/v1/accounts/{accountId}", account1.getAccountNumber())
                        .with(user(user1.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.accountNumber").value(account1.getAccountNumber()));

        verify(accountRepository, times(1)).findByAccountNumberAndUserId(account1.getAccountNumber(), user1.getId());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    public void testPatchAccount_Forbidden() throws Exception {
        mockAuthUser(user1);

        // Account exists but belongs to another user
        when(accountRepository.findByAccountNumberAndUserId(account1.getAccountNumber(), user1.getId()))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber(account1.getAccountNumber()))
                .thenReturn(Optional.of(account1)); // exists but different owner

        mockMvc.perform(patch("/v1/accounts/{accountId}", account1.getAccountNumber())
                        .with(user(user1.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"accountType\":\"SAVINGS\"}"))
                .andExpect(status().isForbidden());

        verify(accountRepository, times(1)).findByAccountNumber(account1.getAccountNumber());
    }

    @Test
    public void testDeleteAccount_Success() throws Exception {
        mockAuthUser(user1);

        when(accountRepository.findByAccountNumberAndUserId(account1.getAccountNumber(), user1.getId()))
                .thenReturn(Optional.of(account1));

        doNothing().when(accountRepository).delete(account1);

        mockMvc.perform(delete("/v1/accounts/{accountId}", account1.getAccountNumber())
                        .with(user(user1.getEmail()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(accountRepository, times(1)).delete(account1);
    }

    @Test
    public void testDeleteAccount_Forbidden() throws Exception {
        mockAuthUser(user1);

        when(accountRepository.findByAccountNumberAndUserId(account1.getAccountNumber(), user1.getId()))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber(account1.getAccountNumber()))
                .thenReturn(Optional.of(account1)); // belongs to another

        mockMvc.perform(delete("/v1/accounts/{accountId}", account1.getAccountNumber())
                        .with(user(user1.getEmail()))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(accountRepository, times(0)).delete(any());
    }

    @Test
    public void testCreateTransaction_Success() throws Exception {
        mockAuthUser(user1);

        Transaction tx = new Transaction();
        tx.setId("tx-123");
        // other fields can be set if needed

        when(transactionService.processTransaction(any(String.class), any(Long.class), any()))
                .thenReturn(tx);

        String requestJson = "{\"type\":\"withdrawal\",\"amount\":100.00, \"currency\": \"GBP\",\"reference\": \"Test\"}";

        mockMvc.perform(post("/v1/accounts/{accountId}/transactions", account1.getAccountNumber())
                        .with(user(user1.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        verify(transactionService, times(1)).processTransaction(any(String.class), any(Long.class), any());
    }

    @Test
    public void testGetTransactionHistory_Success() throws Exception {
        mockAuthUser(user1);

        Transaction tx = new Transaction();
        tx.setId("tx-1");

        when(transactionService.getTransactionHistory(account1.getAccountNumber(), user1.getId()))
                .thenReturn(List.of(tx));

        mockMvc.perform(get("/v1/accounts/{accountId}/transactions", account1.getAccountNumber())
                        .with(user(user1.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("tx-1"));

        verify(transactionService, times(1)).getTransactionHistory(account1.getAccountNumber(), user1.getId());
    }

    @Test
    public void testGetTransactionDetails_Success() throws Exception {
        mockAuthUser(user1);

        Transaction tx = new Transaction();
        tx.setId("tx-99");

        when(transactionService.getTransactionDetails(account1.getAccountNumber(), "tx-99", user1.getId()))
                .thenReturn(tx);

        mockMvc.perform(get("/v1/accounts/{accountId}/transactions/{transactionId}", account1.getAccountNumber(), "tx-99")
                        .with(user(user1.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tx-99"));

        verify(transactionService, times(1)).getTransactionDetails(account1.getAccountNumber(), "tx-99", user1.getId());
    }

}

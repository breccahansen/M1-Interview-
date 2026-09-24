package com.westlake.advisor.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class AccountRepositoryTest {

    private final AccountRepository repo = new AccountRepository();

    @Test
    public void seededAccountsAreLoaded() {
        assertTrue(repo.findByAccountNumber("7781-2204").isPresent());
        assertTrue(repo.findByAccountNumber("7781-9930").isPresent());
        assertFalse(repo.findByAccountNumber("0000-0000").isPresent());
    }

    @Test
    public void positionQuantitySumsLots() {
        Account a = repo.findByAccountNumber("7781-2204").get();
        assertEquals(new BigDecimal("133"), a.findPosition("AAPL").getQuantity());
    }
}

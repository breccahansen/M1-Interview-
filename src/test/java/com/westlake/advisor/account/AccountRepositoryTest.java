package com.westlake.advisor.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AccountRepositoryTest {

    private final AccountRepository repo = new AccountRepository();

    @Test
    void seededAccountsAreLoaded() {
        assertTrue(repo.findByAccountNumber("7781-2204").isPresent());
        assertTrue(repo.findByAccountNumber("7781-9930").isPresent());
        assertTrue(repo.findByAccountNumber("7782-0017").isPresent());
        assertFalse(repo.findByAccountNumber("0000-0000").isPresent());
    }

    @Test
    void seededAccountMetadata() {
        Account a = repo.findByAccountNumber("7781-2204").get();
        assertEquals("ADV-114", a.getAdvisorId());
        assertEquals(AccountType.INDIVIDUAL_BROKERAGE, a.getType());
        assertEquals(AccountStatus.OPEN, a.getStatus());
        assertEquals(3, a.getPositions().size());

        Account c = repo.findByAccountNumber("7782-0017").get();
        assertEquals("ADV-207", c.getAdvisorId());
        assertEquals(AccountType.TRUST, c.getType());
        assertTrue(c.getPositions().isEmpty());
    }

    @Test
    void positionQuantitySumsLots() {
        Account a = repo.findByAccountNumber("7781-2204").get();
        assertEquals(new BigDecimal("133"), a.findPosition("AAPL").getQuantity());
        assertEquals(new BigDecimal("212.5"), a.findPosition("VTI").getQuantity());
    }

    @Test
    void saveReplacesExistingAccount() {
        Account replacement = new Account("7781-9930", "ADV-999", AccountType.JOINT_BROKERAGE);
        assertSame(replacement, repo.save(replacement));
        assertSame(replacement, repo.findByAccountNumber("7781-9930").get());
    }

    @Test
    void findPositionIsCaseInsensitiveAndNullWhenAbsent() {
        Account a = repo.findByAccountNumber("7781-2204").get();
        assertEquals("AAPL", a.findPosition("aapl").getSymbol());
        assertNull(a.findPosition("TSLA"));
    }

    @Test
    void removePositionDropsItAndPositionsViewIsReadOnly() {
        Account a = repo.findByAccountNumber("7781-2204").get();
        Position aapl = a.findPosition("AAPL");
        a.removePosition(aapl);
        assertNull(a.findPosition("AAPL"));
        assertEquals(2, a.getPositions().size());
        assertThrows(UnsupportedOperationException.class, () -> a.getPositions().add(aapl));
    }

    @Test
    void statusCanBeChanged() {
        Account a = repo.findByAccountNumber("7781-9930").get();
        a.setStatus(AccountStatus.RESTRICTED);
        assertEquals(AccountStatus.RESTRICTED, repo.findByAccountNumber("7781-9930").get().getStatus());
    }
}

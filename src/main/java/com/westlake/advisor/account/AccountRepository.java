package com.westlake.advisor.account;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.westlake.advisor.costbasis.TaxLot;

/**
 * In-memory stand-in for the AccountMaster DB2 tables. Seeded with a handful of
 * accounts so the service can run without the mainframe gateway.
 */
@Repository
public class AccountRepository {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public AccountRepository() {
        seed();
    }

    public Optional<Account> findByAccountNumber(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber));
    }

    public Account save(Account account) {
        accounts.put(account.getAccountNumber(), account);
        return account;
    }

    private void seed() {
        Account a = new Account("7781-2204", "ADV-114", AccountType.INDIVIDUAL_BROKERAGE);
        Position aapl = new Position("AAPL");
        aapl.addLot(new TaxLot("L1", new BigDecimal("100"), new BigDecimal("143.27"), LocalDate.of(2021, 3, 12)));
        aapl.addLot(new TaxLot("L2", new BigDecimal("33"), new BigDecimal("171.13"), LocalDate.of(2023, 8, 2)));
        a.addPosition(aapl);
        Position vti = new Position("VTI");
        vti.addLot(new TaxLot("L3", new BigDecimal("212.5"), new BigDecimal("219.34"), LocalDate.of(2020, 11, 30)));
        a.addPosition(vti);
        Position vea = new Position("VEA");
        vea.addLot(new TaxLot("L5", new BigDecimal("33"), new BigDecimal("50.135"), LocalDate.of(2024, 5, 9)));
        a.addPosition(vea);
        save(a);

        Account b = new Account("7781-9930", "ADV-114", AccountType.ROTH_IRA);
        Position msft = new Position("MSFT");
        msft.addLot(new TaxLot("L4", new BigDecimal("40"), new BigDecimal("301.85"), LocalDate.of(2022, 1, 18)));
        b.addPosition(msft);
        save(b);

        Account c = new Account("7782-0017", "ADV-207", AccountType.TRUST);
        save(c);
    }
}

package com.westlake.advisor.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.westlake.advisor.account.Account;
import com.westlake.advisor.account.AccountRepository;
import com.westlake.advisor.account.AccountStatus;
import com.westlake.advisor.account.AccountType;
import com.westlake.advisor.account.Position;
import com.westlake.advisor.costbasis.CostBasisCalculator;
import com.westlake.advisor.costbasis.CostBasisMethod;
import com.westlake.advisor.costbasis.InsufficientSharesException;
import com.westlake.advisor.costbasis.TaxLot;
import com.westlake.advisor.notification.AdvisorNotifier;

class TransferServiceTest {

    private static final String FROM = "1000-0001";
    private static final String TO = "1000-0002";
    private static final String ADVISOR = "ADV-900";

    /** Records outbound notifications instead of logging them. */
    static class RecordingNotifier extends AdvisorNotifier {
        final List<String> calls = new ArrayList<>();

        @Override
        public void transferCompleted(String advisorId, String transferId, String symbol, BigDecimal quantity) {
            calls.add(advisorId + "|" + transferId + "|" + symbol + "|" + quantity);
        }
    }

    private AccountRepository repo;
    private RecordingNotifier notifier;
    private TransferService service;
    private Account from;
    private Account to;
    private TaxLot l1;
    private TaxLot l2;

    @BeforeEach
    void setUp() {
        repo = new AccountRepository();
        notifier = new RecordingNotifier();
        service = new TransferService(repo, new CostBasisCalculator(), notifier);

        from = new Account(FROM, ADVISOR, AccountType.INDIVIDUAL_BROKERAGE);
        Position aapl = new Position("AAPL");
        l1 = new TaxLot("L1", new BigDecimal("100"), new BigDecimal("143.27"), LocalDate.of(2021, 3, 12));
        l2 = new TaxLot("L2", new BigDecimal("33"), new BigDecimal("171.13"), LocalDate.of(2023, 8, 2));
        aapl.addLot(l1);
        aapl.addLot(l2);
        from.addPosition(aapl);
        repo.save(from);

        to = new Account(TO, ADVISOR, AccountType.TRUST);
        repo.save(to);
    }

    private TransferRequest request(String symbol, String qty, CostBasisMethod method) {
        TransferRequest r = new TransferRequest();
        r.setFromAccount(FROM);
        r.setToAccount(TO);
        r.setSymbol(symbol);
        r.setQuantity(new BigDecimal(qty));
        r.setMethod(method);
        return r;
    }

    // ---------------------------------------------------------------- happy path

    @Test
    void fifoTransferRelievesOldestLotPartiallyAndCreatesDashTLot() {
        TransferResult result = service.transfer(request("AAPL", "40", CostBasisMethod.FIFO));

        assertEquals("AAPL", result.getSymbol());
        assertEquals(new BigDecimal("40"), result.getQuantity());
        // 40 x 143.27 = 5730.80
        assertEquals(new BigDecimal("5730.80"), result.getCostBasisTransferred());
        assertEquals(Arrays.asList("L1-T"), result.getLotIds());
        assertTrue(result.getTransferId().matches("TRF-\\d{4}-[0-9A-F]{8}"), result.getTransferId());

        // partial relief: source lot shrinks but stays; L2 untouched
        Position source = from.findPosition("AAPL");
        assertEquals(2, source.getLots().size());
        assertEquals(new BigDecimal("60"), l1.getQuantity());
        assertEquals(new BigDecimal("33"), l2.getQuantity());
        assertEquals(new BigDecimal("93"), source.getQuantity());

        // destination gets a new position with the "-T" lot carrying cost and date
        Position dest = to.findPosition("AAPL");
        assertNotNull(dest);
        assertEquals(1, dest.getLots().size());
        TaxLot moved = dest.getLots().get(0);
        assertEquals("L1-T", moved.getLotId());
        assertEquals(new BigDecimal("40"), moved.getQuantity());
        assertEquals(new BigDecimal("143.27"), moved.getUnitCost());
        assertEquals(LocalDate.of(2021, 3, 12), moved.getAcquiredOn());

        assertEquals(1, notifier.calls.size());
        assertEquals(ADVISOR + "|" + result.getTransferId() + "|AAPL|40", notifier.calls.get(0));
    }

    @Test
    void fullReliefOfOneLotRemovesThatLotButKeepsPosition() {
        TransferResult result = service.transfer(request("AAPL", "100", CostBasisMethod.FIFO));

        assertEquals(new BigDecimal("14327.00"), result.getCostBasisTransferred());
        assertEquals(Arrays.asList("L1-T"), result.getLotIds());

        Position source = from.findPosition("AAPL");
        assertNotNull(source);
        assertEquals(1, source.getLots().size());
        assertSame(l2, source.getLots().get(0));
        assertEquals(new BigDecimal("33"), source.getQuantity());
    }

    @Test
    void spanningTwoLotsRemovesExhaustedLotAndPartiallyRelievesNext() {
        TransferResult result = service.transfer(request("AAPL", "120", CostBasisMethod.FIFO));

        // 100 x 143.27 + 20 x 171.13 = 14327.00 + 3422.60
        assertEquals(new BigDecimal("17749.60"), result.getCostBasisTransferred());
        assertEquals(Arrays.asList("L1-T", "L2-T"), result.getLotIds());

        Position source = from.findPosition("AAPL");
        assertEquals(1, source.getLots().size());
        assertEquals(new BigDecimal("13"), l2.getQuantity());

        Position dest = to.findPosition("AAPL");
        assertEquals(2, dest.getLots().size());
        assertEquals(new BigDecimal("120"), dest.getQuantity());
        assertEquals(new BigDecimal("171.13"), dest.getLots().get(1).getUnitCost());
        assertEquals(LocalDate.of(2023, 8, 2), dest.getLots().get(1).getAcquiredOn());
    }

    @Test
    void fullReliefOfAllLotsRemovesPositionFromSource() {
        TransferResult result = service.transfer(request("AAPL", "133", CostBasisMethod.FIFO));

        assertEquals(new BigDecimal("19974.29"), result.getCostBasisTransferred());
        assertNull(from.findPosition("AAPL"));
        assertTrue(from.getPositions().isEmpty());
        assertEquals(new BigDecimal("133"), to.findPosition("AAPL").getQuantity());
    }

    @Test
    void lifoTransferRelievesNewestLotFirst() {
        TransferResult result = service.transfer(request("AAPL", "10", CostBasisMethod.LIFO));

        // 10 x 171.13
        assertEquals(new BigDecimal("1711.30"), result.getCostBasisTransferred());
        assertEquals(Arrays.asList("L2-T"), result.getLotIds());
        assertEquals(new BigDecimal("100"), l1.getQuantity());
        assertEquals(new BigDecimal("23"), l2.getQuantity());
    }

    @Test
    void transferIntoExistingDestinationPositionAppendsLots() {
        Position existing = new Position("aapl");
        TaxLot l9 = new TaxLot("L9", new BigDecimal("5"), new BigDecimal("100.00"), LocalDate.of(2020, 1, 1));
        existing.addLot(l9);
        to.addPosition(existing);

        service.transfer(request("AAPL", "10", CostBasisMethod.FIFO));

        assertEquals(1, to.getPositions().size());
        assertSame(existing, to.findPosition("AAPL"));
        assertEquals(2, existing.getLots().size());
        assertSame(l9, existing.getLots().get(0));
        assertEquals("L1-T", existing.getLots().get(1).getLotId());
        assertEquals(new BigDecimal("15"), existing.getQuantity());
    }

    @Test
    void transferredBasisIsRoundedHalfUpToCents() {
        Position vea = new Position("VEA");
        vea.addLot(new TaxLot("L5", new BigDecimal("33"), new BigDecimal("50.135"), LocalDate.of(2024, 5, 9)));
        from.addPosition(vea);

        TransferResult result = service.transfer(request("VEA", "33", CostBasisMethod.FIFO));

        // 33 x 50.135 = 1654.455 -> 1654.46
        assertEquals(new BigDecimal("1654.46"), result.getCostBasisTransferred());
        assertEquals(2, result.getCostBasisTransferred().scale());
    }

    @Test
    void destinationMayBeRestrictedOrTransferPending() {
        to.setStatus(AccountStatus.RESTRICTED);
        assertEquals(new BigDecimal("1"), service.transfer(request("AAPL", "1", CostBasisMethod.FIFO)).getQuantity());

        to.setStatus(AccountStatus.TRANSFER_PENDING);
        assertEquals(new BigDecimal("1"), service.transfer(request("AAPL", "1", CostBasisMethod.FIFO)).getQuantity());

        assertEquals(new BigDecimal("2"), to.findPosition("AAPL").getQuantity());
    }

    @Test
    void savesBothAccountsBackToRepository() {
        List<String> saved = new ArrayList<>();
        AccountRepository spying = new AccountRepository() {
            @Override
            public Account save(Account account) {
                saved.add(account.getAccountNumber());
                return super.save(account);
            }
        };
        spying.save(from);
        spying.save(to);
        saved.clear();

        new TransferService(spying, new CostBasisCalculator(), notifier)
                .transfer(request("AAPL", "1", CostBasisMethod.FIFO));

        assertEquals(Arrays.asList(FROM, TO), saved);
    }

    // ---------------------------------------------------------------- rejections

    @Test
    void unknownSourceAccountIsRejected() {
        TransferRequest r = request("AAPL", "1", CostBasisMethod.FIFO);
        r.setFromAccount("0000-0000");

        AccountNotFoundException e = assertThrows(AccountNotFoundException.class, () -> service.transfer(r));
        assertEquals("Account not found: 0000-0000", e.getMessage());
        assertNoSideEffects();
    }

    @Test
    void unknownDestinationAccountIsRejected() {
        TransferRequest r = request("AAPL", "1", CostBasisMethod.FIFO);
        r.setToAccount("9999-9999");

        AccountNotFoundException e = assertThrows(AccountNotFoundException.class, () -> service.transfer(r));
        assertEquals("Account not found: 9999-9999", e.getMessage());
        assertNoSideEffects();
    }

    @Test
    void restrictedSourceAccountIsRejected() {
        from.setStatus(AccountStatus.RESTRICTED);

        TransferNotAllowedException e = assertThrows(TransferNotAllowedException.class,
                () -> service.transfer(request("AAPL", "1", CostBasisMethod.FIFO)));
        assertEquals("Source account 1000-0001 is RESTRICTED", e.getMessage());
        assertNoSideEffects();
    }

    @Test
    void closedSourceAccountIsRejected() {
        from.setStatus(AccountStatus.CLOSED);

        TransferNotAllowedException e = assertThrows(TransferNotAllowedException.class,
                () -> service.transfer(request("AAPL", "1", CostBasisMethod.FIFO)));
        assertEquals("Source account 1000-0001 is CLOSED", e.getMessage());
        assertNoSideEffects();
    }

    @Test
    void transferPendingSourceAccountIsRejected() {
        from.setStatus(AccountStatus.TRANSFER_PENDING);

        TransferNotAllowedException e = assertThrows(TransferNotAllowedException.class,
                () -> service.transfer(request("AAPL", "1", CostBasisMethod.FIFO)));
        assertEquals("Source account 1000-0001 is TRANSFER_PENDING", e.getMessage());
        assertNoSideEffects();
    }

    @Test
    void closedDestinationAccountIsRejected() {
        to.setStatus(AccountStatus.CLOSED);

        TransferNotAllowedException e = assertThrows(TransferNotAllowedException.class,
                () -> service.transfer(request("AAPL", "1", CostBasisMethod.FIFO)));
        assertEquals("Destination account 1000-0002 is closed", e.getMessage());
        assertNoSideEffects();
    }

    @Test
    void crossAdvisorTransferIsRejected() {
        Account other = new Account("2000-0001", "ADV-207", AccountType.TRUST);
        repo.save(other);
        TransferRequest r = request("AAPL", "1", CostBasisMethod.FIFO);
        r.setToAccount("2000-0001");

        TransferNotAllowedException e = assertThrows(TransferNotAllowedException.class, () -> service.transfer(r));
        assertEquals("Cross-advisor transfers require ACATS workflow", e.getMessage());
        assertNoSideEffects();
        assertTrue(other.getPositions().isEmpty());
    }

    @Test
    void sourceStatusIsCheckedBeforeDestinationAndAdvisor() {
        from.setStatus(AccountStatus.CLOSED);
        to.setStatus(AccountStatus.CLOSED);

        TransferNotAllowedException e = assertThrows(TransferNotAllowedException.class,
                () -> service.transfer(request("AAPL", "1", CostBasisMethod.FIFO)));
        assertEquals("Source account 1000-0001 is CLOSED", e.getMessage());
    }

    @Test
    void unknownSymbolIsRejected() {
        TransferNotAllowedException e = assertThrows(TransferNotAllowedException.class,
                () -> service.transfer(request("TSLA", "1", CostBasisMethod.FIFO)));
        assertEquals("Account 1000-0001 holds no TSLA", e.getMessage());
        assertNoSideEffects();
    }

    @Test
    void insufficientSharesIsRejectedWithoutMutatingLots() {
        InsufficientSharesException e = assertThrows(InsufficientSharesException.class,
                () -> service.transfer(request("AAPL", "134", CostBasisMethod.FIFO)));
        assertEquals("Insufficient shares of AAPL: requested 134, available 133", e.getMessage());
        assertNoSideEffects();
    }

    private void assertNoSideEffects() {
        assertEquals(new BigDecimal("100"), l1.getQuantity());
        assertEquals(new BigDecimal("33"), l2.getQuantity());
        assertEquals(2, from.findPosition("AAPL").getLots().size());
        assertNull(to.findPosition("AAPL"));
        assertTrue(notifier.calls.isEmpty());
    }
}

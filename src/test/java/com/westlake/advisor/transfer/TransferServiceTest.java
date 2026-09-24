package com.westlake.advisor.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

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

public class TransferServiceTest {

    private static final String FROM = "1000-0001";
    private static final String TO = "1000-0002";
    private static final String OTHER_ADVISOR = "1000-0003";
    private static final String ADVISOR = "ADV-900";

    private AccountRepository repo;
    private TransferService service;
    private Account from;
    private Account to;

    @BeforeEach
    public void setUp() {
        repo = new AccountRepository();
        service = new TransferService(repo, new CostBasisCalculator(), new AdvisorNotifier());

        from = new Account(FROM, ADVISOR, AccountType.INDIVIDUAL_BROKERAGE);
        Position aapl = new Position("AAPL");
        aapl.addLot(new TaxLot("L1", new BigDecimal("100"), new BigDecimal("143.27"), LocalDate.of(2021, 3, 12)));
        aapl.addLot(new TaxLot("L2", new BigDecimal("33"), new BigDecimal("171.13"), LocalDate.of(2023, 8, 2)));
        from.addPosition(aapl);
        Position vea = new Position("VEA");
        vea.addLot(new TaxLot("L5", new BigDecimal("33"), new BigDecimal("50.135"), LocalDate.of(2024, 5, 9)));
        from.addPosition(vea);
        repo.save(from);

        to = new Account(TO, ADVISOR, AccountType.TRUST);
        repo.save(to);

        repo.save(new Account(OTHER_ADVISOR, "ADV-001", AccountType.TRUST));
    }

    private static TransferRequest request(String fromAcct, String toAcct, String symbol, String qty, CostBasisMethod method) {
        TransferRequest r = new TransferRequest();
        r.setFromAccount(fromAcct);
        r.setToAccount(toAcct);
        r.setSymbol(symbol);
        r.setQuantity(new BigDecimal(qty));
        r.setMethod(method);
        return r;
    }

    private TransferRequest aapl(String qty) {
        return request(FROM, TO, "AAPL", qty, CostBasisMethod.FIFO);
    }

    @Test
    public void fifoHappyPathMovesOldestLotsAndCarriesBasis() {
        TransferResult result = service.transfer(aapl("120"));

        assertEquals("AAPL", result.getSymbol());
        assertEquals(new BigDecimal("120"), result.getQuantity());
        // 100 x 143.27 + 20 x 171.13 = 14327.00 + 3422.60
        assertEquals(new BigDecimal("17749.60"), result.getCostBasisTransferred());
        assertEquals(Arrays.asList("L1-T", "L2-T"), result.getLotIds());
        assertTrue(result.getTransferId().matches("TRF-\\d{4}-[0-9A-F]{8}"), result.getTransferId());

        Position dest = to.findPosition("AAPL");
        assertNotNull(dest);
        assertEquals(new BigDecimal("120"), dest.getQuantity());
        TaxLot movedL1 = dest.getLots().get(0);
        assertEquals("L1-T", movedL1.getLotId());
        assertEquals(new BigDecimal("143.27"), movedL1.getUnitCost());
        assertEquals(LocalDate.of(2021, 3, 12), movedL1.getAcquiredOn());
        TaxLot movedL2 = dest.getLots().get(1);
        assertEquals(new BigDecimal("20"), movedL2.getQuantity());
        assertEquals(LocalDate.of(2023, 8, 2), movedL2.getAcquiredOn());
    }

    @Test
    public void partialLotReliefLeavesRemainderInSource() {
        service.transfer(aapl("40"));

        Position source = from.findPosition("AAPL");
        assertEquals(2, source.getLots().size());
        assertEquals("L1", source.getLots().get(0).getLotId());
        assertEquals(new BigDecimal("60"), source.getLots().get(0).getQuantity());
        assertEquals(new BigDecimal("33"), source.getLots().get(1).getQuantity());
        assertEquals(new BigDecimal("93"), source.getQuantity());
    }

    @Test
    public void fullLotReliefRemovesLotButKeepsPosition() {
        service.transfer(aapl("100"));

        Position source = from.findPosition("AAPL");
        assertNotNull(source);
        assertEquals(1, source.getLots().size());
        assertEquals("L2", source.getLots().get(0).getLotId());
    }

    @Test
    public void fullPositionReliefRemovesPositionFromSource() {
        TransferResult result = service.transfer(aapl("133"));

        assertNull(from.findPosition("AAPL"));
        assertNotNull(from.findPosition("VEA"));
        assertEquals(1, from.getPositions().size());
        assertEquals(new BigDecimal("19974.29"), result.getCostBasisTransferred());
        assertEquals(new BigDecimal("133"), to.findPosition("AAPL").getQuantity());
    }

    @Test
    public void transferredBasisRoundsHalfUpForThreeDecimalUnitCost() {
        // WMP-1042: 33 x 50.135 = 1654.455 -> 1654.46
        TransferResult result = service.transfer(request(FROM, TO, "VEA", "33", CostBasisMethod.FIFO));
        assertEquals(new BigDecimal("1654.46"), result.getCostBasisTransferred());
    }

    @Test
    public void appendsToExistingDestinationPosition() {
        Position existing = new Position("AAPL");
        existing.addLot(new TaxLot("X1", new BigDecimal("5"), new BigDecimal("100.00"), LocalDate.of(2020, 1, 1)));
        to.addPosition(existing);

        service.transfer(aapl("10"));

        assertSame(existing, to.findPosition("AAPL"));
        assertEquals(2, existing.getLots().size());
        assertEquals(new BigDecimal("15"), existing.getQuantity());
        assertEquals(1, to.getPositions().size());
    }

    @Test
    public void lifoMovesNewestLotFirst() {
        TransferResult result = service.transfer(request(FROM, TO, "AAPL", "33", CostBasisMethod.LIFO));

        assertEquals(Arrays.asList("L2-T"), result.getLotIds());
        assertEquals(new BigDecimal("5647.29"), result.getCostBasisTransferred());
        assertEquals(1, from.findPosition("AAPL").getLots().size());
        assertEquals("L1", from.findPosition("AAPL").getLots().get(0).getLotId());
    }

    @Test
    public void symbolLookupIsCaseInsensitive() {
        TransferResult result = service.transfer(request(FROM, TO, "aapl", "1", CostBasisMethod.FIFO));
        assertEquals("aapl", result.getSymbol());
        assertEquals(new BigDecimal("132"), from.findPosition("AAPL").getQuantity());
    }

    @Test
    public void persistsBothAccounts() {
        service.transfer(aapl("10"));
        assertEquals(new BigDecimal("123"), repo.findByAccountNumber(FROM).get().findPosition("AAPL").getQuantity());
        assertEquals(new BigDecimal("10"), repo.findByAccountNumber(TO).get().findPosition("AAPL").getQuantity());
    }

    @Test
    public void unknownSourceAccountIsRejected() {
        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> service.transfer(request("0000-0000", TO, "AAPL", "1", CostBasisMethod.FIFO)));
        assertEquals("Account not found: 0000-0000", ex.getMessage());
    }

    @Test
    public void unknownDestinationAccountIsRejected() {
        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> service.transfer(request(FROM, "0000-0000", "AAPL", "1", CostBasisMethod.FIFO)));
        assertEquals("Account not found: 0000-0000", ex.getMessage());
    }

    @Test
    public void restrictedSourceAccountIsRejected() {
        from.setStatus(AccountStatus.RESTRICTED);
        TransferNotAllowedException ex = assertThrows(TransferNotAllowedException.class, () -> service.transfer(aapl("1")));
        assertEquals("Source account " + FROM + " is RESTRICTED", ex.getMessage());
        assertEquals(new BigDecimal("133"), from.findPosition("AAPL").getQuantity());
    }

    @Test
    public void transferPendingSourceAccountIsRejected() {
        from.setStatus(AccountStatus.TRANSFER_PENDING);
        TransferNotAllowedException ex = assertThrows(TransferNotAllowedException.class, () -> service.transfer(aapl("1")));
        assertEquals("Source account " + FROM + " is TRANSFER_PENDING", ex.getMessage());
    }

    @Test
    public void closedDestinationAccountIsRejected() {
        to.setStatus(AccountStatus.CLOSED);
        TransferNotAllowedException ex = assertThrows(TransferNotAllowedException.class, () -> service.transfer(aapl("1")));
        assertEquals("Destination account " + TO + " is closed", ex.getMessage());
    }

    @Test
    public void restrictedDestinationAccountIsAllowed() {
        to.setStatus(AccountStatus.RESTRICTED);
        TransferResult result = service.transfer(aapl("1"));
        assertEquals(new BigDecimal("1"), result.getQuantity());
    }

    @Test
    public void crossAdvisorTransferIsRejected() {
        TransferNotAllowedException ex = assertThrows(TransferNotAllowedException.class,
                () -> service.transfer(request(FROM, OTHER_ADVISOR, "AAPL", "1", CostBasisMethod.FIFO)));
        assertEquals("Cross-advisor transfers require ACATS workflow", ex.getMessage());
    }

    @Test
    public void unknownSymbolIsRejected() {
        TransferNotAllowedException ex = assertThrows(TransferNotAllowedException.class,
                () -> service.transfer(request(FROM, TO, "MSFT", "1", CostBasisMethod.FIFO)));
        assertEquals("Account " + FROM + " holds no MSFT", ex.getMessage());
        assertNull(to.findPosition("MSFT"));
    }

    @Test
    public void insufficientSharesIsRejectedWithoutMutatingAccounts() {
        InsufficientSharesException ex = assertThrows(InsufficientSharesException.class, () -> service.transfer(aapl("134")));
        assertEquals("Insufficient shares of AAPL: requested 134, available 133", ex.getMessage());
        assertEquals(new BigDecimal("133"), from.findPosition("AAPL").getQuantity());
        assertNull(to.findPosition("AAPL"));
    }
}

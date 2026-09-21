package com.westlake.advisor.transfer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.westlake.advisor.account.Account;
import com.westlake.advisor.account.AccountRepository;
import com.westlake.advisor.account.AccountStatus;
import com.westlake.advisor.account.Position;
import com.westlake.advisor.costbasis.CostBasisCalculator;
import com.westlake.advisor.costbasis.LotAllocation;
import com.westlake.advisor.costbasis.TaxLot;
import com.westlake.advisor.notification.AdvisorNotifier;

/**
 * Internal (same-custodian) position transfers between accounts, e.g. moving
 * shares from a taxable brokerage account into a trust. Cost basis and
 * acquisition dates carry over with the lots.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accounts;
    private final CostBasisCalculator costBasis;
    private final AdvisorNotifier notifier;

    public TransferService(AccountRepository accounts, CostBasisCalculator costBasis, AdvisorNotifier notifier) {
        this.accounts = accounts;
        this.costBasis = costBasis;
        this.notifier = notifier;
    }

    public TransferResult transfer(TransferRequest request) {
        Account from = accounts.findByAccountNumber(request.getFromAccount())
                .orElseThrow(() -> new AccountNotFoundException(request.getFromAccount()));
        Account to = accounts.findByAccountNumber(request.getToAccount())
                .orElseThrow(() -> new AccountNotFoundException(request.getToAccount()));

        if (from.getStatus() != AccountStatus.OPEN) {
            throw new TransferNotAllowedException("Source account " + from.getAccountNumber() + " is " + from.getStatus());
        }
        if (to.getStatus() == AccountStatus.CLOSED) {
            throw new TransferNotAllowedException("Destination account " + to.getAccountNumber() + " is closed");
        }
        if (!from.getAdvisorId().equals(to.getAdvisorId())) {
            throw new TransferNotAllowedException("Cross-advisor transfers require ACATS workflow");
        }

        Position source = from.findPosition(request.getSymbol());
        if (source == null) {
            throw new TransferNotAllowedException("Account " + from.getAccountNumber() + " holds no " + request.getSymbol());
        }

        List<LotAllocation> allocations = costBasis.selectLots(source, request.getQuantity(), request.getMethod());

        Position destination = to.findPosition(request.getSymbol());
        if (destination == null) {
            destination = new Position(request.getSymbol());
            to.addPosition(destination);
        }

        BigDecimal transferredBasis = BigDecimal.ZERO;
        List<String> lotIds = new ArrayList<>();
        for (LotAllocation allocation : allocations) {
            TaxLot lot = allocation.getLot();
            BigDecimal qty = allocation.getQuantity();

            lot.setQuantity(lot.getQuantity().subtract(qty));
            if (lot.getQuantity().signum() == 0) {
                source.getLots().remove(lot);
            }

            TaxLot moved = new TaxLot(lot.getLotId() + "-T", qty, lot.getUnitCost(), lot.getAcquiredOn());
            destination.addLot(moved);
            lotIds.add(moved.getLotId());
            transferredBasis = transferredBasis.add(allocation.getCostBasis());
        }

        if (source.getLots().isEmpty()) {
            from.removePosition(source);
        }

        accounts.save(from);
        accounts.save(to);

        String transferId = "TRF-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Transfer {} complete: {} {} from {} to {}", transferId, request.getQuantity(), request.getSymbol(),
                from.getAccountNumber(), to.getAccountNumber());

        notifier.transferCompleted(from.getAdvisorId(), transferId, request.getSymbol(), request.getQuantity());

        return new TransferResult(transferId, request.getSymbol(), request.getQuantity(),
                transferredBasis.setScale(2, BigDecimal.ROUND_HALF_UP), lotIds);
    }
}

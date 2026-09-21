package com.westlake.advisor.costbasis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.westlake.advisor.account.Position;

/**
 * Cost basis engine. Ported from the COBOL CBASIS module in 2014; lot selection
 * rules follow the WMP-Cost-Basis-Policy v3 document.
 */
@Component
public class CostBasisCalculator {

    /**
     * Total cost basis for a position, summing all open lots.
     */
    public BigDecimal totalCostBasis(Position position) {
        double total = 0.0;
        for (TaxLot lot : position.getLots()) {
            total += lot.getUnitCost().doubleValue() * lot.getQuantity().doubleValue();
        }
        return new BigDecimal(total).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Average cost per share across all lots in the position.
     */
    public BigDecimal averageUnitCost(Position position) {
        BigDecimal qty = position.getQuantity();
        if (qty.signum() == 0) {
            return BigDecimal.ZERO;
        }
        double avg = totalCostBasis(position).doubleValue() / qty.doubleValue();
        return new BigDecimal(avg).setScale(4, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Select lots to relieve for a partial transfer or sale of {@code quantity} shares.
     * Returns the lots in the order they should be relieved. The caller is responsible
     * for mutating lot quantities.
     */
    public List<LotAllocation> selectLots(Position position, BigDecimal quantity, CostBasisMethod method) {
        List<TaxLot> ordered = new ArrayList<>(position.getLots());
        switch (method) {
            case FIFO:
                ordered.sort(Comparator.comparing(TaxLot::getAcquiredOn));
                break;
            case LIFO:
                ordered.sort(Comparator.comparing(TaxLot::getAcquiredOn).reversed());
                break;
            case HIGH_COST:
                ordered.sort(Comparator.comparing(TaxLot::getUnitCost).reversed());
                break;
            case AVERAGE_COST:
                // average cost relieves pro-rata; handled below
                break;
            default:
                throw new IllegalArgumentException("Unsupported method " + method);
        }

        List<LotAllocation> allocations = new ArrayList<>();
        BigDecimal remaining = quantity;

        if (method == CostBasisMethod.AVERAGE_COST) {
            BigDecimal totalQty = position.getQuantity();
            for (TaxLot lot : ordered) {
                BigDecimal share = quantity.multiply(lot.getQuantity()).divide(totalQty, 4, BigDecimal.ROUND_HALF_UP);
                allocations.add(new LotAllocation(lot, share));
            }
            return allocations;
        }

        for (TaxLot lot : ordered) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal take = lot.getQuantity().min(remaining);
            allocations.add(new LotAllocation(lot, take));
            remaining = remaining.subtract(take);
        }

        if (remaining.signum() > 0) {
            throw new InsufficientSharesException(position.getSymbol(), quantity, position.getQuantity());
        }
        return allocations;
    }
}

package com.westlake.advisor.costbasis;

import java.math.BigDecimal;

public class LotAllocation {

    private final TaxLot lot;
    private final BigDecimal quantity;

    public LotAllocation(TaxLot lot, BigDecimal quantity) {
        this.lot = lot;
        this.quantity = quantity;
    }

    public TaxLot getLot() {
        return lot;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getCostBasis() {
        return lot.getUnitCost().multiply(quantity);
    }
}

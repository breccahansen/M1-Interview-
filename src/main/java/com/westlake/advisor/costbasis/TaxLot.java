package com.westlake.advisor.costbasis;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TaxLot {

    private final String lotId;
    private BigDecimal quantity;
    private final BigDecimal unitCost;
    private final LocalDate acquiredOn;

    public TaxLot(String lotId, BigDecimal quantity, BigDecimal unitCost, LocalDate acquiredOn) {
        this.lotId = lotId;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.acquiredOn = acquiredOn;
    }

    public String getLotId() {
        return lotId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public LocalDate getAcquiredOn() {
        return acquiredOn;
    }

    public BigDecimal getTotalCost() {
        return unitCost.multiply(quantity);
    }
}

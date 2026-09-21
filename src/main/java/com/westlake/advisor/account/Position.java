package com.westlake.advisor.account;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.westlake.advisor.costbasis.TaxLot;

public class Position {

    private final String symbol;
    private final List<TaxLot> lots = new ArrayList<>();

    public Position(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public List<TaxLot> getLots() {
        return lots;
    }

    public void addLot(TaxLot lot) {
        lots.add(lot);
    }

    public BigDecimal getQuantity() {
        BigDecimal total = BigDecimal.ZERO;
        for (TaxLot lot : lots) {
            total = total.add(lot.getQuantity());
        }
        return total;
    }
}

package com.westlake.advisor.costbasis;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import com.westlake.advisor.account.Position;

public class CostBasisCalculatorTest {

    private final CostBasisCalculator calc = new CostBasisCalculator();

    private Position twoLotPosition() {
        Position p = new Position("AAPL");
        p.addLot(new TaxLot("L1", new BigDecimal("100"), new BigDecimal("143.27"), LocalDate.of(2021, 3, 12)));
        p.addLot(new TaxLot("L2", new BigDecimal("33"), new BigDecimal("171.13"), LocalDate.of(2023, 8, 2)));
        return p;
    }

    @Test
    public void fifoRelievesOldestLotFirst() {
        List<LotAllocation> allocs = calc.selectLots(twoLotPosition(), new BigDecimal("120"), CostBasisMethod.FIFO);
        assertEquals(2, allocs.size());
        assertEquals("L1", allocs.get(0).getLot().getLotId());
        assertEquals(new BigDecimal("100"), allocs.get(0).getQuantity());
        assertEquals("L2", allocs.get(1).getLot().getLotId());
        assertEquals(new BigDecimal("20"), allocs.get(1).getQuantity());
    }

    @Test
    public void totalCostBasisSumsLots() {
        assertEquals(new BigDecimal("19974.29"), calc.totalCostBasis(twoLotPosition()));
    }
}

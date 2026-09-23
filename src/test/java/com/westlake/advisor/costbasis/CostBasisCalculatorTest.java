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

    private Position veaPosition() {
        Position p = new Position("VEA");
        p.addLot(new TaxLot("L5", new BigDecimal("33"), new BigDecimal("50.135"), LocalDate.of(2024, 1, 9)));
        return p;
    }

    @Test
    public void totalCostBasisRoundsHalfUpWithThreeDecimalUnitCost() {
        // 33 * 50.135 = 1654.455 exactly; HALF_UP must yield 1654.46 (WMP-1042)
        assertEquals(new BigDecimal("1654.46"), calc.totalCostBasis(veaPosition()));
    }

    @Test
    public void totalCostBasisSumsMultipleThreeDecimalLotsExactly() {
        Position p = veaPosition();
        // 17 * 12.345 = 209.865; total 1654.455 + 209.865 = 1864.320
        p.addLot(new TaxLot("L6", new BigDecimal("17"), new BigDecimal("12.345"), LocalDate.of(2024, 5, 20)));
        assertEquals(new BigDecimal("1864.32"), calc.totalCostBasis(p));
    }

    @Test
    public void averageUnitCostUsesExactDecimalArithmetic() {
        assertEquals(new BigDecimal("50.1350"), calc.averageUnitCost(veaPosition()));
    }
}

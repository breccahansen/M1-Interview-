package com.westlake.advisor.costbasis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

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
    public void fifoStopsOnceQuantityIsCovered() {
        List<LotAllocation> allocs = calc.selectLots(twoLotPosition(), new BigDecimal("100"), CostBasisMethod.FIFO);
        assertEquals(1, allocs.size());
        assertEquals("L1", allocs.get(0).getLot().getLotId());
        assertEquals(new BigDecimal("100"), allocs.get(0).getQuantity());
        assertEquals(new BigDecimal("14327.00"), allocs.get(0).getCostBasis());
    }

    @Test
    public void lifoRelievesNewestLotFirst() {
        List<LotAllocation> allocs = calc.selectLots(twoLotPosition(), new BigDecimal("40"), CostBasisMethod.LIFO);
        assertEquals(2, allocs.size());
        assertEquals("L2", allocs.get(0).getLot().getLotId());
        assertEquals(new BigDecimal("33"), allocs.get(0).getQuantity());
        assertEquals("L1", allocs.get(1).getLot().getLotId());
        assertEquals(new BigDecimal("7"), allocs.get(1).getQuantity());
    }

    @Test
    public void highCostRelievesMostExpensiveLotFirstRegardlessOfDate() {
        Position p = twoLotPosition();
        // newest lot but cheapest: must be relieved last
        p.addLot(new TaxLot("L3", new BigDecimal("10"), new BigDecimal("99.00"), LocalDate.of(2024, 1, 1)));
        List<LotAllocation> allocs = calc.selectLots(p, new BigDecimal("135"), CostBasisMethod.HIGH_COST);
        assertEquals(3, allocs.size());
        assertEquals("L2", allocs.get(0).getLot().getLotId());
        assertEquals(new BigDecimal("33"), allocs.get(0).getQuantity());
        assertEquals("L1", allocs.get(1).getLot().getLotId());
        assertEquals(new BigDecimal("100"), allocs.get(1).getQuantity());
        assertEquals("L3", allocs.get(2).getLot().getLotId());
        assertEquals(new BigDecimal("2"), allocs.get(2).getQuantity());
    }

    @Test
    public void averageCostRelievesProRataAcrossAllLots() {
        List<LotAllocation> allocs = calc.selectLots(twoLotPosition(), new BigDecimal("40"), CostBasisMethod.AVERAGE_COST);
        assertEquals(2, allocs.size());
        // 40 x 100 / 133 = 30.0752 ; 40 x 33 / 133 = 9.9248
        assertEquals("L1", allocs.get(0).getLot().getLotId());
        assertEquals(new BigDecimal("30.0752"), allocs.get(0).getQuantity());
        assertEquals("L2", allocs.get(1).getLot().getLotId());
        assertEquals(new BigDecimal("9.9248"), allocs.get(1).getQuantity());
        assertEquals(new BigDecimal("40.0000"),
                allocs.get(0).getQuantity().add(allocs.get(1).getQuantity()));
    }

    @Test
    public void insufficientSharesThrowsWithRequestedAndAvailable() {
        InsufficientSharesException ex = assertThrows(InsufficientSharesException.class,
                () -> calc.selectLots(twoLotPosition(), new BigDecimal("133.5"), CostBasisMethod.FIFO));
        assertEquals("Insufficient shares of AAPL: requested 133.5, available 133", ex.getMessage());
    }

    @Test
    public void totalCostBasisSumsLots() {
        assertEquals(new BigDecimal("19974.29"), calc.totalCostBasis(twoLotPosition()));
    }

    @Test
    public void totalCostBasisRoundsHalfUpForThreeDecimalUnitCost() {
        // WMP-1042: 33 x 50.135 = 1654.455 -> 1654.46 (CBASIS book of record)
        Position p = new Position("VEA");
        p.addLot(new TaxLot("L5", new BigDecimal("33"), new BigDecimal("50.135"), LocalDate.of(2024, 5, 9)));
        assertEquals(new BigDecimal("1654.46"), calc.totalCostBasis(p));
        assertEquals(new BigDecimal("50.1350"), calc.averageUnitCost(p));
    }

    @Test
    public void totalCostBasisIsExactAcrossMultipleFractionalLots() {
        // 12.5 x 10.005 = 125.0625 ; 7 x 20.015 = 140.105 ; sum 265.1675 -> 265.17
        Position p = new Position("VTI");
        p.addLot(new TaxLot("L1", new BigDecimal("12.5"), new BigDecimal("10.005"), LocalDate.of(2024, 1, 2)));
        p.addLot(new TaxLot("L2", new BigDecimal("7"), new BigDecimal("20.015"), LocalDate.of(2024, 2, 3)));
        assertEquals(new BigDecimal("265.17"), calc.totalCostBasis(p));
        // 265.1675 / 19.5 = 13.59833... -> 13.5983 (from the unrounded total)
        assertEquals(new BigDecimal("13.5983"), calc.averageUnitCost(p));
    }

    @Test
    public void averageUnitCostIsZeroForEmptyPosition() {
        assertEquals(BigDecimal.ZERO, calc.averageUnitCost(new Position("EMPTY")));
    }
}

package com.westlake.advisor.costbasis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.westlake.advisor.account.Position;

class CostBasisCalculatorTest {

    private final CostBasisCalculator calc = new CostBasisCalculator();

    private Position twoLotPosition() {
        Position p = new Position("AAPL");
        p.addLot(new TaxLot("L1", new BigDecimal("100"), new BigDecimal("143.27"), LocalDate.of(2021, 3, 12)));
        p.addLot(new TaxLot("L2", new BigDecimal("33"), new BigDecimal("171.13"), LocalDate.of(2023, 8, 2)));
        return p;
    }

    /** Three lots whose date order differs from their cost order. */
    private Position threeLotPosition() {
        Position p = new Position("VTI");
        p.addLot(new TaxLot("OLD_MID", new BigDecimal("10"), new BigDecimal("200.00"), LocalDate.of(2020, 1, 1)));
        p.addLot(new TaxLot("MID_HIGH", new BigDecimal("20"), new BigDecimal("250.00"), LocalDate.of(2021, 1, 1)));
        p.addLot(new TaxLot("NEW_LOW", new BigDecimal("30"), new BigDecimal("150.00"), LocalDate.of(2022, 1, 1)));
        return p;
    }

    @Test
    void fifoRelievesOldestLotFirst() {
        List<LotAllocation> allocs = calc.selectLots(twoLotPosition(), new BigDecimal("120"), CostBasisMethod.FIFO);
        assertEquals(2, allocs.size());
        assertEquals("L1", allocs.get(0).getLot().getLotId());
        assertEquals(new BigDecimal("100"), allocs.get(0).getQuantity());
        assertEquals("L2", allocs.get(1).getLot().getLotId());
        assertEquals(new BigDecimal("20"), allocs.get(1).getQuantity());
    }

    @Test
    void fifoIgnoresInsertionOrderAndSortsByAcquisitionDate() {
        Position p = new Position("X");
        TaxLot newer = new TaxLot("NEWER", new BigDecimal("5"), new BigDecimal("10"), LocalDate.of(2024, 1, 1));
        TaxLot older = new TaxLot("OLDER", new BigDecimal("5"), new BigDecimal("10"), LocalDate.of(2020, 1, 1));
        p.addLot(newer);
        p.addLot(older);

        List<LotAllocation> allocs = calc.selectLots(p, new BigDecimal("5"), CostBasisMethod.FIFO);
        assertEquals(1, allocs.size());
        assertSame(older, allocs.get(0).getLot());
    }

    @Test
    void lifoRelievesNewestLotFirst() {
        List<LotAllocation> allocs = calc.selectLots(threeLotPosition(), new BigDecimal("35"), CostBasisMethod.LIFO);
        assertEquals(2, allocs.size());
        assertEquals("NEW_LOW", allocs.get(0).getLot().getLotId());
        assertEquals(new BigDecimal("30"), allocs.get(0).getQuantity());
        assertEquals("MID_HIGH", allocs.get(1).getLot().getLotId());
        assertEquals(new BigDecimal("5"), allocs.get(1).getQuantity());
        // 30 x 150 + 5 x 250
        assertEquals(new BigDecimal("4500.00"), allocs.get(0).getCostBasis());
        assertEquals(new BigDecimal("1250.00"), allocs.get(1).getCostBasis());
    }

    @Test
    void highCostRelievesMostExpensiveLotFirst() {
        List<LotAllocation> allocs = calc.selectLots(threeLotPosition(), new BigDecimal("25"), CostBasisMethod.HIGH_COST);
        assertEquals(2, allocs.size());
        assertEquals("MID_HIGH", allocs.get(0).getLot().getLotId());
        assertEquals(new BigDecimal("20"), allocs.get(0).getQuantity());
        assertEquals("OLD_MID", allocs.get(1).getLot().getLotId());
        assertEquals(new BigDecimal("5"), allocs.get(1).getQuantity());
    }

    @Test
    void averageCostRelievesProRataAcrossAllLots() {
        // 60 shares total; transferring 30 takes half of every lot
        List<LotAllocation> allocs = calc.selectLots(threeLotPosition(), new BigDecimal("30"), CostBasisMethod.AVERAGE_COST);
        assertEquals(3, allocs.size());
        assertEquals("OLD_MID", allocs.get(0).getLot().getLotId());
        assertEquals(new BigDecimal("5.0000"), allocs.get(0).getQuantity());
        assertEquals("MID_HIGH", allocs.get(1).getLot().getLotId());
        assertEquals(new BigDecimal("10.0000"), allocs.get(1).getQuantity());
        assertEquals("NEW_LOW", allocs.get(2).getLot().getLotId());
        assertEquals(new BigDecimal("15.0000"), allocs.get(2).getQuantity());
    }

    @Test
    void averageCostRoundsProRataShareToFourDecimals() {
        // 133 shares; 10 x 100 / 133 = 7.5187969... -> 7.5188 ; 10 x 33 / 133 = 2.4812030... -> 2.4812
        List<LotAllocation> allocs = calc.selectLots(twoLotPosition(), new BigDecimal("10"), CostBasisMethod.AVERAGE_COST);
        assertEquals(new BigDecimal("7.5188"), allocs.get(0).getQuantity());
        assertEquals(new BigDecimal("2.4812"), allocs.get(1).getQuantity());
    }

    @Test
    void exactQuantityOfSingleLotAllocatesWholeLot() {
        List<LotAllocation> allocs = calc.selectLots(twoLotPosition(), new BigDecimal("100"), CostBasisMethod.FIFO);
        assertEquals(1, allocs.size());
        assertEquals("L1", allocs.get(0).getLot().getLotId());
        assertEquals(new BigDecimal("100"), allocs.get(0).getQuantity());
    }

    @Test
    void zeroQuantitySelectsNoLots() {
        assertTrue(calc.selectLots(twoLotPosition(), BigDecimal.ZERO, CostBasisMethod.FIFO).isEmpty());
    }

    @Test
    void insufficientSharesThrowsWithRequestedAndAvailable() {
        InsufficientSharesException e = assertThrows(InsufficientSharesException.class,
                () -> calc.selectLots(twoLotPosition(), new BigDecimal("133.5"), CostBasisMethod.LIFO));
        assertEquals("Insufficient shares of AAPL: requested 133.5, available 133", e.getMessage());
    }

    @Test
    void insufficientSharesOnEmptyPosition() {
        InsufficientSharesException e = assertThrows(InsufficientSharesException.class,
                () -> calc.selectLots(new Position("EMPTY"), BigDecimal.ONE, CostBasisMethod.HIGH_COST));
        assertEquals("Insufficient shares of EMPTY: requested 1, available 0", e.getMessage());
    }

    @Test
    void lotAllocationCostBasisIsUnitCostTimesQuantity() {
        TaxLot lot = new TaxLot("L", new BigDecimal("33"), new BigDecimal("50.135"), LocalDate.of(2024, 5, 9));
        LotAllocation a = new LotAllocation(lot, new BigDecimal("3"));
        assertSame(lot, a.getLot());
        assertEquals(new BigDecimal("150.405"), a.getCostBasis());
        assertEquals(new BigDecimal("1654.455"), lot.getTotalCost());
    }

    @Test
    void totalCostBasisSumsLots() {
        assertEquals(new BigDecimal("19974.29"), calc.totalCostBasis(twoLotPosition()));
    }

    @Test
    void totalCostBasisRoundsHalfUpForThreeDecimalUnitCost() {
        // WMP-1042: 33 x 50.135 = 1654.455 -> 1654.46 (CBASIS book of record)
        Position p = new Position("VEA");
        p.addLot(new TaxLot("L5", new BigDecimal("33"), new BigDecimal("50.135"), LocalDate.of(2024, 5, 9)));
        assertEquals(new BigDecimal("1654.46"), calc.totalCostBasis(p));
        assertEquals(new BigDecimal("50.1350"), calc.averageUnitCost(p));
    }

    @Test
    void totalCostBasisIsExactAcrossMultipleFractionalLots() {
        // 12.5 x 10.005 = 125.0625 ; 7 x 20.015 = 140.105 ; sum 265.1675 -> 265.17
        Position p = new Position("VTI");
        p.addLot(new TaxLot("L1", new BigDecimal("12.5"), new BigDecimal("10.005"), LocalDate.of(2024, 1, 2)));
        p.addLot(new TaxLot("L2", new BigDecimal("7"), new BigDecimal("20.015"), LocalDate.of(2024, 2, 3)));
        assertEquals(new BigDecimal("265.17"), calc.totalCostBasis(p));
        // 265.1675 / 19.5 = 13.59833... -> 13.5983 (from the unrounded total)
        assertEquals(new BigDecimal("13.5983"), calc.averageUnitCost(p));
    }

    @Test
    void averageUnitCostIsZeroForEmptyPosition() {
        assertEquals(BigDecimal.ZERO, calc.averageUnitCost(new Position("EMPTY")));
        assertEquals(new BigDecimal("0.00"), calc.totalCostBasis(new Position("EMPTY")));
    }
}

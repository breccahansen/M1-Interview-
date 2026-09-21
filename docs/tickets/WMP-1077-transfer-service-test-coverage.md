# WMP-1077 · Bring TransferService and CostBasisCalculator to ≥ 80% line coverage

| Field | Value |
|---|---|
| Type | Tech debt |
| Priority | P3 |
| Component | transfer, costbasis |
| Epic | Quality Gates (WMP-1005) |

## Context

`TransferService` has **zero** unit tests. JaCoCo currently reports ~40% line coverage
for the module. The Quality Gates program requires ≥ 80% before the service can be
onboarded to the automated release train.

## Scope

- Unit tests for `TransferService.transfer` covering: happy path (FIFO), partial lot
  relief, full lot relief removing the lot and the position, cross-advisor rejection,
  restricted/closed account rejection, unknown symbol, insufficient shares.
- Unit tests for `CostBasisCalculator.selectLots` for LIFO, HIGH_COST and AVERAGE_COST.
- Prefer plain JUnit with hand-built fixtures over Spring context tests.
- Migrate the test suite to JUnit 5 if it can be done in the same PR without churn.

## Acceptance criteria

- [ ] JaCoCo line coverage ≥ 80% for `com.westlake.advisor.transfer` and
      `com.westlake.advisor.costbasis`.
- [ ] Tests run in under 5 seconds.

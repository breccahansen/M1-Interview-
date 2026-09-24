# WMP-1077 · Bring backend (TransferService, CostBasisCalculator) and Advisor Workstation to ≥ 80% line coverage

| Field | Value |
|---|---|
| Type | Tech debt |
| Priority | P3 |
| Component | transfer, costbasis, frontend |
| Epic | Quality Gates (WMP-1005) |

## Context

`TransferService` has **zero** unit tests. JaCoCo currently reports ~40% line coverage
for the backend. The Angular Advisor Workstation (`frontend/`) has a single pipe spec;
Karma reports ~21% line coverage and none of the components or the API service are
tested. The Quality Gates program requires ≥ 80% before the service can be
onboarded to the automated release train.

## Scope

- Unit tests for `TransferService.transfer` covering: happy path (FIFO), partial lot
  relief, full lot relief removing the lot and the position, cross-advisor rejection,
  restricted/closed account rejection, unknown symbol, insufficient shares.
- Unit tests for `CostBasisCalculator.selectLots` for LIFO, HIGH_COST and AVERAGE_COST.
- Prefer plain JUnit with hand-built fixtures over Spring context tests.
- Migrate the test suite to JUnit 5 if it can be done in the same PR without churn.
- Frontend (`frontend/`): Jasmine specs for `AdvisorApiService` (HttpTestingController),
  `PositionsComponent` (loading, 404, totals), `TransferComponent` (validation, same-account
  guard, success and 422 paths) and `LotBadgeComponent`. No real HTTP calls.

## Acceptance criteria

- [ ] JaCoCo line coverage ≥ 80% for `com.westlake.advisor.transfer` and
      `com.westlake.advisor.costbasis`.
- [ ] Karma line coverage ≥ 80% for `frontend/src/app`
      (`npm test -- --watch=false --browsers=ChromeHeadless`, see `coverage/`).
- [ ] Backend tests run in under 5 seconds; frontend suite under 30 seconds.
- [ ] Tests assert behaviour (values, rejections), not just "does not throw".

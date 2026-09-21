# WMP-1042 · Cost basis on positions API off by $0.01 vs. CBASIS book of record

| Field | Value |
|---|---|
| Type | Bug |
| Priority | P2 |
| Component | costbasis |
| Reporter | Advisor Center support (escalation from ADV-114) |
| Affects | advisor-platform 4.11.x |

## Summary

`GET /api/v1/accounts/{acct}/positions` returns a `costBasis` that disagrees with the
nightly CBASIS reconciliation file by one cent on certain lots. Advisors see the
discrepancy on the client statement preview and open support cases.

## Reproduction

Account `7781-2204`, position `VEA`, single lot L5: 33 shares @ 50.135 (fractional-share
average fill, 3-decimal unit cost).

```
GET /api/v1/accounts/7781-2204/positions
```

| Source | costBasis |
|---|---|
| API (actual) | `1654.45` |
| CBASIS book of record (expected) | `1654.46` |

Manual math: 33 × 50.135 = 1654.455 → half-up to cents = **1654.46**.

Other lots with a 3-decimal unit cost and an odd quantity show the same behaviour.
Lots with 2-decimal unit costs reconcile cleanly.

## Acceptance criteria

- [ ] `costBasis` and `averageUnitCost` match exact decimal arithmetic with HALF_UP rounding
      for all lots, including 3+ decimal unit costs and fractional quantities.
- [ ] Regression test covering the VEA example above and at least one multi-lot position.
- [ ] No change to the response schema.
- [ ] `TransferResult.costBasisTransferred` continues to reconcile with the sum of relieved lots.

## Notes from the on-call engineer

The calculator was ported from COBOL in 2014 and nobody currently on the team has touched
it. Please keep the change minimal and explain the root cause in the PR.

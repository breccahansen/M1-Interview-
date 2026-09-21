# WMP-1080 · Write developer documentation for the costbasis module

| Field | Value |
|---|---|
| Type | Task |
| Priority | P3 |
| Component | costbasis |

## Context

The original author of the cost basis port left in 2019. New engineers rely on reading
the code and on the archived WMP-Cost-Basis-Policy v3 PDF. We need in-repo documentation.

## Deliverable

`docs/costbasis.md` covering:

- Lot selection semantics for each `CostBasisMethod`, with a worked example per method.
- How partial lot relief mutates `TaxLot` quantities and creates `-T` lots on transfer.
- Rounding and scale rules (where cents vs. 4-decimal scale is used and why).
- Known limitations and open questions (e.g. wash-sale handling is out of scope).

Also add an Architecture Decision Record under `docs/adr/` capturing the decision to use
exact decimal arithmetic for money.

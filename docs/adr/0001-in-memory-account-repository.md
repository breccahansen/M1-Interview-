# ADR 0001 · In-memory AccountRepository for local development

**Status:** Accepted (2019-04)

## Context

The production AccountMaster lives in DB2 on the mainframe and is reached through the
MQ gateway. Engineers cannot run the gateway locally.

## Decision

`AccountRepository` is an in-memory, seeded implementation. The production adapter is
supplied by the `advisor-platform-mainframe` deployment module and is not in this repo.

## Consequences

- Local runs and unit tests never touch the mainframe.
- Seed data must be kept representative of production edge cases (fractional shares,
  multi-lot positions, restricted accounts).

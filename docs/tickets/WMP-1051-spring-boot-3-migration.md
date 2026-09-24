# WMP-1051 · Migrate advisor-platform to Spring Boot 3.x / Jakarta EE 9+

| Field | Value |
|---|---|
| Type | Story |
| Priority | P1 |
| Component | platform |
| Epic | Java Platform Modernization (WMP-1000) |
| Blocked by | WMP-1091 (Java 11 → 17) |

## Why

Spring Boot 2.7 reached end of OSS support in November 2023. Enterprise Architecture
requires all Tier-1 services to be on a supported Boot line by end of quarter.

## Scope

- Requires Java 17 (WMP-1091) — do it first or in the same PR.
- Upgrade parent to Spring Boot 3.x (latest GA in the 3.x line).
- Replace `javax.*` imports with `jakarta.*` (validation, servlet).
- Remove deprecated `BigDecimal.ROUND_*` constants in favour of `RoundingMode`.
- Keep JUnit 4 tests passing (JUnit 5 migration is tracked separately as WMP-1077).
- Update CI workflow if the Java/Maven setup changes.

## Acceptance criteria

- [ ] `mvn verify` is green.
- [ ] Application starts and both controllers respond (`/api/v1/accounts/7781-2204`,
      `POST /api/v1/transfers`).
- [ ] No `javax.*` imports remain.
- [ ] PR description lists every breaking change encountered and how it was resolved.

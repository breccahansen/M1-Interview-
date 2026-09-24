# WMP-1091 · Upgrade advisor-platform runtime from Java 11 to Java 17

| Field | Value |
|---|---|
| Type | Story |
| Priority | P1 |
| Component | platform |
| Epic | Java Platform Modernization (WMP-1000) |
| Blocks | WMP-1051 (Spring Boot 3 requires Java 17) |

## Why

Java 11 leaves the firm's supported-runtime list at the end of the quarter, and the
Spring Boot 3 migration (WMP-1051) cannot start until the service builds and runs on
Java 17. This ticket is the prerequisite hop.

## Scope

- Move `java.version` to 17 and the CI workflow (`.github/workflows/ci.yml`) to
  Temurin 17.
- Compile with `-Xlint:all` once and resolve any new warnings that indicate
  removed/deprecated APIs (e.g. `BigDecimal.ROUND_*`, illegal reflective access).
- Where it improves clarity without changing behaviour, adopt Java 17 language features:
  `record` for immutable value types (`TransferResult`, `TaxLot` candidates), switch
  expressions in `CostBasisCalculator`, `Stream.toList()`, text blocks in tests.
- Verify JaCoCo 0.8.11 instruments Java 17 bytecode correctly (it does; keep the
  version unless the build says otherwise).
- Do **not** upgrade Spring Boot in this ticket.

## Acceptance criteria

- [ ] `mvn verify` is green on JDK 17 with `java.version` = 17.
- [ ] Application starts on JDK 17 and both endpoints respond.
- [ ] No `--add-opens` / `--illegal-access` flags required.
- [ ] CI runs on Temurin 17.
- [ ] PR description summarises API changes encountered and the language features
      adopted, so the review board can assess risk.

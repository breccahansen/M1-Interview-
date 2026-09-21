# Westlake Advisor Platform (WMP)

Advisor-facing account, position transfer and cost-basis services for a fictional
brokerage. Spring Boot 2.7 · Java 17 · Maven · JUnit 4.

This repository exists as a **realistic legacy-modernization sandbox** for demonstrating
Devin in a customer setting. It is intentionally a little dated: an EOL Spring Boot line,
`javax.*` imports, a vulnerable dependency, thin test coverage, and a subtle money-math
bug ported from COBOL.

## Run

```bash
mvn -q verify                 # build + tests + JaCoCo report (target/site/jacoco)
mvn spring-boot:run           # http://localhost:8080
curl -s localhost:8080/api/v1/accounts/7781-2204/positions | jq
curl -s -X POST localhost:8080/api/v1/transfers -H 'content-type: application/json' \
  -d '{"fromAccount":"7781-2204","toAccount":"7781-9930","symbol":"AAPL","quantity":120,"method":"FIFO"}'
```

## Backlog (`docs/tickets/`)

| Ticket | Type | Demo angle |
|---|---|---|
| [WMP-1042](docs/tickets/WMP-1042-cost-basis-penny-mismatch.md) | Bug | Ticket → root cause → tested PR in one session |
| [WMP-1051](docs/tickets/WMP-1051-spring-boot-3-migration.md) | Migration | Spring Boot 2.7 → 3.x, `javax` → `jakarta` |
| [WMP-1063](docs/tickets/WMP-1063-commons-text-cve.md) | Vulnerability | CVE remediation with evidence for AppSec |
| [WMP-1077](docs/tickets/WMP-1077-transfer-service-test-coverage.md) | Tech debt | 40% → 80% coverage on an untested service |
| [WMP-1080](docs/tickets/WMP-1080-document-costbasis-module.md) | Docs | Documentation + ADR for an orphaned module |

## Layout

```
src/main/java/com/westlake/advisor
├── account/       Account, Position, in-memory AccountRepository (seeded)
├── costbasis/     TaxLot, CostBasisCalculator (FIFO/LIFO/HIGH_COST/AVERAGE_COST)
├── transfer/      TransferService: same-custodian position transfers
├── notification/  AdvisorNotifier (commons-text StringSubstitutor)
└── web/           REST controllers under /api/v1
```

All data is fictional. Account numbers, advisors and tickers do not refer to any real
customer or institution.

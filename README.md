# Westlake Advisor Platform (WMP)

Advisor-facing account, position transfer and cost-basis services for a fictional
brokerage. Spring Boot 2.7 · Java 11 · Maven · JUnit 4 backend, with an Angular 18 advisor
workstation in `frontend/`.

This repository exists as a **realistic legacy-modernization sandbox** for demonstrating
Devin in a customer setting. It is intentionally a little dated: Java 11, an EOL Spring Boot line,
`javax.*` imports, a vulnerable dependency, thin test
coverage on both tiers, and a subtle money-math bug ported from COBOL.

## Run

```bash
mvn -q verify                 # build + tests + JaCoCo report (target/site/jacoco)
mvn spring-boot:run           # http://localhost:8080
curl -s localhost:8080/api/v1/accounts/7781-2204/positions | jq
curl -s -X POST localhost:8080/api/v1/transfers -H 'content-type: application/json' \
  -d '{"fromAccount":"7781-2204","toAccount":"7781-9930","symbol":"AAPL","quantity":120,"method":"FIFO"}'

cd frontend && npm ci && npm start       # Angular 18 UI on http://localhost:4200 (proxies /api)
npm test -- --watch=false --browsers=ChromeHeadless   # Karma + coverage
```

## Backlog (`docs/tickets/`)

| Ticket | Type | Demo angle |
|---|---|---|
| [WMP-1042](docs/tickets/WMP-1042-cost-basis-penny-mismatch.md) | Bug | Ticket → root cause → tested PR in one session |
| [WMP-1090](docs/tickets/WMP-1090-angular-14-to-18-upgrade.md) | Migration | Angular 14 → 18, one major at a time |
| [WMP-1091](docs/tickets/WMP-1091-java-11-to-17-upgrade.md) | Migration | Java 11 → 17 (prerequisite for Boot 3) |
| [WMP-1051](docs/tickets/WMP-1051-spring-boot-3-migration.md) | Migration | Spring Boot 2.7 → 3.x, `javax` → `jakarta` |
| [WMP-1063](docs/tickets/WMP-1063-commons-text-cve.md) | Vulnerability | CVE remediation with evidence for AppSec |
| [WMP-1077](docs/tickets/WMP-1077-transfer-service-test-coverage.md) | Tech debt | 40% → 80% backend, 21% → 80% frontend coverage |
| [WMP-1080](docs/tickets/WMP-1080-document-costbasis-module.md) | Docs | Documentation + ADR for an orphaned module |

## Layout

```
src/main/java/com/westlake/advisor
├── account/       Account, Position, in-memory AccountRepository (seeded)
├── costbasis/     TaxLot, CostBasisCalculator (FIFO/LIFO/HIGH_COST/AVERAGE_COST)
├── transfer/      TransferService: same-custodian position transfers
├── notification/  AdvisorNotifier (commons-text StringSubstitutor)
└── web/           REST controllers under /api/v1
frontend/          Angular 18 Advisor Workstation (positions table, transfer form)
```

All data is fictional. Account numbers, advisors and tickers do not refer to any real
customer or institution.

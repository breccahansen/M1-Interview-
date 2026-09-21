# WMP-1063 · Remediate CVE-2022-42889 (Apache Commons Text "Text4Shell")

| Field | Value |
|---|---|
| Type | Vulnerability |
| Priority | P1 |
| Component | notification |
| Source | AppSec dependency scan, finding #DS-88213 |
| SLA | 14 days (Critical) |

## Finding

`org.apache.commons:commons-text:1.9` is a direct dependency. Versions before 1.10.0
allow arbitrary code execution when `StringSubstitutor` interpolates untrusted input.
`AdvisorNotifier` uses `StringSubstitutor` to render advisor notifications.

## Required

- [ ] Upgrade to a patched version (≥ 1.10.0).
- [ ] Confirm `AdvisorNotifier` behaviour is unchanged (add a unit test if none exists).
- [ ] Review whether any template value can come from user input; if so, disable
      script/URL/DNS lookups explicitly.
- [ ] Attach evidence to the AppSec finding: dependency tree before/after.

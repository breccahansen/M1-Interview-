# WMP-1090 · Upgrade Advisor Workstation from Angular 14 to Angular 18

| Field | Value |
|---|---|
| Type | Story |
| Priority | P1 |
| Component | frontend |
| Epic | Web Platform Modernization (WMP-1001) |

## Why

Angular 14 left long-term support in November 2023 and no longer receives security
fixes. Enterprise Architecture requires all advisor-facing web apps on an actively
supported Angular line (18+) before the next penetration-test cycle. The app also pins
Node 16, which is itself EOL.

## Scope

- Walk the app forward one major at a time (14 → 15 → 16 → 17 → 18) using `ng update`,
  fixing each version's breaking changes before moving on. Do not skip majors.
- Bump the Node baseline (`.nvmrc`, CI) to a version supported by Angular 18.
- Update TypeScript, RxJS, zone.js and the Karma/Jasmine toolchain to the versions
  Angular 18 requires.
- Adopt the new `@angular-devkit/build-angular:application` builder and the standalone
  bootstrap (`bootstrapApplication`) if the migration schematics offer them cleanly;
  otherwise keep NgModules and note the follow-up.
- Optional, only if low-risk: convert `*ngIf` / `*ngFor` to the built-in control flow
  (`@if` / `@for`) via the official schematic.
- Do **not** change behaviour, routes, API contracts or visuals.

## Acceptance criteria

- [ ] `npm ci && npm run build` succeeds on the new Node baseline.
- [ ] `npm test -- --watch=false --browsers=ChromeHeadless` passes.
- [ ] `ng version` reports Angular 18.x for `@angular/core` and `@angular/cli`.
- [ ] `npm audit --production` shows no high/critical findings introduced by the upgrade.
- [ ] Positions table and transfer form behave identically against the running API.
- [ ] PR description lists each major hop, the breaking changes hit, and how each was
      resolved (for the architecture review board).

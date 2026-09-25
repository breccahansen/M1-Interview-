# Advisor Workstation (frontend)

Angular 18 single-page app used by advisors to view positions and submit same-custodian
transfers against the WMP API. Built with NgModules, `provideHttpClient`, reactive forms and
Karma/Jasmine tests.

```bash
npm ci
npm start            # http://localhost:4200, proxies /api to localhost:8080
npm test -- --watch=false --browsers=ChromeHeadless   # unit tests + coverage (coverage/)
npm run build
```

Node 22 is the baseline (see `.nvmrc`); Angular 18 supports Node ^18.19.1, ^20.11.1 or ^22.

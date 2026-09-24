# Advisor Workstation (frontend)

Angular 14 single-page app used by advisors to view positions and submit same-custodian
transfers against the WMP API. Built with NgModules, `HttpClientModule`, reactive forms and
Karma/Jasmine tests.

```bash
npm ci
npm start            # http://localhost:4200, proxies /api to localhost:8080
npm test -- --watch=false --browsers=ChromeHeadless   # unit tests + coverage (coverage/)
npm run build
```

Node 16 or 18 is required for Angular 14 (see `.nvmrc`).

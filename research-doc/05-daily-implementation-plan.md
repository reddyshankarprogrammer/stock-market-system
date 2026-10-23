# 5. Daily implementation plan (Quantiva): 2 hours on weekdays, 4 hours on weekends

**Question:** Plan the implementation of [04-product-technology-roadmap.md](04-product-technology-roadmap.md) as daily sessions, starting today, with more time at weekends.

## Assumptions

- One person (Shanker, with Claude Code).
- **Weekdays 2 hours, Saturday and Sunday 4 hours each: 18 hours a week.** The 4-hour weekend figure is my assumption; change it and the dates below move.
- Start **Friday 23 October 2026** (today).
- Work is cut into **units of 2 hours**. A weekday holds one unit, a weekend day holds two. The first week has 5 units (Fri 1, Sat 2, Sun 2); every full week has 9.
- Units 1–40 are planned one by one and cover roadmap phases 0–3 (design, identity, landing page, settings, MVP). They finish on **Sunday 22 November 2026**. Later phases are planned in blocks of five units (10 hours), because they depend on what the first 40 units deliver.
- Total scope is about 200 units (400 hours): roughly **22 weeks, finishing late March 2027**, if no day is missed. Public holidays and sick days push the dates; the order does not change.

## Session routine

| Session | Steps |
|---|---|
| Weekday, 2 h | 10 min read yesterday's note and pick the goal · 90 min build · 20 min tests, docs, changelog, commit, two-line "done / next" note |
| Weekend, 4 h | Two units back to back with a 10-minute break between them. Put the harder unit first. Keep the last 15 minutes for a full test run of every touched service and a push |

Rules: a unit that does not finish moves to the next session; do not start a second goal early. Tests of the touched service pass before you stop (`.\mvnw.cmd test` or `npm test`). Follow root `CLAUDE.md` section 5 for changes that span services and section 8 for Kubernetes and CI/CD.

## Calendar for units 1–40

| Date | Hours | Units |
|---|---|---|
| Fri 23 Oct | 2 | 1 |
| Sat 24 · Sun 25 Oct | 4 + 4 | 2–3 · 4–5 |
| Mon 26 – Fri 30 Oct | 2 each | 6, 7, 8, 9, 10 |
| Sat 31 Oct · Sun 1 Nov | 4 + 4 | 11–12 · 13–14 |
| Mon 2 – Fri 6 Nov | 2 each | 15, 16, 17, 18, 19 |
| Sat 7 · Sun 8 Nov | 4 + 4 | 20–21 · 22–23 |
| Mon 9 – Fri 13 Nov | 2 each | 24, 25, 26, 27, 28 |
| Sat 14 · Sun 15 Nov | 4 + 4 | 29–30 · 31–32 |
| Mon 16 – Fri 20 Nov | 2 each | 33, 34, 35, 36, 37 |
| Sat 21 · Sun 22 Nov | 4 + 4 | 38–39 · 40 (then 2 h free: catch-up and MVP demo preparation) |

## Units 1–40

### Phase 0: decisions and design (units 1–5, 23–25 Oct)

| Unit | Goal (2 h) | Done when |
|---|---|---|
| 1 | Decision log: launch market, self-hosted Keycloak, billing provider, passkeys vs PIN (open items in [04](04-product-technology-roadmap.md) section 12). Create the Figma file. | One-page decision log committed; Figma file exists |
| 2 | Design tokens from [QUANTIVA_BRAND_IDENTITY.md](branding/QUANTIVA_BRAND_IDENTITY.md): colours, type scale, spacing, dark and light | Token sheet in Figma |
| 3 | Core components (button, input, card, table, nav) in dark and light | Component sheet in Figma |
| 4 | Landing page wireframe, reusing `Landing page-html/Main.dc.html`; pricing section with the five plans; register, login, forgot password and verify-email screens | Desktop and mobile frames match the field table in doc 4 section 3 |
| 5 | Dashboard, trades table, settings (5 categories) and admin portal wireframes; mobile versions of dashboard and settings. Sign-off | Five main screens approved at phone, tablet and desktop widths |

### Phase 1: Keycloak running locally (units 6–10, 26–30 Oct)

| Unit | Goal (2 h) | Done when |
|---|---|---|
| 6 | Add Keycloak 26 and its database to `docker-compose.yml`; variables in `.env` and `.env.example` | Admin console opens locally |
| 7 | Realm `quantiva`: password policy, email verification, remember me, forgot password; Mailpit container as test mailbox | Test user registers and gets a verification mail |
| 8 | Custom user attributes: mobile (no OTP), gender, country, accepted terms and privacy versions with timestamp | Attributes appear on the token and in the admin console |
| 9 | Identity providers in dev: Google, Microsoft, GitHub; link accounts by verified email | Each of the three logs a test user in |
| 10 | Passkey (WebAuthn) and authenticator-code flows; export the realm JSON to the repo | Passkey registers in a browser; realm export committed |

### Phase 1: gateway and services on Keycloak tokens (units 11–15, 31 Oct – 2 Nov)

| Unit | Goal (2 h) | Done when |
|---|---|---|
| 11 | api-gateway: validate Keycloak tokens (JWKS), still emit `X-User-Id` / `X-User-Role` | Gateway test with a Keycloak-signed token passes |
| 12 | `JwtAuthFilterTest` cases for public, internal and admin paths on the new tokens; role mapping `USER`/`ADMIN` | All gateway tests green |
| 13 | user-service: create or update the local user on first login; new fields added to the `user.registered` map (add only, never rename) | notification-service still consumes the event; user-service tests green |
| 14 | Remove or isolate the old password login path; fix the fallback secrets from root `CLAUDE.md` section 7 | No default `jwt.secret` or DB password left in code |
| 15 | Postman collection updates; run Newman end to end | Collection passes on the new auth |

### Phase 1/2: Keycloak on K3s, web app scaffold (units 16–20, 3–7 Nov)

| Unit | Goal (2 h) | Done when |
|---|---|---|
| 16 | `k8s/keycloak/` manifests, Argo CD application, secret template, `scripts/create-secrets.sh` entry | `kubectl kustomize k8s/keycloak` renders; pushed to a branch |
| 17 | Scaffold `web-app/`: Next.js 16, React 19, TypeScript, Tailwind 4, shadcn/ui, tokens from Figma | App runs; dark and light theme switch |
| 18 | Dockerfile (distroless style), `k8s/web-app/`, Argo CD application, entries in `ci.yml`, `security.yml`, `cd.yml`, `release.yml`, `dependabot.yml` | CI builds the image |
| 19 | Ingress: web app and `/api` on one host (no CORS); `/ws` route for live prices | Browser calls the API from the same origin |
| 20 | Buffer: finish unfinished items from units 6–19 (Saturday, first half) | Identity and scaffold checklist complete |

### Phase 2: landing page (units 21–25, 7–10 Nov)

| Unit | Goal (2 h) | Done when |
|---|---|---|
| 21 | Navigation, hero, call to action, footer | Matches the Figma frame |
| 22 | Feature sections with poster images (clips come later) and animated statistics | All seven feature blocks present |
| 23 | Pricing section for Free, Pro, AI Pro, Quant Pro, Enterprise; FAQ; waitlist form | Waitlist stores an email (backend endpoint behind the gateway) |
| 24 | Hero clip and two feature clips (Screen Studio or Playwright recording); WebM and MP4, lazy loaded, reduced-motion respected | Clips play on scroll |
| 25 | SEO (metadata, sitemap, OpenGraph), PostHog, Lighthouse run | Loads under 2.5 s on a mid-range phone profile |

### Phase 1/3: sign-up and login screens (units 26–30, 11–14 Nov)

| Unit | Goal (2 h) | Done when |
|---|---|---|
| 26 | Auth session: OIDC login against Keycloak, server-side session, httpOnly cookie | Logged-in state survives reload; no token in browser storage |
| 27 | Registration form with zod validation: email, password and confirmation, mobile, gender, country, two consent checkboxes | Invalid input is rejected; a valid form creates the user |
| 28 | Login page: email, password, remember me, forgot password, social buttons (Google, Microsoft, GitHub) | All login paths work |
| 29 | Verify-email page, error states, Cloudflare Turnstile on register and login | Unverified user cannot enter the app |
| 30 | Playwright tests for register, verify, login, logout, forgot password | Tests run in CI |

### Phase 3: app shell and settings (units 31–35, 14–17 Nov)

| Unit | Goal (2 h) | Done when |
|---|---|---|
| 31 | App shell: sidebar on laptop and desktop, bottom navigation on phones; page skeletons | Layout checked at 360, 768, 1280, 1920 and 2560 px |
| 32 | API client: add springdoc to the services that lack it, generate TypeScript types with openapi-typescript | Typed client calls account-service |
| 33 | User-preferences backend (theme, language, notification choices) in user-service; DB default per root rule 7 | GET/PUT endpoints with ownership check and tests |
| 34 | Settings screens: General (theme, language) and Notifications | Choices persist across devices |
| 35 | Settings: Security (change password, two-factor, passkey, sessions, devices, login history from Keycloak) | Sessions listed and can be signed out |

### Phase 3: MVP screens (units 36–40, 18–22 Nov)

| Unit | Goal (2 h) | Done when |
|---|---|---|
| 36 | Settings: Downloads (export format) and About (version, release notes, licence, legal links, contact) | All five categories complete |
| 37 | Accounts list and statement upload (Groww, Zerodha, Binance, Exness) against account-service | A statement imports from the UI |
| 38 | Trades table with filters, sorting and pagination; trade detail | Filters match API results |
| 39 | Dashboard KPIs and P&L calendar | Numbers equal account-service performance output |
| 40 | Portfolio with live prices (ingress `/ws`); responsive pass on all screens; MVP demo | A trader imports a statement and reviews a month on desktop and phone |

## After unit 40: blocks of five units (10 hours)

A block is five units, about 4 calendar days at this pace. Plan each block in detail one block ahead, in the same table format, after reviewing what the previous block delivered.

| Phase | Blocks | Focus per block | Estimated end |
|---|---|---|---|
| 4. Journaling | 9–12 | 9 journal-service skeleton (Flyway, Kafka, gateway route, CI/CD entries) · 10 notes and tags API · 11 screenshots and daily journal UI · 12 playbooks, tests, docs | Mon 7 Dec 2026 |
| 5. Analytics and charts | 13–17 | 13 analytics endpoints (win rate, profit factor, expectancy, drawdown) · 14 reports by symbol, tag, hour, weekday · 15 R-multiple and equity curve · 16 candles endpoint and real price source · 17 trade replay | Sun 27 Dec 2026 |
| 6. AI | 18–23 | 18 ai-service skeleton (FastAPI, Anthropic SDK) · 19 CSV mapping and auto-tagging (Haiku) · 20–21 AI trade coach (Opus) · 22 Kronos forecasts · 23 evaluation on 20 test accounts, privacy disclosure | Tue 19 Jan 2027 |
| 7. Mobile, plans and growth | 24–31 | 24 admin/config service: feature switches and audit log · 25 plans and entitlement checks · 26–27 billing provider and webhooks · 28 PWA and push · 29–31 Expo app with Face ID and fingerprint, store release | Sat 20 Feb 2027 |
| 8. Admin portal and revenue | 32–37 | 32 user management · 33 subscription and broker management · 34 market data and AI model management · 35 audit log screens · 36–37 revenue dashboard | Sun 14 Mar 2027 |
| 9. Hardening | 38–40 | Security review, penetration test, load test, backup and recovery drills, compliance review | Sat 27 Mar 2027 |

## Checkpoints

| After | Date | Check |
|---|---|---|
| Unit 5 | Sun 25 Oct | Design signed off; decisions logged |
| Unit 15 | Mon 2 Nov | Users register and log in through Keycloak; gateway, user-service and Newman green |
| Unit 25 | Tue 10 Nov | Landing page live with waitlist |
| Unit 40 | Sun 22 Nov | MVP demo; revisit launch market, billing provider and price source before phase 4 |

## Risks to the schedule

- **Weekend load:** 8 hours of weekend work after 10 hours on weekdays is heavy. Take one full free day every second week; that adds about one week to the end date.
- Social providers Apple and LinkedIn need external approval; they are left out of units 1–40 and picked up when approved.
- The Keycloak migration (units 11–14) touches the gateway and user-service, the most sensitive code; keep it on a branch until Newman passes.
- Launch-market and billing decisions (unit 1) change blocks 24–27 and the real price source in block 16.
- Only unit 20 is a buffer. Missed days push every later date.

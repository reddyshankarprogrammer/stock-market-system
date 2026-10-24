# web-app (Quantiva landing page and app)

Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4. Frontend for Quantiva (quantiva.ai). It is independent of the Java services: nothing in the other service folders depends on it yet.

Plan: [research-doc/04-product-technology-roadmap.md](../research-doc/04-product-technology-roadmap.md) and [05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md) (units 17–25).

## Run

```powershell
cd web-app
npm install
npm run dev      # http://localhost:3000
npm run build
npm run lint
```

Requires Node 20.9 or newer.

## Brand tokens

Defined once in `app/globals.css`: the six brand colours (navy `#0B1F3A`, electric blue `#146EF5`, growth green `#00C878`, graphite `#1E293B`, silver `#DCE3EA`, white), light and dark roles, one radius (`rounded-brand`) and fonts Inter (body) and Manrope (headlines, `font-display`) loaded in `app/layout.tsx`. Use only these colours; see [branding](../research-doc/branding/QUANTIVA_BRAND_IDENTITY.md).

## Feature clips (Playwright)

Clips are built UI scenes, not screen captures: `app/clips/<name>` (noindex) renders a 1280×720 stage from `components/clips/`, with the timings of [research-doc/07-feature-video-scripts.md](../research-doc/07-feature-video-scripts.md). `scripts/record-clips.mjs` pauses every CSS animation, steps the Web Animations clock frame by frame (30 fps), screenshots each frame with Playwright and encodes `public/video/<name>.webm` (VP9) plus an end-frame `.jpg` poster with Playwright's bundled ffmpeg. Every clip is exactly 8.00 s and about 100–200 KB.

```powershell
npx playwright install chromium     # once
npm run build; npx next start -p 3100   # keep running
npm run clips:record                # in another terminal; CLIPS=calendar renders one clip
```

Clips so far: `calendar`, `import`. The page plays them with `components/FeatureVideo.tsx` (loads on scroll, pause button, still poster for reduced motion).

## Authentication (interim, on user-service)

Onboarding: after registering (and for any account that has not finished it) the dashboard sends the user to `/onboarding`, a four-step wizard (trading experience, brokers, plan of interest, check and complete). Answers are saved as you continue, so a reload resumes at the first unanswered step; "Skip for now" lasts for the browser session. The plan step records interest only: billing and prices do not exist yet. Data cache: the browser uses ONE shared TanStack `QueryClient` (`components/Providers.tsx`), because Next keeps visited pages in memory and a client per layout would let an update (such as "onboarding completed") miss the dashboard's cached profile; login and register clear the cached profile instead of seeding it.

Pages: `/login`, `/register` (all 9 fields, Terms and Risk Disclaimer you must scroll to the end to accept), `/dashboard` (protected placeholder). `/forgot-password` and `/reset-password` work against user-service (the confirmation is identical for every email; the emailed link is single-use and expires in 30 minutes; locally the email arrives in Mailpit at http://localhost:8025). `/verify-email` is an honest "Not available yet" page: there is no verification backend. `/signup` redirects to `/register`.

How it works:

```
browser form (React Hook Form + Zod) → lib/auth/auth-service.ts → /api/auth/* (Next.js route handlers) → api-gateway → user-service
```

- **Tokens never reach browser JavaScript.** The route handlers keep the access and refresh tokens in `httpOnly`, `SameSite=Lax` cookies (`Secure` in production). `lib/auth/auth-store.ts` holds only the profile. `proxy.ts` (Next.js 16 route protection) redirects visitors without a session cookie away from `/dashboard` and `/onboarding`, and signed-in visitors away from `/login` and `/register`.
- **Files:** `lib/auth/auth-service.ts` (browser API), `auth-hooks.ts` (TanStack Query), `auth-store.ts` (Zustand), `schemas.ts` (Zod, shared by browser and server), `server.ts` (gateway calls with an 8 s timeout, error mapping, cookies), `legal-content.ts` (draft Terms and Risk Disclaimer).
- **Configuration:** `API_GATEWAY_URL` (default `http://localhost:8080`), see `.env.example`.
- **Security rules in the route handlers:** same-origin check for state-changing calls, JSON only, wrong password and unknown email give the same message, open redirects blocked (`?next=` must be a same-site path).
- **Sessions renew themselves.** The access cookie lasts 15 minutes. When it expires (or user-service answers 401), `/api/auth/me` exchanges the refresh cookie at `POST /api/v1/users/refresh`, retries once and sets a new access cookie. The refresh cookie lasts 7 days with **Remember me** and ends with the browser session without it. An invalid refresh token ends the session and clears both cookies; a gateway outage during renewal keeps the cookies. `proxy.ts` counts a refresh-only session as signed in.
- **Remember me** also remembers the email on this device (never the password).

Known limits (also in [research-doc/05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md)):
- Mobile, gender, country and the Terms version are forwarded to user-service, which stores them (the server records the acceptance time). The mobile number is not verified. Consent is stored per user, but the legal text itself is a draft. Accounts created before this change have these fields empty.
- There is no rate limiting on the login route; add it at the gateway or ingress.
- The disclaimer text is a draft and needs legal review before launch.

Run and test:

```powershell
npm run build
$env:API_GATEWAY_URL="http://localhost:8099"; npx next start -p 3100   # terminal 1, pointing at the mock gateway
npm run e2e:auth                                                        # terminal 2: 25 browser checks, starts the mock gateway itself
```

**Against the real local stack** (no mocks): with `docker compose up -d` (MySQL, Kafka, Mailpit), user-service on 8081, api-gateway on 8080 and the web app on 3000 (started *without* `API_GATEWAY_URL`), run `npm run e2e:auth:real`. It registers a user, renews the session through the real refresh endpoint, checks that a wrong password and an unknown email look the same, requests a reset, reads the email from Mailpit (http://localhost:8025), resets the password, and proves the old session and the old link stop working. **It creates test users** (`e2e-<timestamp>@example.com`) in your local database and mail in Mailpit; remove them with `DELETE FROM password_reset_tokens WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'e2e-%@example.com'); DELETE FROM users WHERE email LIKE 'e2e-%@example.com';`.

`e2e/mock-gateway.mjs` imitates the documented user-service behaviour (duplicate email 400, unknown email and wrong password both 401, blocked account 403 after the correct password, slow and failing gateways). For real use, run the whole system (`docker compose up -d`, `.\scripts\start-all.ps1`) and leave `API_GATEWAY_URL` at its default.

## App shell and typed API client

- **Shell:** the route group `app/(app)/` wraps `/dashboard`, `/accounts`, `/trades`, `/portfolio` and `/settings` (`/onboarding` stays a focused page without it). From 1024 px up a sidebar (brand, five links, theme, log out); below that a top bar and a fixed bottom navigation (5 items, 56 px high, safe-area aware). One list (`components/shell/nav.ts`) drives both; the current page has `aria-current="page"`. `proxy.ts` protects all five paths. Trades, Portfolio and Settings are honest skeletons that say the screen is planned.
- **Typed client:** `openapi/account-service.json` is the OpenAPI document of account-service (springdoc, `/v3/api-docs`), saved in Git so changes are reviewed. `npm run api:spec:account` fetches it from a running service (gateway-set `X-User-Id` headers are removed, because a browser never sends them); `npm run api:types` generates `lib/api/account-service.d.ts` with openapi-typescript. `lib/api/account-api.ts` wraps it with openapi-fetch, `lib/api/account-hooks.ts` adds TanStack hooks, and a changed endpoint now fails `tsc` instead of the browser. CI fails when the generated types differ from the saved spec.
- **BFF pass-through:** the typed clients call `/api/gw/<gateway path>` (`app/api/gw/[...path]/route.ts`), which adds the session's access token (renewing it from the refresh cookie once, like the other handlers), forwards method, query and body, and relays only status and JSON. Only `api/v1/{accounts,trades,holdings,statements,fees,taxes,charges,dividends,interest}` is reachable; `..` and encoded slashes are refused; writes need a same-origin JSON request; the gateway still checks the token and blocks internal and admin paths.
- **Accounts page:** the first screen on the typed client (read-only list with money formatting, empty, error and signed-out states). Creating an account and statement upload (multipart, not yet allowed through the BFF) come with the MVP screens.
- **Other services:** only account-service has springdoc today. Adding it to the other services (user-service is Boot 3, the rest Boot 4) is still to do.
- **Tests:** `npm run e2e:auth` (87 checks, mock gateway) and `npm run e2e:accounts:real` (8 checks on the real stack: two users, create and list accounts, foreign access 403, traversal, cross-origin, renewal). The real-stack test leaves two users and four accounts behind: delete them as the file header describes.

## Trading terminal (preview, sample data)

`/terminal` is a dark-only, institutional-style dashboard built from the design brief of 24 October 2026 (saved as D6 in `research-doc/06-ai-prompt-library.md`). **Every figure on it is sample data** generated from a fixed seed in `lib/terminal/sample-data.ts` (96 trades, 16 instruments, 5 strategies, fixed "today" 2026-10-24) and the page says so in a banner. Strategies, signals, risk scores and Sharpe ratios have no backend yet, so the real accounts are not used here; the signed-in app (`/dashboard`, `/trades`, `/portfolio`) is the real one and links to the preview.

| Part | What it does |
|---|---|
| Shell | Collapsible sidebar (the choice is remembered) with the 16 destinations of the brief: only Dashboard is a link, the other 15 are marked "Soon" and cannot be activated; below 1024 px a drawer (Escape closes it and returns focus) |
| Filter bar | Asset, market, exchange, date range, portfolio, strategy, risk, search, Clear and Refresh. The filters narrow the trades and **every card, chart and table follows them** |
| KPI cards | Portfolio value, today's P&L, monthly P&L, win rate, open positions, risk score, Sharpe ratio: figure, trend (arrow and sign, not colour alone), sparkline, teal accent line |
| Trading activity | The 12 columns of the brief, status chips, sorting with `aria-sort`, 10 per page, a column menu, and **Export CSV** of exactly the rows and columns on screen (formula-safe: a text cell starting with `=`, `+`, `-` or `@` is neutralised) |
| Analytics and allocation | Equity curve, monthly returns, win/loss ratio, sector performance, and three allocation donuts (sector, asset, geography), drawn with Recharts. Each chart is hidden from assistive technology and has the same numbers as a table; allocation percentages add up to 100 |
| Right panel | Place order (links to the real Trades page), Run screener, Start backtest and Generate AI signal (marked "Soon"); an AI widget whose text is fixed sample text, labelled as such and as not advice |

Palette (exception to the brand colours, only here): background `#080B0F`, teal `#00E5B0`, loss `#FF7A8A`, muted text `#9AA7B4`, field borders `#7B8794` for 3:1 against the black. Tokens are `term-*` in `app/globals.css`. Checks: `npm run test:unit` (8 `node --test` checks of the sample data: determinism, profit and ROI from prices, sector and monthly totals, allocation sums, KPI count and sparklines, filters, CSV) and 8 browser checks, including axe (WCAG 2.1 A and AA) on a desktop and a phone, no sideways scroll at 360, 768, 1024, 1440 and 2560 px, and 44 px controls. axe finds only part of the accessibility problems: a screen-reader and keyboard review is still to do.

Not built: the other pages of the brief (portfolio, market analysis, journal, strategy builder, screener, analytics, risk management, AI assistant), TradingView or ECharts charts, shadcn components, real data.

## Launch list (waitlist)

The landing page has a "Join the launch list" section (`components/WaitlistForm.tsx`, `id="waitlist"`) before the final call to action: email, a consent checkbox that links to the privacy policy, and a button. It posts to `/api/waitlist`, which checks the same Zod schema, answers bots that fill the hidden `company` field with success without storing anything, allows 10 requests a minute per address (per pod, `lib/rate-limit.ts`; 429 with `Retry-After`), and calls the public `POST /api/v1/users/waitlist` of user-service. A new and an already listed email get the same confirmation. The copy says Quantiva is not open yet and promises only what is true: the address is kept with the time of consent and no email is sent. Open: who may read the list and how it is exported, the sending of the launch email (no email provider is chosen), a shared rate limiter, and the privacy policy text that covers the list (the legal pages are drafts).

## MVP screens (accounts, trades, dashboard, portfolio)

All of them read account-service through the typed client and the `/api/gw` pass-through; every figure is shown as the service reports it (formatting only, no recalculation). Which account a screen shows is the `account` in the URL (default: the first), so links can be shared and a reload keeps the place. Data screens share `QueryState`: loading, a clear error with Try again, and a signed-out visitor is sent to `/login?next=`.

| Screen | What it does |
|---|---|
| `/accounts` | List with links, **Open an account** (name, held at, type, currency; tracking accounts are explained as such) |
| `/accounts/[id]` | Summary figures (equity, cash, market value, realised and unrealised profit, fees, taxes); for Groww, Zerodha, Binance and Exness accounts a **statement upload** (CSV, XLS, XLSX up to 20 MB, checked in the browser first), the import result of each statement (imported, already known, skipped, warnings, a failed import with its reason) and delete with a confirmation |
| `/trades` | Table of one account: date filters (sent to account-service when both ends are given, otherwise applied in the page, because the service answers 400 to a lone `from`), symbol and side filters, sortable columns with `aria-sort`, 15 per page, all state in the URL; realised profit of the filtered view. **Record a trade**: at the market price on platform and paper accounts, by hand (symbol, quantity, price, date) on tracking accounts |
| `/trades/[id]` | One trade with gross, fees, taxes, net, realised profit, average cost, source |
| `/dashboard` | Key figures from summary and performance, and a **profit and loss calendar**: for each day the sum of the trades' own `realizedProfit`, month navigation, month total, all-time total. The all-time total equals the account's realised profit (the page says so if it ever does not) |
| `/portfolio` | Open positions with average and current price, market value, unrealised profit in amount and percent; asks for new prices every 10 seconds and has **Refresh prices**; a failed refresh keeps the last prices and says so |

Notes. There is no websocket: "live" means polling account-service, which takes prices from market-data-service. The calendar is built in the browser because account-service has no per-day endpoint; the tests compare it with the API. Colour is never the only signal: profits carry `+` and `-`. Tables scroll inside their own region on phones (keyboard focusable), so the page itself never scrolls sideways. `npm run e2e:mvp:real` (9 checks) is the real-stack walk-through: it registers, opens a Zerodha account, imports a tradebook through the real parser, compares the trades table, filters, dashboard, calendar and portfolio with account-service's own answers, trades on a paper account against live market-data prices, and checks six screens for sideways overflow at 360 px. It leaves a user, two accounts, a statement and its stored file behind (cleanup SQL in `docs/changelog.md`).

## Settings

`/settings` opens General; the category list is a column on laptops and a row of tabs on phones (the row scrolls, the page does not). The browser talks only to `/api/preferences` and `/api/auth/change-password`; user-service owns the data (`user_preferences`, so choices follow the user to other devices).

| Category | What works | Not built (and said so on screen) |
|---|---|---|
| General | Theme System, Light, Dark; applied at once and saved. The account's theme wins at login on a new device (`PreferencesSync`); the theme switch in the shell saves too. Language shows English | More languages |
| Notifications | In-app, push and email switches (real `role="switch"`), saved on each click, put back with an error message if the save fails. Push and email default to off | Push delivery (mobile app) and alert emails do not exist yet; the choice is only stored |
| Security | Change password: current, new (same rule as sign-up) and confirm. A wrong current password (422) leaves the session alone; success replaces the cookies with the fresh tokens, so this device stays signed in and every other session ends. The refresh cookie becomes a session cookie (Remember me is not known here) | Two-factor, passkeys, Face and fingerprint, sessions and devices, login history (need Keycloak): listed as "Not available yet" |
| Downloads | Export format CSV or JSON, saved | Excel and PDF are shown disabled; the download location exists only in desktop and mobile apps, so it is not shown |
| About | Version (from `package.json` at build), legal links, contact | Release notes and licence are marked "Not published yet" |

Notes: the settings queries wait for the profile query, so an expired access cookie is renewed once, not twice in parallel. Radios are updated at once from local state, because React puts a controlled radio back to its old value if state has not changed by the end of the click. `npm run e2e:settings:real` is the real-stack test (8 checks; it leaves one user and one preferences row, deletion SQL in the file header).

## Deployment (Docker, Kubernetes, CI)

- **Image:** `web-app/Dockerfile` (multi-stage; `next build` with `output: "standalone"`; distroless Node 24, uid 65532, no shell). `docker build -t web-app web-app`, then `docker run -p 3000:3000 -e API_GATEWAY_URL=http://host.docker.internal:8080 web-app`. Verified read-only (`--read-only`, only `/tmp` and `/app/.next/cache` writable, all capabilities dropped).
- **Probe:** `GET /api/health` returns `{"status":"ok"}` and never calls the gateway.
- **Cluster:** `k8s/web-app/` (Deployment, Service, ConfigMap, HPA 2 to 5, PDB, ServiceAccount; no Secret), ingress `https://app.stockmarket.local` in `k8s/ingress/web-app-ingress.yaml`, NetworkPolicy `allow-ingress-controller-to-web-app`, Argo CD application `web-app`, Grafana board "Stock Market / Web App" (container metrics and logs: Next.js has no Prometheus endpoint).
- **CI:** job `web-app` in `ci.yml` (lint, type check, build, browser tests on the standalone server); the image job, CD, release and Dependabot (npm and docker) include it.
- **Behind the ingress:** the same-origin check in `sameOriginOnly` compares the `Origin` host with `X-Forwarded-Host` or `Host`, because the server sees `http` while the browser sends `https`. Cookies are `Secure` in production, so use HTTPS (localhost is exempt).
- **Open:** the public production address (the host name above is the local-cluster name); not yet run in a real cluster.

## Not done yet

Dockerfile, `k8s/web-app/`, Argo CD application and the CI/CD service lists (root `CLAUDE.md` section 8.6) are planned for unit 18. Landing page sections start in unit 21.

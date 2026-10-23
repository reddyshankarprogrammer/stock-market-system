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

## Deployment (Docker, Kubernetes, CI)

- **Image:** `web-app/Dockerfile` (multi-stage; `next build` with `output: "standalone"`; distroless Node 24, uid 65532, no shell). `docker build -t web-app web-app`, then `docker run -p 3000:3000 -e API_GATEWAY_URL=http://host.docker.internal:8080 web-app`. Verified read-only (`--read-only`, only `/tmp` and `/app/.next/cache` writable, all capabilities dropped).
- **Probe:** `GET /api/health` returns `{"status":"ok"}` and never calls the gateway.
- **Cluster:** `k8s/web-app/` (Deployment, Service, ConfigMap, HPA 2 to 5, PDB, ServiceAccount; no Secret), ingress `https://app.stockmarket.local` in `k8s/ingress/web-app-ingress.yaml`, NetworkPolicy `allow-ingress-controller-to-web-app`, Argo CD application `web-app`, Grafana board "Stock Market / Web App" (container metrics and logs: Next.js has no Prometheus endpoint).
- **CI:** job `web-app` in `ci.yml` (lint, type check, build, browser tests on the standalone server); the image job, CD, release and Dependabot (npm and docker) include it.
- **Behind the ingress:** the same-origin check in `sameOriginOnly` compares the `Origin` host with `X-Forwarded-Host` or `Host`, because the server sees `http` while the browser sends `https`. Cookies are `Secure` in production, so use HTTPS (localhost is exempt).
- **Open:** the public production address (the host name above is the local-cluster name); not yet run in a real cluster.

## Not done yet

Dockerfile, `k8s/web-app/`, Argo CD application and the CI/CD service lists (root `CLAUDE.md` section 8.6) are planned for unit 18. Landing page sections start in unit 21.

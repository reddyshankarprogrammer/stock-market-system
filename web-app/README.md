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

Pages: `/login`, `/register` (all 9 fields, Terms and Risk Disclaimer you must scroll to the end to accept), `/dashboard` (protected placeholder). `/forgot-password`, `/reset-password` and `/verify-email` are honest "Not available yet" pages: user-service has no such endpoints. `/signup` redirects to `/register`.

How it works:

```
browser form (React Hook Form + Zod) → lib/auth/auth-service.ts → /api/auth/* (Next.js route handlers) → api-gateway → user-service
```

- **Tokens never reach browser JavaScript.** The route handlers keep the access and refresh tokens in `httpOnly`, `SameSite=Lax` cookies (`Secure` in production). `lib/auth/auth-store.ts` holds only the profile. `proxy.ts` (Next.js 16 route protection) redirects visitors without a session cookie away from `/dashboard` and `/onboarding`, and signed-in visitors away from `/login` and `/register`.
- **Files:** `lib/auth/auth-service.ts` (browser API), `auth-hooks.ts` (TanStack Query), `auth-store.ts` (Zustand), `schemas.ts` (Zod, shared by browser and server), `server.ts` (gateway calls with an 8 s timeout, error mapping, cookies), `legal-content.ts` (draft Terms and Risk Disclaimer).
- **Configuration:** `API_GATEWAY_URL` (default `http://localhost:8080`), see `.env.example`.
- **Security rules in the route handlers:** same-origin check for state-changing calls, JSON only, wrong password and unknown email give the same message, open redirects blocked (`?next=` must be a same-site path).
- **Remember me** remembers the email on this device; with the refresh endpoint (not built yet) it will also keep the session longer.

Known limits (also in [research-doc/05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md)):
- Sessions last 15 minutes: user-service has no refresh endpoint.
- Mobile, gender, country and the Terms acceptance are validated but **not saved**: user-service does not store them yet. The dashboard says so after registering.
- There is no rate limiting on the login route; add it at the gateway or ingress.
- The disclaimer text is a draft and needs legal review before launch.

Run and test:

```powershell
npm run build
$env:API_GATEWAY_URL="http://localhost:8099"; npx next start -p 3100   # terminal 1, pointing at the mock gateway
npm run e2e:auth                                                        # terminal 2: 25 browser checks, starts the mock gateway itself
```

`e2e/mock-gateway.mjs` imitates the documented user-service behaviour (duplicate email 400, unknown email 404, wrong password 401, slow and failing gateways). For real use, run the whole system (`docker compose up -d`, `.\scripts\start-all.ps1`) and leave `API_GATEWAY_URL` at its default.

## Not done yet

Dockerfile, `k8s/web-app/`, Argo CD application and the CI/CD service lists (root `CLAUDE.md` section 8.6) are planned for unit 18. Landing page sections start in unit 21.

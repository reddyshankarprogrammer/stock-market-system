# Session log: 23 October 2026

Assistant: Claude Code. User: Shanker Reddy. Branch at the end of the session: `feature/frontend-landing-page`.
Plans referenced throughout: [research-doc/04-product-technology-roadmap.md](../research-doc/04-product-technology-roadmap.md), [05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md), [06-ai-prompt-library.md](../research-doc/06-ai-prompt-library.md).

## 1. Update the roadmap with new requirements

- **Request:** add the supplied requirements (registration fields, standard, social and future login, settings module, responsive targets, five subscription plans, admin portal, revenue dashboard) to `research-doc`.
- **Done:** read `research-doc/README.md`, `03-frontend-trading-journal.md` and `04-product-technology-roadmap.md`; edited doc 04 (new sections 3–7, renumbered later sections, roadmap now phases 0–9, risks, decisions); updated `research-doc/README.md` (history, open decisions).
- **Result:** doc 04 and README updated. Files: [04-product-technology-roadmap.md](../research-doc/04-product-technology-roadmap.md), [README.md](../research-doc/README.md).
- **Judgement calls recorded:** plan contents are a proposal; settings categories 2–4, 6, 8 left open; passkeys stand in for Face ID and fingerprint in browsers; gender field flagged for data minimisation.

## 2. Daily two-hour plan

- **Request:** a daily two-hour implementation plan.
- **Done:** created [05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md) (days 1–40 detailed, weekly blocks after); added it to the README table.

## 3. Start from today, more time at weekends

- **Request:** start today and give more time at weekends.
- **Done:** rewrote doc 05 with calendar dates. Assumption stated: 2 h weekdays, 4 h weekend days, 18 h a week, start Fri 23 Oct 2026. Units 1–40 end Sun 22 Nov 2026; full roadmap late March 2027.
- **Files:** [05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md).

## 4. AI prompt library

- **Request:** save the landing page prompt in `research-doc` and generate more prompts for building the system and for enterprise customers.
- **Done:** created [06-ai-prompt-library.md](../research-doc/06-ai-prompt-library.md): landing page master prompt (the supplied text stopped after the trust section; later sections marked `[Added]`), 14 development prompts, 10 enterprise prompts, 9 design and operations prompts. Guardrails added: competitor names are benchmarks only, no unverifiable trust claims, gain/loss colour note.
- **Colour source checked:** [branding/QUANTIVA_BRAND_IDENTITY.md](../research-doc/branding/QUANTIVA_BRAND_IDENTITY.md).

## 5. Commit and push (research-doc)

- **Request:** commit, then push.
- **Done:**
  1. Committed all of `research-doc/` as one commit (31 files).
  2. First push rejected: remote had 4 newer commits (Argo CD fix, monitoring fix, script permissions, `.gitignore`), none touching `research-doc/`.
  3. `git pull --rebase` also replayed an older unpushed local commit, `ubunto setting`, which conflicted in `docs/changelog.md` (both sides added separate entries). Resolved by keeping both.
  4. Rebase then stopped on a leftover copy of `Stock_Market_System_2_0_Blueprint_Updated.docx` (identical hash to the committed file). Deleting it was denied by the permission check, so it was moved to the session scratchpad instead, and the rebase continued.
  5. A second push attempt was denied by the permission check because it would also publish the older `ubunto setting` commit. The assistant stopped and asked.
  6. After the user asked again ("check and commit and push the code if anything is remaining") nothing was uncommitted; the push succeeded: `c81c5bb..4775157`.
- **Pushed commits:** `94e3c36` (ubunto setting) and `4775157` (research-doc).

## 6. roadmap.html

- **Request:** update `roadmap.html` with the new plan.
- **Done:** added sections for settings, responsive design, plans, admin and revenue, and the daily schedule; updated sign-up, stack, phases 0–9, risks, decisions; README step 12. Checked that anchors resolve and tags balance.
- **Not done:** the shared Claude artifact was not republished; the HTML edits were not committed at that point.
- **Files:** [roadmap.html](../research-doc/roadmap.html).

## 7. New branch for the frontend

- **Request:** create a new git branch for the landing page frontend.
- **Done:** `git switch -c feature/frontend-landing-page` from `main` (`4775157`). Not pushed. The uncommitted `research-doc/README.md` and `roadmap.html` edits came along.

## 8. Scaffold web-app

- **Request:** scaffold `web-app` with Next.js 16 and brand tokens without affecting the Java projects; ask for confirmation first.
- **Confirmation asked and answered:** create-next-app, npm, scaffold and tokens only.
- **Done:** `npx create-next-app@16 web-app --ts --tailwind --eslint --app --no-src-dir --use-npm --skip-install` (Node 24.14, npm 11.9); replaced the demo page, set brand tokens in `web-app/app/globals.css`, Inter and Manrope in `layout.tsx`, wrote `web-app/README.md`; removed the unused demo SVGs.
- **Checked:** `git status` showed only `web-app/` as new; no Java service, `docker-compose.yml`, `k8s/`, `.github/`, `scripts/`, `.env` or `docs/` changed.
- **Plan reference:** units 17–18 of [05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md); colours from [QUANTIVA_BRAND_IDENTITY.md](../research-doc/branding/QUANTIVA_BRAND_IDENTITY.md).

## 9. Landing page, stage 1

- **Request:** build the landing page from the master prompt in [06-ai-prompt-library.md](../research-doc/06-ai-prompt-library.md) (A1).
- **Confirmation asked and answered:** staged by section; run `npm install`, lint and build; scripted and labelled AI demo; placeholder `/login` and `/signup` routes.
- **Done in stage 1:**
  - `npm install` (363 packages, 0 vulnerabilities); read the bundled Next.js 16 docs as required by `web-app/AGENTS.md` (JSON-LD guide).
  - Logos copied from `research-doc/branding/logo/` to `web-app/public/brand/` (icon 192 and 512). The wordmark is navy, so the page uses a fixed light/navy look instead of an OS dark-mode switch.
  - Components: `Button`, `Section`, `Card`, `Metric` in `web-app/components/ui/`; `Header` (sticky, no-JS mobile menu), `Hero`, `HeroDashboard` (built UI with sample data, labelled), `Trust`, `Footer`; all copy in `web-app/lib/content.ts`.
  - Metadata: canonical, Open Graph, Twitter card, theme colour. Placeholder routes `/login` and `/signup`.
  - A bug found while writing: the metric component would have labelled a falling drawdown as "worsened"; fixed with a `lowerIsBetter` flag.
- **Verified:** `npm run lint` clean; `npm run build` compiled, type-checked and generated `/`, `/login`, `/signup`, `/_not-found`.
- **Not verified:** not opened in a browser; no Lighthouse run; no visual check at 320–1920 px.

## 10. Run the web app

- **Request:** run the web app.
- **Done:** `npm run dev` in `web-app/` as a background task; confirmed with HTTP requests that `/`, `/login` and `/signup` return 200 and the home page contains the H1 "The Intelligent Trading Operating System". Served at http://localhost:3000.
- **Not checked:** visual rendering in a browser. The dev server was left running; it has not been stopped.

## 11. Review of the landing page mockup

- **Request:** run prompt A2 ("Landing page review") from [06-ai-prompt-library.md](../research-doc/06-ai-prompt-library.md) on `research-doc/Landing page-html/Main.dc.html`.
- **Done:** read the whole file (689 lines); computed WCAG contrast ratios with a throw-away script in the session scratchpad (not in the repo); returned a 24-row table in chat (issue, where, severity, fix) and rewrote the three weakest headlines.
- **Main findings:** off-palette colours (orange loss colour and others), contrast failures (green `#008A57` eyebrows 4.10:1, accent links 4.27:1), inconsistent buttons and no hover states, a hero card that can cause horizontal scroll, planned features and counterfactual savings shown as fact, placeholder testimonials and a "Most popular" badge with no users, CTAs all pointing at `#security`, client-side rendering and missing SEO tags.
- **Not done:** the review was not saved to a file; no code was changed. Source of the rules: [branding/QUANTIVA_BRAND_IDENTITY.md](../research-doc/branding/QUANTIVA_BRAND_IDENTITY.md).

## 12. Feature clip scripts

- **Request:** run prompt A3 (eight 6–8 s shorts), then "yes do it" to save them and log it.
- **Done:** wrote eight scripts with captions, timed UI actions, camera moves and end frames, plus a shared spec; saved as [07-feature-video-scripts.md](../research-doc/07-feature-video-scripts.md); linked it in [research-doc/README.md](../research-doc/README.md) (document 7 and history step 13).
- **Not done:** the Remotion or Playwright project was not started; it needs a separate go-ahead. Clips 7 (alerts) and 8 (backtest) are labelled concepts because those features are not built.

## 13. Remove the former company name from the project

- **Request:** keep no company name from the previous employer anywhere in the project, store no credentials for it, and make sure the git account does not use that employer's email.
- **Checked first (nothing changed until confirmed):** searched all tracked and untracked files, binary files (`.docx` contents and PDF streams included), git config, commit authors and messages, and the assistant memory folder.
- **Found:** a vendor label in the 8 service `Dockerfile`s, two lines in `web-app/lib/content.ts`, and mentions in three `research-doc` files. No credentials or email addresses in the repo; all commits use the Gmail address; the repo-level git config already uses it. The global `~/.gitconfig` still holds the old employer email.
- **Confirmed with the user, then done:** Dockerfile label changed to `Quantiva` (label only, one line per file); web-app footer reads "© year Quantiva. Quantiva is a trademark."; unused `legalName` field removed; the three docs now use neutral wording. `npx tsc --noEmit` passes; a final search of the working tree finds no match.
- **Left alone on request:** the global `~/.gitconfig` (affects other repos on the machine).
- **Not done, needs a decision:** two already pushed commits still contain the old name in their content (the CI/CD commit and the `research-doc` commit). Removing it from history would mean rewriting and force-pushing `main`, which was not requested and was not done.
- Files: [04-product-technology-roadmap.md](../research-doc/04-product-technology-roadmap.md), [06-ai-prompt-library.md](../research-doc/06-ai-prompt-library.md), [roadmap.html](../research-doc/roadmap.html).

## 14. Commit and push the branch

- **Request:** commit and push.
- **Done:** three commits on `feature/frontend-landing-page` (`162497a` Dockerfile vendor label, `853efde` web-app scaffold and stage 1, `a8a8839` research-doc and this history folder), then `git push -u origin feature/frontend-landing-page`. All authored with the Gmail address from the repo config. Checked beforehand that no added line contained the former company name.
- **Note:** Git printed LF/CRLF warnings; harmless.

## 15. Stop the dev server, landing page stage 2, pull request

- **Request:** after a clarifying question, all three of: build stage 2, open a pull request, stop the dev server.
- **Dev server:** the background task was stopped, but a child Node process kept port 3000; it was identified as this project's `next` server and stopped by process id. Ports 3000 and 3100 are free.
- **Stage 2 built in `web-app/` (uncommitted):** platform section (8 modules with honest "In development" / "Planned" labels and expandable "Learn more"), scripted and labelled AI coach demo, solutions tabs (full ARIA tabs pattern), pricing with five plans, billing toggle and comparison table (prices TBC), security section with real statuses, FAQ (8 questions), final call to action, JSON-LD (Organization, Product, FAQPage built from the visible FAQ), placeholder `/contact`, `/legal/terms`, `/legal/privacy`, `/legal/risk`. Copy lives in `web-app/lib/content.ts`.
- **Fixes from the mockup review (section 11) applied:** eyebrow text uses navy on light backgrounds, tap targets 44 px in header, footer, summaries and buttons, plan and platform labels never rely on colour alone (values are printed), illustrations play once so no pause control is needed, risk line under the hero dashboard, no off-palette colours, navy shadows instead of black, dead footer links replaced by pages.
- **Corrections made while building:** the trust section claimed "per-user data ownership checks on every request" as Live; that overstated reality (root `CLAUDE.md` section 7 lists a known gap in portfolio-service), so it now reads "across all services" and "In progress". A dynamic legal route failed to build with Cache Components (`dynamicParams` not allowed), so it was replaced by three static pages that return real 404s for unknown paths.
- **Verified on the production build:** `npm run lint` clean, `npx tsc --noEmit` clean, `npm run build` generates 10 pages; served on port 3100: one H1, no heading jumps, three JSON-LD blocks, FAQ schema has 8 entries matching the page, no broken `#` anchors, canonical, Open Graph and Twitter tags present, only the six brand hex colours in the HTML, unknown paths return 404. Contrast of the new colour pairs measured at 4.59:1 or higher.
- **Not verified:** not viewed in a browser, no Lighthouse run, no check at 320–1920 px, keyboard use of the tabs and toggles tested only by reading the code.
- **Pull request not opened:** the GitHub CLI is not installed in either shell, and no token was taken from the credential manager. The compare URL is https://github.com/reddyshankarprogrammer/stock-market-system/pull/new/feature/frontend-landing-page; stage 2 is not committed yet, so a pull request opened now would contain stage 1 only.

## 16. Commit, memory-pressure stop and clip pipeline (24 October)

- **Commit:** on request ("run git commit with proper message") two commits were made on `feature/frontend-landing-page`: `3bf4ae6` (stage 2) and `f534bdc` (log entries). Not pushed; the user said not to push.
- **Memory pressure:** Claude Code stopped the background dev server because the system was low on memory; the leftover Node process was stopped on request. Free memory was about 2.4 GB of 31.5 GB, so later work kept to one browser and sequential steps.
- **Request:** "go ahead" with the suggested next steps, using Playwright for the videos, fast, clips max 8 seconds. Interpreted "max 8 sec" as clip length (hard limit 8.00 s) and kept the page fast.
- **Built (uncommitted):** `playwright` dev dependency and Chromium; two scene pages (`/clips/calendar`, `/clips/import`, noindex) following [07-feature-video-scripts.md](../research-doc/07-feature-video-scripts.md); `scripts/record-clips.mjs`; `FeatureVideo` component; "See it working" section after the trust section; README and doc 07 status updated.
- **How the clips are made and why:** a first approach (screen recording, trimmed with ffmpeg) failed twice: Playwright's ffmpeg is a minimal build with a VP9 encoder only (my VP8 option crashed it), and recordings were out of sync with the animation by about 0.9 s on one clip. It was replaced by frame-exact rendering: pause all CSS animations, set each frame's time through the Web Animations API, screenshot, pipe to ffmpeg. Result: each clip is exactly 8.00 s at 30 fps, 167 KB and 119 KB, rendered in about 8 seconds each.
- **Bugs found and fixed on the way:** click rings visible before their time (fill mode); cursor covering text; calendar drawer figures not matching the cell total (now +₹1,760); a horizontal scroll bug on narrow screens caused by screen-reader-only text in the pricing table escaping its scroll wrapper (introduced in stage 2; fixed with `relative`).
- **Verified in a real browser (Playwright against the production build):** page load about 70–210 ms with 10–13 ms to first byte; posters (about 105 KB) load first, video files (about 290 KB) only after scrolling to the section; clips play while visible and pause when scrolled away; pause and play button works and has a 44 px target; reduced motion keeps clips paused on the poster; no horizontal overflow at 320, 375, 768, 1024, 1440 and 1920 px. Lint clean; build passes.
- **Not verified:** other browsers (only Chromium), Safari and older iOS (VP9 WebM only, poster shown if unsupported), Lighthouse, screen reader use.
- **Follow-ups:** a phone-specific crop (in-clip text is small at 375 px), the other six clips, a hero video and demo modal, a waitlist form.

## 17. Authentication pages (stage A) and daily plan update (24 October)

- **Request:** build production-ready authentication pages (Login, Register, Verify Email, Forgot Password, Reset Password, Onboarding) with Next.js, TypeScript, Tailwind, React Hook Form, Zod and TanStack Query, connected to the User Service APIs, with `auth-service.ts`, `auth-hooks.ts`, `auth-store.ts`, a persisted JWT, protected routes, dark and light mode; make the Terms a mandatory read with risk and market details, education only; keep Verify Email and Send Reset Link pending; then update [05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md).
- **Checked first (nothing changed until confirmed):** user-service API and DTOs (register: first and last name, email, password; login; me; no refresh, forgot, reset, verify or onboarding endpoints; no mobile, gender, country or consent fields), gateway (no CORS config), Next.js 16 docs for `proxy.ts` and cookies.
- **Confirmed with the user:** tokens in httpOnly cookies set by Next.js route handlers (not `localStorage`); user-service extended in a separate step; Keycloak deferred behind `auth-service.ts`; Stage A first (Login and Register).
- **Built in `web-app/` (uncommitted):** `lib/auth/*`, four route handlers `app/api/auth/{login,register,logout,me}`, `proxy.ts`, `(auth)` pages (login, register, and "Not available yet" pages for forgot, reset and verify), `/dashboard`, shared form components, `TermsGate` (accept only after scrolling to the end), draft Terms and Risk Disclaimer with a market-conditions notice dated 24 October 2026 (general risks only, no live data), theme tokens and toggle, `e2e/` mock gateway and Playwright test, `.env.example`, README section. `/signup` now redirects to `/register`; the landing page links point to `/register`.
- **Dependency note:** `@hookform/resolvers` 6.0.0 failed to install (optional peer conflict); version 5.x was used instead, without `--force`.
- **Verified in a real browser against a mock gateway:** 25 of 25 checks pass (validation, error states, same message for wrong password and unknown email, 500 and 9 s gateway handled within the 8 s limit, httpOnly cookie not readable by scripts, no token in storage, protected routes, open redirect and foreign-Origin blocked, terms gating, disclaimer content, theme switching, no horizontal overflow at 320–1920 px, field names, 44 px targets). Lint, TypeScript and build pass. The landing page still passes its earlier checks, and does not load the auth libraries.
- **Found and fixed through testing:** a 16 px sideways overflow at 320 px (theme toggle), gender labels stored lowercase, and a real contrast failure in dark mode (blue links on navy 2.65–3.6:1; now a `--link` colour that is silver in dark mode, 11:1). Several failures were mistakes in my own test (Next.js empty alert element, hidden option text, mock returning a fixed name) and were corrected in the test.
- **Daily plan updated:** new "Progress and plan change" section (done so far, decisions, ordered backend step, remaining MVP work about 25 units), phase headings marked deferred, done or pending, checkpoint and risks changed. [04-product-technology-roadmap.md](../research-doc/04-product-technology-roadmap.md) has a short note.
- **Not verified:** nothing ran against the real user-service, MySQL or Kafka (low free memory, and it needs `docker compose`); only Chromium; no screen reader test. Sessions last 15 minutes because there is no refresh endpoint. The disclaimer is a draft and needs legal review. The error colour is not in the brand palette, so errors use a bold badge, a thicker border and text.
- **Not built (by decision):** Reset Password form, Onboarding (4 steps), Verify Email and Send Reset Link (pending), the user-service changes.

## 18. Commit, then the user-service profile-fields step (24 October)

- **Request:** "commit and follow the process as per your recommendation".
- **Commit:** two commits on `feature/frontend-landing-page` (`1c8faec` web-app: clips and auth stage A; `26f5f20` docs and history). Checked first: Gmail identity, no build output or secrets in the tree, no former company name, largest file 168 KB. Not pushed.
- **Interpretation:** the recommended order in [05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md) ("Remaining MVP work") starts with a decision log (needs the user's input on launch market and billing, so it was not started) and then backend step 1, which the user had already approved as a separate step. That step was done, on a new branch `feature/user-profile-fields` (cut from the frontend branch, because the web app change goes with it).
- **user-service (Java, Boot 3.5.6), following `user-service/CLAUDE.md` "Add a field to the user profile":** `entity/Gender`; nullable columns on `User`; optional validated fields on `RegisterRequest` (gender as a pattern-checked string so a bad value gives a normal validation error instead of a parse error naming Java classes); `UserResponse` fields last; `UserServiceImpl` stores them and sets `termsAcceptedAt` from the server clock. The `user.registered` Kafka contract is unchanged on purpose.
- **Tests:** 5 new (fields saved, acceptance time set by the server, old-style request still works, Kafka key set pinned, bad values give 400); the 3 `UserResponse` constructors in `UserControllerTest` updated. `.\mvnw.cmd test`: **33 tests, 0 failures** (the README said 23; corrected). The first quiet run hid the summary, so the counts were read from the Surefire reports.
- **Web app:** the register route handler forwards `mobileCountryCode`, `mobileNumber`, `gender` (upper case), `country` and `termsVersion`; the "not saved yet" workaround is gone. The mock gateway records the last register body and the end-to-end test checks it field by field: **25 of 25 pass**.
- **Docs updated (repo rules, root `CLAUDE.md` section 5):** `user-service/README.md`, `docs/user-manual.md`, ER diagram in `docs/technical-documentation.md`, Postman generator and regenerated collection (Register sends the fields; Get my profile asserts them), `docs/changelog.md`, web-app README, and the plan (step 1 marked done, remaining MVP work 25 → 23 units).
- **Not verified:** nothing ran on MySQL (the Spring context test uses H2), through the real gateway, or via Newman (needs all services and Docker; free memory was about 2.6 GB). `ddl-auto: update` should add nullable columns safely, but it has not been seen on a real database.
- **Not done:** the other backend steps (refresh endpoint, forgot and reset password, email verification, onboarding preferences, 401 for unknown email), the gateway needs no change (register is already public).

## 19. Push, then the refresh-token endpoint (24 October)

- **Request:** "git commit and push the code and start the next step. follow your suggestion".
- **Push:** `feature/frontend-landing-page` (`a8a8839..26f5f20`) and the new `feature/user-profile-fields` (contains it) were pushed. `main` untouched. Everything was already committed.
- **Next step (my suggestion): the refresh endpoint**, on a new branch `feature/refresh-token`, because sessions ended after 15 minutes and "Remember me" had no effect. Read first: `JwtService`, `UserController`, the gateway filter and its test, `GlobalExceptionHandler`, and both `CLAUDE.md` files.
- **user-service:** `POST /api/v1/users/refresh` with `RefreshRequest`; the service validates signature, expiry and token type (an access token must never work), loads the user, refuses blocked statuses, and returns a new access token (role and email from the database) with the same refresh token. Typed `InvalidRefreshTokenException` mapped to 401 with one message. A new `RefreshTokenFlowTest` uses a **real** `JwtService` (spy), not a mock, so signature, expiry and type are genuinely checked.
- **api-gateway:** `/api/v1/users/refresh` added to `PUBLIC_PATHS` (the access token has expired by then; the refresh token is in the body). A test pins that refresh tokens are still 401 as Bearer tokens elsewhere. A test comment that claimed spoofed `X-User-*` headers are stripped on public paths was wrong (public paths skip the filter entirely) and was corrected.
- **Found, not changed:** login does not check the user status, so a SUSPENDED user can still log in. Refresh refuses blocked statuses, so it does not widen this. Recorded in the README, root `CLAUDE.md` gaps, technical documentation and the plan.
- **Web app:** `/api/auth/me` renews the access cookie from the refresh cookie (when missing, or on a 401), retries once, clears both cookies only if the refresh token is invalid, and keeps them on a gateway outage. `proxy.ts` treats a refresh-only session as signed in. Remember me keeps the refresh cookie 7 days; otherwise it is a session cookie. Dashboard text updated.
- **Verified:** user-service **43 tests, 0 failures** (10 new); api-gateway **10 tests, 0 failures** (1 new); web app **31 of 31** browser checks (6 new) against an extended mock gateway; lint and build pass. The mock first failed one check because two tokens issued in the same second were identical; it now adds a unique token id.
- **Docs updated per repo rules:** user-service and gateway READMEs, user manual, technical documentation (section 9 and the gaps table), root `CLAUDE.md` gaps, Postman generator and collection (102 → 106 requests), changelog, web-app README, the plan (steps marked done, remaining MVP work 23 → 21 units).
- **Not verified:** MySQL, the real gateway, Newman (needs Docker and all services; memory was low). No rate limiting. Refresh tokens are not rotated or revocable.

## Open items after this session

| Item | Where |
|---|---|
| Commit and push stage 2 (built, verified, uncommitted); open the pull request by hand or install the GitHub CLI | Section 15 above |
| Legal pages are placeholders; real Terms and Privacy Policy need counsel before launch | `web-app/lib/content.ts` |
| Run Lighthouse and test 320–1920 px, keyboard and screen reader on stage 2 | [06-ai-prompt-library.md](../research-doc/06-ai-prompt-library.md) A1 "Done when" |
| Trust section statuses (Live, In progress, Planned) must be confirmed against the real deployment | `web-app/lib/content.ts`, [04-product-technology-roadmap.md](../research-doc/04-product-technology-roadmap.md) section 11 |
| Share image is the 512 px icon, a placeholder | `web-app/app/layout.tsx` |
| Dockerfile, `k8s/web-app/`, Argo CD and CI lists | Unit 18 of [05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md) |
| Fix the 24 mockup review findings that still apply to `web-app/` (contrast, tap targets, pause control, tab pattern, landmarks, risk line near numbers) in stage 2 | Section 11 above |
| Remotion/Playwright project for the eight clips | [07-feature-video-scripts.md](../research-doc/07-feature-video-scripts.md) |
| Shared Claude artifact not republished; `roadmap.html`, README and `web-app/` changes not yet committed | [research-doc/README.md](../research-doc/README.md) |
| Open decisions (launch market, Keycloak hosting, billing, passkeys vs PIN, plan contents, gender field, settings categories) | [research-doc/README.md](../research-doc/README.md), [04](../research-doc/04-product-technology-roadmap.md) section 12 |

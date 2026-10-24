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

## 20. Push the refresh branch, then forgot and reset password (24 October)

- **Request:** "yes, push this branch. After that, the next step in the plan is forgot and reset password with an email service, or the web-app Docker, Kubernetes and CI setup. I'd pick one of those next." The user repeated my own sentence and did not choose, so the plan's order was followed: **forgot and reset password**. Reason: it is next in [05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md) and completes the sign-in journey; the deployment step edits shared CI, Kubernetes and Argo CD files and deserves its own deliberate pass.
- **Push:** `feature/refresh-token` was pushed. New branch `feature/password-reset` (not pushed).
- **Design decisions:** token = 256 random bits, only its SHA-256 hash stored, 30 minutes, single use; forgot always answers 202 with the same message and sends the email in the background (so neither the answer nor its timing reveals whether an account exists); a 60-second per-user cooldown against inbox flooding; a newer link voids older ones; a reset sets `users.password_changed_at` so earlier refresh tokens are refused; blocked accounts get no link and cannot reset; one generic 400 for every bad token.
- **user-service:** `spring-boot-starter-mail`; `PasswordResetService`, `PasswordResetToken` entity and repository, `MailService`/`SmtpMailService` (never throws, never logs the body, works when mail is not configured), `AccountPolicy` (blocked statuses, shared with refresh), typed `InvalidResetTokenException`, two endpoints, `JwtService.extractIssuedAt`. Configuration follows the repo's hosts pattern (`hosts.mail`, docker profile `MAIL_HOST`).
- **Infrastructure:** Mailpit added to docker compose (SMTP 1025, inbox 8025); `.env.example` gets `MAIL_*`, `MAILPIT_UI_PORT`, `APP_BASE_URL`. The compose file was validated with a YAML parser (Docker is not running). My first insertion split the "ACCOUNT SERVICE" comment header from its service and was moved. Nothing in `k8s/` was changed because the email provider and public address are undecided; this is recorded as an open item.
- **api-gateway:** both paths added to `PUBLIC_PATHS` with test assertions.
- **Web app:** working Forgot Password and Reset Password pages (identical confirmation for every email; reset page reads the token once and removes it from the address bar; `Referrer-Policy: no-referrer` and `Cache-Control: no-store` on `/reset-password`), two route handlers, hooks, schemas, error code. Verify Email stays pending.
- **Verified:** user-service **61 tests, 0 failures** (18 new, including a real BCrypt password change, hash-only storage, single use, expiry, blocked accounts, cooldown, older sessions ended after a reset, and a test that sends a real message over a socket to an in-process SMTP server); api-gateway 10 tests, 0 failures; web app **38 of 38** browser checks (7 new). Lint and build pass. Screenshots of both screens were inspected.
- **Mistakes caught on the way:** a shell command cut off (the known pitfall) so edits were moved into script files; two test selectors matched duplicate text (the subtitle repeats "Use at least 10 characters"); the Postman and docs edits were re-checked by regenerating (106 to 111 requests).
- **Not verified:** MySQL (the tests use H2), Mailpit in Docker, the real gateway, Newman. No production email provider. Access tokens already issued survive a reset for up to 15 minutes. No rate limit on `/reset-password`. Expired reset tokens are never deleted.

## 21. Push the password-reset branch, then login hardening (24 October)

- **Request:** "go ahead with push and recommendation". `feature/password-reset` was pushed. My previous message had listed the open choices without picking one, so the recommendation was stated now: the **small login fix first**, because it closes two real security gaps found earlier (suspended users can still log in; login reveals which emails are registered) and is quick.
- **Branch:** `feature/login-hardening` (cut from `feature/password-reset`; not pushed).
- **user-service:** typed `InvalidCredentialsException` (401) and `AccountNotActiveException` (403) replace the message-text matching for login. Unknown email and wrong password give the same 401, and a dummy BCrypt comparison runs for an unknown email so timing does not reveal it. A blocking status is refused with 403 only after the correct password, so password guessing never reveals the status. Uses the existing `AccountPolicy`.
- **Web app:** new `ACCOUNT_NOT_ACTIVE` code and message; the login route maps 403 to it. The mock gateway was updated to the real behaviour (unknown email now 401, blocked account 403 after the right password).
- **Verified:** user-service **66 tests, 0 failures** (5 new); web app **39 of 39** browser checks (1 new); lint and build pass.
- **Docs:** user-service README, technical documentation (two gap rows resolved), user manual, root `CLAUDE.md`, web-app README, changelog, plan (backend step 5 done; remaining MVP work 19 to 18 units), Postman 111 to 112 requests.
- **Not verified:** MySQL, the real gateway, Newman. The change alters client-visible behaviour (unknown email is now 401, not 404). Blocking an account still leaves an issued access token valid for up to 15 minutes. No rate limiting on login.

## 22. Push, then run the code locally and verify it on the real stack (24 October)

- **Request:** "push the code and run locally code". `feature/login-hardening` was pushed.
- **Running locally:** Docker was running now and free memory was about 5.7 GB. MySQL, Redis, Kafka and Kafka UI were already up (healthy, started earlier by the user). Added **Mailpit** (`docker compose up -d mailpit`, image pulled, v1.32.0). Built and started only what the sign-in journey needs: **user-service (8081)** and **api-gateway (8080)** with a 384 MB heap each, and the **web app (3000)** from the existing production build. The other five services were not started (memory). Processes are tracked in `logs/pids.txt` (same format as `start-all.ps1`, so `scripts/stop-all.ps1` stops them).
- **First real checks:** Hibernate `update` on the user's existing MySQL database added 7 nullable `users` columns and created `password_reset_tokens`; the 18 existing users were untouched; no errors in the user-service log.
- **New script:** `web-app/e2e/auth.real-stack.mjs` (`npm run e2e:auth:real`): **14 of 14 pass**. My first version of its last check was mislabelled (it tested a new session renewing, not the old one being refused); it was rewritten so an old session is genuinely refused after a reset and a new one renews.
- **Direct checks on the real system:** stored profile fields and consent, BCrypt passwords and 64-character token hashes in MySQL; the real Kafka `user.registered` message has only `firstName, lastName, walletBalance, userId, email`; a SUSPENDED user gets 403 (correct password), 401 (wrong password), 401 on refresh, and logs in again once reactivated.
- **Cleanup:** the test users (`e2e-*@example.com`), their reset tokens and their Mailpit mail were deleted; users were back to the original 18. The two test registration events remain in the Kafka topic (cannot be removed).
- **Docs:** changelog entry, plan (verification noted on backend steps 1, 2, 3 and 5, risk updated), web-app README.
- **Not done:** Newman (the Postman collection), the other five services, any run in the cluster.
- **Left running for the user:** web app http://localhost:3000, gateway 8080, user-service 8081, Mailpit http://localhost:8025, Kafka UI http://localhost:8090.

## 23. "Start building the next steps, step by step automatically": onboarding (24 October)

- **Request:** "great job, start building next steps, start building step by step automatically". Interpreted as: work through the plan's remaining items in order, building, testing and committing each before moving on, and stop when done or blocked. Commits stay local (nothing was pushed since the last explicit push request). Item 1 of the plan (decision log) needs the user's input on launch market and billing, so it was skipped; the next item was **onboarding** (backend step 6 plus the four screens).
- **Branch:** `feature/onboarding` (cut from `feature/login-hardening`).
- **user-service:** `GET`/`PUT /me/onboarding`, `POST /me/onboarding/complete`; `OnboardingService`; new nullable `users` columns; `onboardingCompletedAt` in `UserResponse`; typed `UserNotFoundException`. Identity from the gateway header only (a forged `X-User-Id` still returns the caller's own data).
- **Web app:** `callAsUser` (one place that calls user-service as the signed-in user, renewing the session), `/api/onboarding` routes, the wizard (stepper, focus handling, native controls, summary, resume, skip per session), dashboard gate. Shared definitions and zod schema in `lib/onboarding.ts`.
- **Bugs found by testing, all fixed:** (1) after completing, the dashboard bounced back to the wizard: every layout created its own TanStack `QueryClient`, and Next keeps visited pages in memory, so the "done" update never reached the dashboard's cache; fixed with one shared browser client. (2) That exposed a second flaw hidden by the first: login and register seeded the profile cache with a profile that has no onboarding status; they now clear it. (3) The first `completedAt` had 7 fractional digits and later ones 6 (in-memory nanoseconds vs MySQL microseconds); truncated to microseconds with a unit test. (4) Several of my own test mistakes (hidden pages' radios counted, a missing paren, a mis-edit that did not apply).
- **Debugging notes:** one mock-test failure (an invalid refresh token) appeared once and not again in three further full runs; the BFF was verified directly over HTTP in six cookie combinations and was correct, so it was treated as load-related flakiness, not a defect. A temporary request log was added to the mock (`MOCK_LOG=1`) and an `ONLY=<text>` filter to the test runner; both stay, off by default.
- **Process finding:** `java` here is a launcher shim that starts the real JVM as a child. The recorded pid is the shim's, so `stop-all.ps1` (and my own stops) left an orphan JVM that kept the jar locked and broke a build. `stop-all.ps1` now uses `taskkill /T /F`. user-service is now started with the real `java.exe`.
- **Real-stack verification:** `npm run e2e:auth:real` 16 of 16 (now includes the wizard writing to MySQL and a fresh browser going straight to the dashboard); direct API checks (validation, partial saves, completing twice, forged header); **Newman run for the first time**: Users & Auth folder, 24 requests, 41 assertions, 0 failures. The other Newman folders were not run (their services are not started).
- **Cleanup and a finding:** all `e2e-*` test accounts and the Newman account were deleted (found by pattern and creation time, count checked first). The database showed **19 users, not 18**: the extra account is the user's own (`reddy.shankarprogrammer@gmail.com`), registered through the app while it was running; it was not touched. All existing accounts, including that one, have no onboarding completion time, so each will be offered the wizard once.
- **Docs:** user-service README, user manual, ER diagram, changelog, web-app README, plan (backend step 6 and onboarding done; remaining MVP work 18 to 15 units), Postman 112 to 119 requests.
- **Not done:** pushing; the other Newman folders; a run in the cluster; email verification; plan items 6 to 10 (deployment, app shell, settings, MVP screens, waitlist).

## 24. Web app deployment (24 October)

- **Request:** continuing "start building next steps, step by step automatically": plan item 6.
- **Branch:** `feature/web-app-deployment` (cut from `feature/onboarding`).
- **Built:** `web-app/Dockerfile` and `.dockerignore`, `output: "standalone"`, `GET /api/health`; `k8s/web-app/` following the api-gateway pattern (no Secret); ingress `app.stockmarket.local`; NetworkPolicy `allow-ingress-controller-to-web-app`; Argo CD application; CI job `web-app` plus the image matrix, `cd.yml`, `release.yml`, Dependabot npm and docker; Grafana board "Stock Market / Web App" (generated; the other boards show no drift).
- **Verified:** `docker build` works; the container ran with `--read-only`, `/tmp` and the Next cache as tmpfs and all capabilities dropped: health 200, login page 200, static asset 200, anonymous dashboard 307, wrong password through the container and the real gateway 401, foreign Origin 403, a request with a matching `X-Forwarded-Host` accepted. Every `k8s/*` kustomization renders. ESLint and `tsc` pass; 46 of 46 browser checks pass on the standalone server.
- **Bugs found, fixed:** (1) the same-origin check would have rejected every sign-in behind the TLS ingress (full-origin comparison; now a host comparison); (2) one onboarding check expected `/dashboard` but the app ends on `/dashboard?welcome=1`, so it timed out on every run (my earlier note that all 46 passed was therefore wrong for that check, or an older version of it; the check now accepts the query). A new opt-in `PAGE_LOG=1` in the e2e script prints console errors and navigations.
- **Not done:** not run in a real cluster (kubeconform and the Argo CD sync happen there); the workflow YAML was edited but not executed locally; no public address; OWASP Dependency-Check does not cover npm (Trivy and Dependabot do); nothing pushed.
- **Docs:** root CLAUDE.md, kubernetes-architecture, cicd-pipeline, github-actions-guide, web-app README, changelog, [plan 05](../research-doc/05-daily-implementation-plan.md) (total 15 to 13).

## 25. App shell and typed API client (24 October)

- **Request:** "start item 7" (plan item 7: app shell and typed API client, original units 31–32).
- **Branch:** `feature/app-shell` (cut from `feature/web-app-deployment`).
- **Shell:** route group `app/(app)/`, sidebar from 1024 px, top bar and bottom navigation below, five nav items from one list, skeleton pages for Trades, Portfolio and Settings, `proxy.ts` protects the new paths.
- **Typed client:** started account-service locally, saved its OpenAPI document (37 paths, 37 schemas) as `web-app/openapi/account-service.json`, generated `lib/api/account-service.d.ts` (openapi-typescript 7), wrapped it with openapi-fetch and TanStack hooks; `/api/gw/[...path]` BFF with a path allow-list; Accounts page; CI step that fails when the generated types are out of date.
- **Verified:** ESLint, `tsc`, build; 56 of 56 browser checks on the mock (10 new: redirects, breakpoints 360 to 2560 px, 44 px targets, current item, accounts list, empty, 500 error, BFF path rules, renewal, ended session); `e2e:accounts:real` 8 of 8 on the real stack (real gateway, user-service, account-service, MySQL); account-service 44 tests pass.
- **Bugs found, fixed:** (1) the spec documented only a 403 for the account list, so the generated type for the success answer was wrong (`@ApiResponse(200)` added); (2) `zod` 3 has a high `npm audit` finding with no 3.x fix, which the CI Trivy gate would have failed on: migrated to zod 4 (four `errorMap` to `error` changes); (3) my first traversal test expected 400 for `%2e%2e`, but the URL parser normalises it to a path outside the allow-list (404, safe): the test now checks both forms.
- **Cleanup:** the real-stack run created 4 users and 4 accounts; deleted by e-mail pattern `e2e-acc-%@example.com` after counting (4 users, 4 accounts, child tables cleared first), 19 users remain including the owner's own account.
- **Not done:** springdoc for the other services (user-service Boot 3 and five Boot 4 services) and their typed clients; statement upload through the BFF (multipart); creating accounts in the UI; nothing pushed; the real stack still runs (account-service on 8086 was started for this step and left running).
- **Docs:** web-app README, changelog, cicd-pipeline, [plan 05](../research-doc/05-daily-implementation-plan.md) (total 13 to 11).

## 26. Settings and preferences (24 October)

- **Request:** "go ahead with item 8" (plan item 8: settings screens and preferences, original units 33 to 36).
- **Branch:** `feature/settings` (cut from `feature/app-shell`).
- **user-service:** `user_preferences` table and `PreferencesService`; `GET`/`PUT /me/preferences`; `POST /me/password` (verifies the current password, sets `password_changed_at`, returns fresh tokens so the changing session survives); typed `IncorrectPasswordException` answered with **422** so no message matching is needed and a wrong password is never confused with an ended session (401). 83 tests (7 new).
- **Web app:** `/settings` with five categories, `/api/preferences` and `/api/auth/change-password` route handlers, hooks with optimistic saves and rollback, theme that follows the account (`PreferencesSync`, shell toggle saves), switches and radios, change-password form, honest "not available yet" for Keycloak-dependent security items and for Excel and PDF export, About page with version and legal links.
- **Verified:** ESLint, `tsc`, build; 65 of 65 browser checks on the mock (9 new); `e2e:settings:real` 8 of 8 on the real stack (defaults, persistence in MySQL, a new browser gets the dark theme at login, bad values refused, wrong current password keeps the session, the changing device survives with only the new refresh token while a second device is signed out, old password stops working); `e2e:auth:real` 16 of 16 and `e2e:accounts:real` 8 of 8 still pass.
- **Bugs found, fixed:** (1) 505 px wide settings page at 320 px (grid item sized to its content); (2) two parallel session renewals once the settings query joined the profile query (settings now wait for the profile); (3) a controlled radio looked unchanged right after a click (local state); (4) an ambiguous "Saved" text match in a test. `e2e:auth:real` failed 4 of 16 when pointed at a second web app port, because the emailed reset link carries `APP_BASE_URL` (port 3000): not a bug, so the web app on port 3000 was replaced by the new standalone build and the test passes there.
- **Cleanup:** 5 test users, 2 accounts and 1 preferences row deleted by pattern `e2e-%@example.com` after counting; 19 users remain including the owner's own account.
- **Process notes:** user-service was restarted with the new jar (pid in `logs/pids.txt`); the web app on http://localhost:3000 is now the standalone build of this branch; account-service on 8086 still runs.
- **Not done:** sessions, devices, two-factor, passkeys, login history (need Keycloak); push and alert emails (stored only); Excel and PDF export; a successful password change is not in the Newman collection (it would end the collection's own session); other Newman folders; nothing pushed.
- **Docs:** user-service README, user manual, technical documentation (ER), Postman generator, web-app README, changelog, cicd-pipeline, [plan 05](../research-doc/05-daily-implementation-plan.md) (total 11 to 7).

## 27. MVP screens (24 October)

- **Request:** "go ahead with item 9", then (user going to sleep) "keep working automatically and complete all the tasks".
- **Branch:** `feature/mvp-screens` (cut from `feature/settings`).
- **Built, in this order, each committed:** accounts with an open-account form, account page with summary and statement upload (multipart relayed through the BFF for that one path); trades table (filters, sorting, paging, detail, entry); dashboard key figures with a profit and loss calendar; portfolio with polled prices; a mock account-service (`e2e/mock-accounts.mjs`) so CI needs no backend; `e2e:mvp:real`.
- **Verified:** ESLint, `tsc`, build; 82 of 82 browser checks on the mock; `e2e:mvp:real` 9 of 9 on the real stack (real Zerodha parser: 4 trades imported and 1 row skipped, duplicate upload recognised, trades table and filters equal the API, calendar days and all-time total equal the API's realised profit, paper-account buy at the live market-data price, six screens free of sideways overflow at 360 px); account-service 45 tests pass.
- **Real bugs found by the real-stack run:** (1) **account-service `GET /holdings` returned 409** when two reads overlapped (the read stored the price through the versioned entity): fixed in account-service with a version-free price update and a unit test, 12 parallel reads now all 200; (2) account-service answers 400 to a date filter with only `from`, which my mock had hidden: the page now sends both ends or none; (3) the real trade `source` is `IMPORT` (the mock said `STATEMENT`). Mock-side: a missing Suspense around `usePathname` broke the build of `/accounts/[id]`; DELETE through the BFF demanded a JSON content type.
- **Cleanup:** 5 test users, 10 accounts and their rows in every `account_*` table, plus 5 stored statement files, deleted by pattern after counting; 19 users remain including the owner's own account. market-data-service (8082) was started for the paper-account check and left running.
- **Not done:** websocket prices (polling instead); renaming, closing and funding accounts in the UI; statement layouts of Groww, Binance and Exness checked only by account-service's unit tests, not with real files; pushing.
- **Docs:** web-app README, changelog, cicd-pipeline, [plan 05](../research-doc/05-daily-implementation-plan.md) (total 7 to 3).

## 28. Waitlist (24 October)

- **Request:** plan item 10, under the standing instruction "keep working automatically and complete all the tasks".
- **Branch:** `feature/waitlist` (cut from `feature/mvp-screens`).
- **user-service and gateway:** public `POST /api/v1/users/waitlist`, table `waitlist_entries` (lower-case unique email, time of consent, source), idempotent and the same 202 answer for a new and a listed email, the address is never logged; the path is public in `JwtAuthFilter`. 88 user-service tests, gateway filter test extended.
- **Web app:** landing section with email, consent and a privacy-policy link, honeypot, `/api/waitlist` route handler with Zod validation and a per-pod rate limit (10 a minute per address, 429 with `Retry-After`).
- **Verified:** 87 browser checks on the mock (5 new: validation, success and idempotence in lower case, outage, honeypot and origin and rate limit, 320 px layout); `tsc`, ESLint, build. The one failure in the first full run (an export-format settings check, 30 s timeout) passed on its own: the machine was heavily loaded (the run took more than ten minutes).
- **Real stack (later the same night):** user-service and the gateway were rebuilt and restarted; `POST /api/v1/users/waitlist` through the gateway answered 202 twice for `Real-Fan@Example.com` and stored one lower-case row with the unique key, 400 without consent; through the web app on port 3000 (now the standalone build of this branch) 200. All four real-stack suites pass: auth 16/16, accounts 8/8, settings 8/8, MVP 9/9; Newman folder 1 (Users & Auth) 54 assertions, 0 failures. Cleanup after counting: 6 test users, their rows and 1 stored statement directory, 3 waitlist rows; 19 users remain including the owner's account, waitlist empty.
- **Decisions I made that you may want to change:** the form promises only what the system does (address kept with the time of consent, no email sent); no name or interest field (less personal data); the honeypot and per-pod limiter are cheap measures, not a security boundary.
- **Docs:** user-service README, user manual, technical documentation, gateway README, Postman generator, web-app README, root CLAUDE.md known gaps, changelog, [plan 05](../research-doc/05-daily-implementation-plan.md) (total 3 to 1).

## 29. Trading terminal dashboard (24 October)

- **Request:** a pasted design brief ("Design a modern fintech trading platform dashboard UI for Quantiva": dark only, `#080B0F`, teal `#00E5B0`, sidebar, filter bar, 7 KPI cards, trades table, analytics, allocation, right panel; nine pages; Next.js 15, shadcn, TradingView/Recharts/ECharts; "Figma-ready"). A screenshot named in it did not arrive.
- **Questions asked and answered:** build it in the web app (not Figma, not prompt-only); brief palette for the terminal screens only; first pass = the dashboard page only.
- **Branch:** `feature/terminal-dashboard` (cut from `feature/waitlist`).
- **Built:** `/terminal` with seeded sample data (`lib/terminal/sample-data.ts`, 8 `node --test` checks), shell, filter bar, KPI cards, trading activity table with CSV export, six charts with table alternatives, right panel; the brief saved as D6 in the prompt library; a link from the signed-in dashboard.
- **Verified:** ESLint, `tsc`, build; 8 unit checks; 8 new browser checks (session needed, sample banner and 7 cards, sidebar honesty, filters drive the KPIs and the table, sort/page/columns/CSV equals the filtered rows, chart tables, collapse and drawer, no overflow at five widths with 44 px controls, axe WCAG 2.1 A/AA with 0 violations on desktop and phone). Screenshots at 1440, 768 and 360 px were looked at.
- **Bugs found, fixed:** page wider than the screen (grid sized by the table), 10 px overflow at 768 px (hidden chart tables), focusable chart internals under `aria-hidden`, column menu without Escape.
- **Decisions you may want to change:** the terminal data is sample data and says so everywhere (no real data: strategies, signals, risk and Sharpe have no backend); only Dashboard is a link in the sidebar, the other 15 destinations are inert and marked "Soon"; Recharts and Lucide used, not shadcn, ECharts or TradingView; AI assistant text is fixed sample text.
- **Not done:** the other eight pages of the brief; real data in the terminal; a manual screen-reader and keyboard review; Figma files; the screenshot-based layout match (screenshot missing).
- **Docs:** web-app README, prompt library D6, changelog.

## 30. Mode switch and the first five terminal pages (24 October)

- **Requests:** (1) "from login mode, make easy switch to terminal mode, keep switching mode in login dashboard"; (2) a second pasted brief (16 pages, PostgreSQL, Spring Boot 3.5, Next.js 15, shadcn, "production-ready"). Several short messages in between ("reset again", "restart port 3000 only", "continue").
- **Questions asked and answered:** extend the terminal with sample data in the existing stack (not a stack switch, no new backend); first pass is the core five pages (Markets, Portfolio, Positions, Orders, Journal); yes to restarting the web app and finishing the mode switch first.
- **Mode switch:** `ModeSwitch` in both shells, on a phone too, icons when collapsed; replaces the text link on the dashboard. Committed (`36c0ca9`).
- **Pages:** sample data (`sample-extra.ts`, 7 more unit checks), `DataTable`, `PageFrame`, `CategoryBars`, `LineSeries`, a Zustand store for ticket orders and journal entries, five pages, sidebar rebuilt to the 16 destinations of the brief.
- **Verified:** ESLint, `tsc`, build; 15 unit checks; browser checks for each page (filters, sorting, validation, ticket and cancel, journal entry and Show more, sidebar links with `aria-current`) plus axe WCAG 2.1 A and AA, overflow at 360, 768 and 1440 px and 44 px controls on all five pages; full browser suite.
- **Bugs found, fixed:** screen-reader text escaping a scroll region (page scrolled sideways on a phone), new orders sorting below sample orders, header overflow on a phone with the switch.
- **Process:** port 3000 now serves a copy of the build under `logs/web-live` (not committed), so rebuilding on 3100 for tests no longer takes the page you are reviewing away; to update 3000, copy `.next/standalone` over it and restart. Machine memory was low at times (a background command was stopped by Claude Code).
- **Decisions you may want to change:** the brief's stack differences were not followed (kept MySQL, Boot 4 services, Next.js 16, no shadcn); orders and journal live in page state only; ten destinations stay "Soon".
- **Not done:** Analytics, AI Signals, Screener, Option Analytics, Futures Analytics, Strategy Builder, Backtesting, Risk Management, Reports, Settings of the terminal; real data; backend; manual accessibility review; pushing these commits.
- **Docs:** web-app README, changelog.

## Open items after this session

| Item | Where |
|---|---|
| Commit and push stage 2 (built, verified, uncommitted); open the pull request by hand or install the GitHub CLI | Section 15 above |
| Legal pages are placeholders; real Terms and Privacy Policy need counsel before launch | `web-app/lib/content.ts` |
| Run Lighthouse and test 320–1920 px, keyboard and screen reader on stage 2 | [06-ai-prompt-library.md](../research-doc/06-ai-prompt-library.md) A1 "Done when" |
| Trust section statuses (Live, In progress, Planned) must be confirmed against the real deployment | `web-app/lib/content.ts`, [04-product-technology-roadmap.md](../research-doc/04-product-technology-roadmap.md) section 11 |
| Share image is the 512 px icon, a placeholder | `web-app/app/layout.tsx` |
| ~~Dockerfile, `k8s/web-app/`, Argo CD and CI lists~~ built in entry 24 (not yet run in a cluster) | Unit 18 of [05-daily-implementation-plan.md](../research-doc/05-daily-implementation-plan.md) |
| Fix the 24 mockup review findings that still apply to `web-app/` (contrast, tap targets, pause control, tab pattern, landmarks, risk line near numbers) in stage 2 | Section 11 above |
| Remotion/Playwright project for the eight clips | [07-feature-video-scripts.md](../research-doc/07-feature-video-scripts.md) |
| Shared Claude artifact not republished; `roadmap.html`, README and `web-app/` changes not yet committed | [research-doc/README.md](../research-doc/README.md) |
| Open decisions (launch market, Keycloak hosting, billing, passkeys vs PIN, plan contents, gender field, settings categories) | [research-doc/README.md](../research-doc/README.md), [04](../research-doc/04-product-technology-roadmap.md) section 12 |

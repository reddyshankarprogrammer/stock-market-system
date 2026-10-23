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

## Not done yet

Dockerfile, `k8s/web-app/`, Argo CD application and the CI/CD service lists (root `CLAUDE.md` section 8.6) are planned for unit 18. Landing page sections start in unit 21.

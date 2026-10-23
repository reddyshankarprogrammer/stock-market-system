# 3. Frontend for a TraderSync / TradeZella-style system

**Question:** Which frontend repo should we use, or how should we plan a complete system similar to TraderSync and TradeZella?

## Recommendation

Build our own frontend. Use the open-source trading journals only as feature references.

| Repo | Stack | License | Why not fork it |
|---|---|---|---|
| [Eleven-Trading/TradeNote](https://github.com/Eleven-Trading/TradeNote) (~975★) | Vue/Vite, Node, MongoDB | GPL v3 | Own MongoDB backend would duplicate our Java services; copyleft obligations. |
| [hugodemenez/deltalytix](https://github.com/hugodemenez/deltalytix) (~159★) | Next.js 16, React 19, Supabase, Prisma, Tailwind | CC BY-NC 4.0 | Commercial use not allowed; tied to Supabase. |

Worth studying from them: P&L calendar, session tags, AI coach, broker imports (Deltalytix imports TradeZella, Interactive Brokers, NinjaTrader and others; AI field mapping for arbitrary CSV files).

Our backend is already strong: account-service has trades, statement import (Groww, Zerodha, Binance, Exness), charges, taxes and performance. We mainly need the UI plus journal features.

## Stack (first version of the recommendation)

| Layer | Choice |
|---|---|
| Language | TypeScript |
| App | React 19 + Vite, TanStack Router, TanStack Query |
| UI | shadcn/ui + Tailwind, starting from [satnaing/shadcn-admin](https://github.com/satnaing/shadcn-admin) (MIT, Vite) |
| Price charts / replay | TradingView lightweight-charts (Apache-2.0, attribution required) |
| Analytics charts | Recharts |
| Journal editor | TipTap |
| Forms | react-hook-form + zod |
| API types | openapi-typescript from springdoc (account-service has springdoc; add it to the others) |

> Later revised in [04-product-technology-roadmap.md](04-product-technology-roadmap.md): Next.js 16 instead of Vite, because the landing page needs server rendering for search engines.

## Findings in the repo (October 2026)

- No frontend exists (no `package.json` anywhere).
- api-gateway has no CORS configuration; `JwtAuthFilter` only lets `OPTIONS` preflight through.
- market-data-service exposes STOMP over `/ws` (with SockJS) and `/topic`; the gateway does not route it.
- Only account-service has springdoc.

## Backend changes needed

1. **Journal features nobody owns yet:** trade notes, daily journal, tags (setup, mistake), screenshots, playbooks. Recommended as a new **journal-service** (Boot 4, Flyway) storing notes and tags against account-service trade ids.
2. **Analytics endpoints:** win rate, profit factor, expectancy, average win/loss, max drawdown; P&L by day, symbol, tag and hour. Can extend account-service performance.
3. **Candles endpoint** in market-data-service for trade replay (same endpoint as the Kronos plan).
4. **Auth:** add refresh tokens (known gap), ideally as an httpOnly cookie, before building the UI.
5. **Same-origin hosting:** serve the web app and `/api` through the same ingress host so no CORS configuration is needed.
6. **Live prices:** expose `/ws` through ingress, or poll prices at first.
7. **New `web-app/` service:** static Nginx image, `k8s/web-app/`, Argo CD application, CI/CD service lists (root `CLAUDE.md` section 8.6).

## Phased plan

| Phase | Deliverable |
|---|---|
| 0. Foundation | Scaffold, login/register, layout, API client, refresh token, ingress |
| 1. MVP journal | Accounts, statement upload (exists), trades table with filters, trade detail, KPI dashboard, P&L calendar |
| 2. Journaling | journal-service: notes, tags, screenshots, daily journal |
| 3. Reports | By tag, symbol and time; equity curve; drawdown; R-multiple |
| 4. Charts and replay | Candles with entry/exit markers, trade replay |
| 5. Extras | Platform trading screens (order-service), AI insights (ai-service / Kronos), playbooks, mobile layout |

Phases 0–1 give a usable TradeZella-style product on existing APIs, without new backend services.

## Sources

- [TradeNote – GitHub](https://github.com/Eleven-Trading/TradeNote)
- [TradeNote – gittrend](https://gittrend.io/repo/Eleven-Trading/TradeNote)
- [Deltalytix – GitHub](https://github.com/hugodemenez/deltalytix)
- [Deltalytix – gittrend](https://gittrend.io/repo/hugodemenez/deltalytix)
- [Deltalytix – Mintlify docs](https://www.mintlify.com/hugodemenez/deltalytix)
- [LuxAlgo Trade Journal](https://www.luxalgo.com/trade-journal.md)
- [Free shadcn admin dashboards – AdminLTE](https://adminlte.io/blog/free-shadcn-admin-dashboards)
- [Best open-source shadcn dashboard templates – Peerlist](https://peerlist.io/vaibhavgupta/articles/best-open-source-shadcn-dashboard-templates)
- [Best shadcn templates 2026 – DesignRevision](https://designrevision.com/blog/best-shadcn-templates.md)

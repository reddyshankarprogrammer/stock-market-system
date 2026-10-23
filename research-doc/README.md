# Quantiva research notes (quantiva.ai)

Research and analysis for the next stage of stock-market-system, whose product brand is **Quantiva** (website: **quantiva.ai**, tagline "Trade With Intelligence."): an own AI model, a frontend, and a product roadmap in the class of TraderSync and TradeZella. These are plans only; no service code has been changed for them.

- Author: Shanker Reddy (with Claude Code)
- Date: 22 October 2026
- Shareable team page: [Stock Market System Roadmap](https://claude.ai/artifact/UMCEna1X4kF5tGALPD9LPh) (private until shared from its Share menu; a local copy is [roadmap.html](roadmap.html))

## Documents

| # | Document | Question it answers |
|---|---|---|
| 1 | [01-stock-market-llm-repos.md](01-stock-market-llm-repos.md) | Which public GitHub repos give us our own LLM or model for the stock market? |
| 2 | [02-ai-model-recommendation.md](02-ai-model-recommendation.md) | Which model to use, in which language, has it been used, and what we must change |
| 3 | [03-frontend-trading-journal.md](03-frontend-trading-journal.md) | Which frontend repo or stack to use for a TraderSync / TradeZella-style system |
| 4 | [04-product-technology-roadmap.md](04-product-technology-roadmap.md) | Latest technologies, landing page, sign-up and login, settings, responsive design, subscription plans, admin portal, AI models, phased roadmap |
| 5 | [05-daily-implementation-plan.md](05-daily-implementation-plan.md) | Daily plan from 23 Oct 2026 (2 h weekdays, 4 h weekends): units 1–40 in detail (phases 0–3), blocks after that |
| 6 | [06-ai-prompt-library.md](06-ai-prompt-library.md) | Landing page master prompt plus 30+ prompts for development, enterprise readiness, design and operations |
| - | [roadmap.html](roadmap.html) | Local copy of the shareable team page (open in a browser) |
| - | [branding/QUANTIVA_BRAND_IDENTITY.md](branding/QUANTIVA_BRAND_IDENTITY.md) | Brand identity; logo files in `branding/logo/` (icon 16-1024 px, primary logo 350/700/1400 px) |
| - | [Landing page-html/Main.dc.html](Landing%20page-html/Main.dc.html) · [Quantiva Landing Page.pdf](Quantiva%20Landing%20Page.pdf) | Landing page mockup rebranded as Quantiva (quantiva.ai) and its PDF export |

## Conversation history

| Step | Request | Outcome |
|---|---|---|
| 1 | Public GitHub repos with their own LLM for the stock market | Shortlist: FinGPT, Kronos, TradingAgents, FinRobot, PIXIU. Doc 1. |
| 2 | Which one to use, with what language, has it been used, what changes | Kronos-small in a Python FastAPI `ai-service`; research-grade (AAAI 2026), no public production deployments found. Doc 2. |
| 3 | Implement it now? | No. Implementation deferred. |
| 4 | Frontend repo or plan for a TraderSync / TradeZella-like system | Build our own React frontend; use TradeNote and Deltalytix only as feature references because of their licenses. Doc 3. |
| 5 | Latest technologies, landing page with short videos, login options (user ID/password, Google, PIN), registration with email verification, AI model choice, roadmap | Next.js 16 + Keycloak + passkeys + Claude models + nine-phase roadmap. Doc 4. |
| 6 | Shareable page for the team | Published as a private artifact (link above). No repo changes. |
| 7 | Save everything to `research-doc/` | This folder. |
| 8 | Registration fields, standard/social/future login, TradingView-style settings, responsive targets, five subscription plans, admin portal, revenue dashboard | Doc 4 updated (sections 3–7, roadmap phases 0–9, risks, decisions). `roadmap.html` and the shared artifact not yet refreshed. |
| 9 | Daily two-hour implementation plan | Doc 5. |
| 10 | Start from today, more time at weekends | Doc 5 rewritten: starts 23 Oct 2026, 2 h weekdays and 4 h weekends (18 h a week), MVP on 22 Nov 2026, everything by late March 2027. |
| 11 | Landing page prompt saved, more prompts for development and enterprise customers | Doc 6. The landing page itself is not built. |

## Key decisions still open

1. Launch market first: India, US, or both.
2. Passkeys only, or passkeys plus a device-bound app PIN.
3. Self-hosted Keycloak or a managed identity service.
4. Claude model tier per AI feature.
5. Remotion company license or hand-recorded clips.
6. Source of real historical price data.
7. Content, limits and prices of the five plans (Free, Pro, AI Pro, Quant Pro, Enterprise), and the billing provider.
8. Gender field: add "Prefer not to say" or make it optional.
9. Settings categories 2–4, 6 and 8 (not in the supplied list).

Star counts and versions were checked in October 2026 and change over time; re-check before relying on them.

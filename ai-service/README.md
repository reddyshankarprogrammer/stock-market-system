# ai-service

Placeholder. Only an empty `main.py` exists; nothing is built, started or routed.

When it is implemented, follow the system conventions:

- Own port in `.env` (`AI_SERVICE_PORT`), a gateway route under `/api/v1/ai/**`, identity from `X-User-Id` / `X-User-Role`.
- Read prices from market-data-service (`GET /api/v1/market-data/stocks/{symbol}`) or Kafka `stock.price.updated`; never write to another service's tables.
- Add it to `scripts/start-all.ps1` and the root `CLAUDE.md` service table.

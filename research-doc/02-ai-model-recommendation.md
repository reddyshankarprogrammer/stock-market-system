# 2. AI model recommendation: Kronos-small in Python

**Question:** Which model should we use, with which programming language, has the model been used, and what changes do we need?

## Recommendation

**Kronos-small** ([shiyu-coder/Kronos](https://github.com/shiyu-coder/Kronos)), written in **Python**.

Why Kronos:
- We run it ourselves; it is not a call to an external API.
- It works on price candles (OHLCV), the kind of data this system already has.
- Kronos-small has 24.7M parameters and runs on CPU, so it fits the existing pod limits without a GPU.
- MIT license, ready-made weights on Hugging Face.

Why not FinGPT first: it needs news or text data, which we do not have, and a GPU for 7B-class models. Add it later for sentiment.

Why Python: Kronos is a PyTorch model with no Java runtime. The Java services stay as they are; only `ai-service` is Python. [ai-service/Dockerfile](../ai-service/Dockerfile) already expects Python 3.12 + FastAPI on port 8000.

## Has it been used?

- **In research, yes.** Peer-reviewed and published at AAAI 2026; about 25k GitHub stars.
- **In production, no public evidence found.** No broker or bank deployments or case studies turned up, and the same is true for FinGPT.
- Treat it as research-grade. Show its output as an informational forecast with a disclaimer, not as trading advice.

## What the repo looks like today (checked October 2026)

| Area | Finding |
|---|---|
| `ai-service/` | `README.md`, empty `main.py`, a production Dockerfile (Python 3.12 slim, uid 10001, port 8000, expects `requirements.txt` and an ASGI `app` with `GET /health`). |
| market-data-service | `StockQuote` stores single ticks (symbol, price, volume, timestamp), not candles. Prices are a random walk (`ThreadLocalRandom`, scheduled). Endpoints: `/stocks`, `/stocks/{symbol}`, `/stocks/list`; no history or candles endpoint. |
| api-gateway | Routes for the seven Java services; no `ai-service` host or `/api/v1/ai/**` route. |
| `k8s/ai-service/` | Deployment (replicas 0, read-only root filesystem, `/tmp` emptyDir of 256Mi, memory limit 1Gi), Service, ConfigMap (`AI_SERVICE_PORT: "8000"`), PDB, HPA kept out of `kustomization.yaml`. |
| CI/CD | `ci.yml` builds the image as soon as `ai-service/requirements.txt` exists; `cd.yml` and `release.yml` skip it until then. |

**Important caveat:** Kronos needs candles (open, high, low, close, volume). On today's random-walk ticks it would only forecast noise. Meaningful results need real historical prices (a known gap in root `CLAUDE.md` section 7).

## Changes needed

| # | Where | Change |
|---|---|---|
| 1 | market-data-service | Add `GET /api/v1/market-data/stocks/{symbol}/candles?interval=1m&limit=512`, aggregating `stock_quotes` into candles. ai-service calls this API and never reads market-data's tables. Optionally load real historical prices. |
| 2 | ai-service (new code) | `requirements.txt` with pinned fastapi, uvicorn, httpx, pandas, numpy, einops, huggingface_hub, safetensors and CPU-only torch. Vendor Kronos's `model/` folder (keep its MIT license; it is not on PyPI). `main.py`: `GET /health`, `GET /api/v1/ai/forecast/{symbol}?horizon=…`, read `X-User-Id`, load the model once at startup. |
| 3 | ai-service/Dockerfile | Install torch from the CPU wheel index (avoids the ~2 GB CUDA build). Download Kronos weights into the image at build time, because the root filesystem is read-only and `/tmp` is limited to 256Mi. |
| 4 | api-gateway | `ai-service: localhost` under `hosts:`, a route `/api/v1/ai/**` to `${AI_SERVICE_PORT}`, the docker profile host, and `JwtAuthFilter` / `JwtAuthFilterTest` lists if needed. |
| 5 | `.env`, `.env.example`, `scripts/start-all.ps1`, `stop-all.ps1` | `AI_SERVICE_PORT` (for example 8087 locally); start and stop ai-service with uvicorn. |
| 6 | `k8s/ai-service/` | Remove `replicas: 0`, add `hpa.yaml` to `kustomization.yaml`, raise the memory limit from 1Gi to about 2Gi for torch, add `HOSTS_AI_SERVICE` to the gateway ConfigMap. |
| 7 | CI/CD | Nothing; the image builds once `requirements.txt` exists. |
| 8 | Docs | ai-service `README.md` and a new `CLAUDE.md`, the service table in root `CLAUDE.md`, `docs/` (API, sequence diagrams), `docs/changelog.md`, Postman requests in `generate_collection.py`. |

Optional later:
- Publish forecasts to a Kafka topic (declare it in `k8s/kafka/kafka-topics.yaml` first).
- Add FinGPT for news sentiment once there is a news source.

**Status:** implementation was explicitly deferred.

## Sources

- [Kronos – AAAI 2026 paper](https://ojs.aaai.org/index.php/AAAI/article/view/39730)
- [Kronos – Trendshift](https://trendshift.io/repositories/15175)
- [Kronos overview – Pebblous](https://blog.pebblous.ai/report/kronos-financial-foundation-model/en/)
- [Kronos – explainx (AAAI acceptance)](https://explainx.ai/blog/kronos-foundation-model-financial-candlesticks-aaai-2026)
- [Kronos overview – Menon Lab](https://themenonlab.blog/blog/kronos-foundation-model-financial-markets-candlestick)
- [Broadridge GenAI adoption study](https://apac.entrepreneur.com/news-and-trends/broadridge-study-financial-institutions-shift-genai-from/503049)

# 1. Public GitHub repos with an own LLM or model for the stock market

**Question:** Which public GitHub repos are the most recommended for running our own LLM (not only calling OpenAI or Claude) for the stock market, as a reference for this project?

**Context:** `ai-service` in this repo is a placeholder (empty `main.py`, a Python Dockerfile, Kubernetes manifests scaled to 0).

## Shortlist

| # | Repo | What it is | Stars (Oct 2026, approx.) | Use it for |
|---|---|---|---|---|
| 1 | [AI4Finance-Foundation/FinGPT](https://github.com/AI4Finance-Foundation/FinGPT) | Open-source financial LLM. LoRA fine-tunes open models (Llama, Qwen, ChatGLM) on market news and filings. Includes FinGPT-Forecaster. MIT license, still maintained. | ~20k | Own text LLM: news and sentiment scoring, stock Q&A, short-term direction forecasts. Fine-tuning fits on one GPU. |
| 2 | [shiyu-coder/Kronos](https://github.com/shiyu-coder/Kronos) | Foundation model for candlestick (OHLCV) data, described as the first open-source one. Trained on 12B+ records from 45+ exchanges. Weights on Hugging Face. MIT license. | ~25k | Own price model: predicts the next candles from price history. Fits the `stock.price.updated` stream better than a text LLM. |
| 3 | [TauricResearch/TradingAgents](https://github.com/TauricResearch/TradingAgents) | Multi-agent LLM framework with analyst, researcher, trader and risk-manager roles. | High (not verified) | Architecture reference for an AI advisor flow. Works with local models through Ollama or vLLM. |
| 4 | [AI4Finance-Foundation/FinRobot](https://github.com/AI4Finance-Foundation/FinRobot) | Agent platform for financial analysis (reports, valuation). Built on FinGPT. | ~6.9k | Report generation and analysis agents. |
| 5 | [The-FinAI/PIXIU](https://github.com/The-FinAI/PIXIU) | FinMA financial LLM, instruction data, and the FLARE benchmark. | not found | Evaluating a model we fine-tune. |

## Recommendation

Use **FinGPT and Kronos together**. TradingAgents is useful for agent design but is an orchestration layer over LLMs, not its own model.

- **Kronos:** price forecasts from market-data candles.
- **FinGPT** (fine-tuned Llama or Qwen with LoRA): sentiment or explanation per symbol.
- **Placement:** both in a Python (FastAPI) `ai-service` behind the gateway, models served through vLLM or Ollama. It would consume `stock.price.updated` from Kafka and expose `/api/v1/ai/...` through `JwtAuthFilter`. As a new service, root `CLAUDE.md` section 8.6 applies.

## Cautions

- market-data-service simulates only six US stocks, so real historical OHLCV data is needed to fine-tune or evaluate Kronos.
- Model output is an informational signal, not investment advice.

## Sources

- [FinGPT – star-history](https://www.star-history.com/ai4finance-foundation/fingpt)
- [FinGPT – oosmetrics](https://oosmetrics.com/repo/AI4Finance-Foundation/FinGPT)
- [FinRobot – gittrend](https://gittrend.io/repo/AI4Finance-Foundation/FinRobot)
- [PIXIU – oosmetrics](https://oosmetrics.com/repo/The-FinAI/PIXIU)
- [Kronos – Trendshift](https://trendshift.io/repositories/15175)
- [Kronos overview – Menon Lab blog](https://themenonlab.blog/blog/kronos-foundation-model-financial-markets-candlestick)
- [Kronos-base – Hugging Face](https://huggingface.co/jeckmy/Kronos-base)
- [Best open-source LLM for finance 2026 – SiliconFlow](https://www.siliconflow.com/articles/best-open-source-llm-for-finance)

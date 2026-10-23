# syntax=docker/dockerfile:1.7
# ai-service: production image (Python, multi-stage, non-root).
# ai-service is not implemented yet (README.md). This Dockerfile defines the contract CI builds
# against as soon as ai-service/requirements.txt exists:
#   - main.py exposes an ASGI app object "app" (e.g. FastAPI) with GET /health
#   - requirements.txt pins every dependency, including uvicorn
#   - the service listens on port 8000

# ---- 1. Build wheels for every dependency ---------------------------------------------------
FROM python:3.12-slim-bookworm AS build
WORKDIR /build
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip \
    pip wheel --wheel-dir /wheels -r requirements.txt

# ---- 2. Runtime ------------------------------------------------------------------------------
FROM python:3.12-slim-bookworm AS runtime
ARG BUILD_VERSION=dev
ARG BUILD_REVISION=unknown
ARG BUILD_DATE=unknown
LABEL org.opencontainers.image.title="ai-service" \
      org.opencontainers.image.description="Stock Market System - ai-service" \
      org.opencontainers.image.source="https://github.com/reddyshankarprogrammer/stock-market-system" \
      org.opencontainers.image.vendor="Quantiva" \
      org.opencontainers.image.authors="Shanker Reddy" \
      org.opencontainers.image.version="${BUILD_VERSION}" \
      org.opencontainers.image.revision="${BUILD_REVISION}" \
      org.opencontainers.image.created="${BUILD_DATE}"

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=build /wheels /wheels
RUN pip install --no-cache-dir --no-index /wheels/* && rm -rf /wheels

COPY --chown=10001:10001 . .
USER 10001:10001

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    AI_SERVICE_PORT=8000

EXPOSE 8000
CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--proxy-headers", "--no-server-header"]

# syntax=docker/dockerfile:1.7
# web-app: production image (Next.js standalone server, multi-stage, non-root, distroless runtime).
#
#   docker build -t web-app web-app
#   docker run --rm -p 3000:3000 -e API_GATEWAY_URL=http://host.docker.internal:8080 web-app
#
# Build arguments: BUILD_VERSION / BUILD_REVISION / BUILD_DATE end up as OCI labels (set by CI).

# ---- 1. Dependencies: cached until package-lock.json changes ---------------------------------
FROM node:24-bookworm-slim AS deps
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

# ---- 2. Build: next build writes .next/standalone ---------------------------------------------
FROM node:24-bookworm-slim AS build
WORKDIR /workspace
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /workspace/node_modules ./node_modules
COPY . .
RUN npm run build

# ---- 3. Runtime: distroless Node 24 (no shell, no package manager), uid 65532 ------------------
FROM gcr.io/distroless/nodejs24-debian12:nonroot AS runtime
ARG BUILD_VERSION=dev
ARG BUILD_REVISION=unknown
ARG BUILD_DATE=unknown
LABEL org.opencontainers.image.title="web-app" \
      org.opencontainers.image.description="Quantiva web app (Next.js)" \
      org.opencontainers.image.source="https://github.com/reddyshankarprogrammer/stock-market-system" \
      org.opencontainers.image.vendor="Quantiva" \
      org.opencontainers.image.authors="Shanker Reddy" \
      org.opencontainers.image.version="${BUILD_VERSION}" \
      org.opencontainers.image.revision="${BUILD_REVISION}" \
      org.opencontainers.image.created="${BUILD_DATE}"

WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    HOSTNAME=0.0.0.0 \
    PORT=3000
COPY --from=build --chown=65532:65532 /workspace/.next/standalone ./
COPY --from=build --chown=65532:65532 /workspace/.next/static ./.next/static
COPY --from=build --chown=65532:65532 /workspace/public ./public

USER 65532:65532
EXPOSE 3000

# Probes come from Kubernetes (GET /api/health): the distroless image has no shell or curl.
# API_GATEWAY_URL is set by the ConfigMap. The root filesystem may be read-only: Next writes only to /app/.next/cache.
CMD ["server.js"]

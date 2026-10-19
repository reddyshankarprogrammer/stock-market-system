# syntax=docker/dockerfile:1.7
# portfolio-service: production image (Java 21, multi-stage, layered, non-root, distroless runtime).
#
#   docker build -t portfolio-service portfolio-service
#   docker run --rm -p 8084:8084 --env-file .env portfolio-service
#
# Build arguments: BUILD_VERSION / BUILD_REVISION / BUILD_DATE end up as OCI labels (set by CI).

# ---- 1. Build: compile and package with the Maven wrapper ----------------------------------
FROM eclipse-temurin:24-jdk-noble AS build
WORKDIR /workspace

# Dependencies first: this layer is cached until pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2     chmod +x mvnw && ./mvnw -B -q -ntp dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2     ./mvnw -B -q -ntp package -DskipTests     && find target -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/application.jar \;

# ---- 2. Extract Spring Boot layers: dependencies change rarely, application code often -------
FROM eclipse-temurin:24-jdk-noble AS extract
WORKDIR /extract
COPY --from=build /workspace/application.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination layers

# ---- 3. Runtime: distroless Java 21 (no shell, no package manager), uid 65532 -----------------
FROM gcr.io/distroless/java21-debian12:nonroot AS runtime
ARG BUILD_VERSION=dev
ARG BUILD_REVISION=unknown
ARG BUILD_DATE=unknown
LABEL org.opencontainers.image.title="portfolio-service"       org.opencontainers.image.description="Stock Market System - portfolio-service"       org.opencontainers.image.source="https://github.com/reddyshankarprogrammer/stock-market-system"       org.opencontainers.image.vendor="Sesami"       org.opencontainers.image.authors="Shanker Reddy"       org.opencontainers.image.version="${BUILD_VERSION}"       org.opencontainers.image.revision="${BUILD_REVISION}"       org.opencontainers.image.created="${BUILD_DATE}"

WORKDIR /application
COPY --from=extract --chown=65532:65532 /extract/layers/dependencies/ ./
COPY --from=extract --chown=65532:65532 /extract/layers/spring-boot-loader/ ./
COPY --from=extract --chown=65532:65532 /extract/layers/snapshot-dependencies/ ./
COPY --from=extract --chown=65532:65532 /extract/layers/application/ ./

USER 65532:65532

# The docker profile switches hosts to the container / Kubernetes service names; in Kubernetes
# the ConfigMap overrides the remaining hosts. Container-aware heap sizing.
ENV SPRING_PROFILES_ACTIVE=docker     JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC"

EXPOSE 8084

# Liveness/readiness come from Kubernetes probes (/actuator/health/liveness|readiness):
# the distroless image has no shell or curl for a Docker HEALTHCHECK.
ENTRYPOINT ["java", "-jar", "/application/application.jar"]

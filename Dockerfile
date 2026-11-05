# ==============================================================================
# Stage 1: Build Application
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source and build fat jar (tests run in CI/CD pipeline)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Stage 2: Minimal, Hardened Production Runtime Image
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Security: run as non-privileged system user (UID/GID 1001)
RUN addgroup -S -g 1001 appgroup && \
    adduser -S -u 1001 -G appgroup -s /bin/sh appuser

# Container-optimized JVM ergonomics:
# - Container-aware memory limits (75% max RAM, 50% initial RAM)
# - Immediate crash on OutOfMemory to trigger orchestrator restart
# - High-performance secure entropy source (/dev/urandom)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Copy the executable jar produced by Spring Boot Maven plugin
COPY --from=builder --chown=appuser:appgroup /build/target/ecommerce-catalog-*.jar app.jar

USER 1001:1001

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

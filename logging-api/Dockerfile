# Multi-stage so the image is reproducible from source alone - no "run mvn
# first" step that someone will forget, and no host JDK required.

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Poms first, on their own layer. Dependencies only re-download when a pom
# changes, not on every source edit.
COPY pom.xml .
COPY observability-contract/pom.xml observability-contract/
COPY observability-starter/pom.xml observability-starter/
COPY logging-api/pom.xml logging-api/
COPY sample-bank-app/pom.xml sample-bank-app/

RUN mvn -B -q -pl logging-api -am dependency:go-offline -DskipTests || true

COPY observability-contract observability-contract
COPY observability-starter observability-starter
COPY logging-api logging-api

# -am builds the modules logging-api depends on. Tests run in CI, not here.
RUN mvn -B -pl logging-api -am package -DskipTests


FROM eclipse-temurin:21-jre-alpine

# Never root. A log store holds unmasked identity in audit_logs; a container
# escape should not also hand over the box.
RUN addgroup -S observability && adduser -S observability -G observability

WORKDIR /app

COPY --from=build /build/logging-api/target/logging-api-*.jar app.jar

RUN mkdir -p /app/logs && chown -R observability:observability /app

USER observability

EXPOSE 8090

# Lets the JVM see the container's memory limit rather than the host's, which
# is how a container gets OOM-killed while the heap still looks healthy.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

# Compose gates dependents on this, so a service never starts against a
# logging-api that is still running Flyway.
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8090/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

# Multi-stage so the image is reproducible from source alone - no "run mvn
# first" step that someone will forget, and no host JDK required.

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Poms first, on their own layer. Dependencies only re-download when a pom
# changes, not on every source edit.
COPY pom.xml .
COPY observability-contract/pom.xml observability-contract/
COPY observability-starter/pom.xml observability-starter/
COPY sample-bank-app/pom.xml sample-bank-app/
COPY sample-bank-app/pom.xml sample-bank-app/

RUN mvn -B -q -pl sample-bank-app -am dependency:go-offline -DskipTests || true

COPY observability-contract observability-contract
COPY observability-starter observability-starter
COPY sample-bank-app sample-bank-app

# -am builds the modules sample-bank-app depends on. Tests run in CI, not here.
RUN mvn -B -pl sample-bank-app -am package -DskipTests


FROM eclipse-temurin:21-jre-alpine

# Never root, same as every other service on the platform.
RUN addgroup -S observability && adduser -S observability -G observability

WORKDIR /app

COPY --from=build /build/sample-bank-app/target/sample-bank-app-*.jar app.jar

RUN mkdir -p /app/logs && chown -R observability:observability /app

USER observability

EXPOSE 8080

# Lets the JVM see the container's memory limit rather than the host's, which
# is how a container gets OOM-killed while the heap still looks healthy.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

# Compose waits on this before declaring the stack ready.
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

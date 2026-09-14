FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
COPY observability-contract/pom.xml observability-contract/
COPY observability-starter/pom.xml observability-starter/
COPY sample-bank-app/pom.xml sample-bank-app/
COPY sample-bank-app/pom.xml sample-bank-app/

RUN mvn -B -q -pl sample-bank-app -am dependency:go-offline -DskipTests || true

COPY observability-contract observability-contract
COPY observability-starter observability-starter
COPY sample-bank-app sample-bank-app

RUN mvn -B -pl sample-bank-app -am package -DskipTests


FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S observability && adduser -S observability -G observability

WORKDIR /app

COPY --from=build /build/sample-bank-app/target/sample-bank-app-*.jar app.jar

RUN mkdir -p /app/logs && chown -R observability:observability /app

USER observability

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

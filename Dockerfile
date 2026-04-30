FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Gradle files first to maximize layer caching.
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon

COPY src src
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon
RUN cp build/libs/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache wget
COPY --from=builder /app/app.jar ./app.jar

ENV PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Dspring.profiles.active=default", "-Dspring.autoconfigure.exclude=com.google.cloud.spring.autoconfigure.sql.GcpCloudSqlAutoConfiguration", "-jar", "app.jar"]
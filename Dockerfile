# Multi-stage Dockerfile for building and running the Spring Boot app
# Build stage
FROM gradle:8.6-jdk21 AS builder

WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . ./
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon -x test

# Run stage
FROM eclipse-temurin:21-jre

ARG JAR_FILE=build/libs/app.jar
COPY --from=builder /home/gradle/project/${JAR_FILE} /app/app.jar

ENV JAVA_OPTS=""
EXPOSE 8080

# Use PORT env if provided by Render; default to 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]

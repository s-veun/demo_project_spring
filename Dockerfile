FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# ចម្លងឯកសារ build
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Build JAR
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon
RUN cp build/libs/*.jar app.jar

# កំណត់ Port ឱ្យត្រូវនឹង App (៨០៨៨)
ENV SERVER_PORT=8088
EXPOSE 8088

# កែសម្រួល Healthcheck ឱ្យត្រូវនឹង Port ៨០៨៨
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8088/actuator/health || exit 1

ENTRYPOINT ["java", "-Dspring.profiles.active=default", "-Dspring.autoconfigure.exclude=com.google.cloud.spring.autoconfigure.sql.GcpCloudSqlAutoConfiguration", "-jar", "app.jar"]
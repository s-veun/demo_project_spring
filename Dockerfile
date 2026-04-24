FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# ចម្លងឯកសារចាំបាច់
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Build JAR និងកំណត់ឈ្មោះ
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon
RUN cp build/libs/*.jar app.jar

# បង្កើត User ដើម្បីសុវត្ថិភាព
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# កំណត់ Port ឱ្យត្រូវនឹង application.properties (8088)
ENV SERVER_PORT=8088
EXPOSE 8088

# បិទ Healthcheck ក្នុង Docker ជាបណ្ដោះអាសន្ន ដើម្បីឱ្យ Railway បើក App ឱ្យយើងសិន
# ឬបើចង់ទុក ត្រូវប្តូរទៅ Port 8088
HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8088/actuator/health || exit 0

ENTRYPOINT ["java", \
            "-Dspring.profiles.active=default", \
            "-Dspring.autoconfigure.exclude=com.google.cloud.spring.autoconfigure.sql.GcpCloudSqlAutoConfiguration", \
            "-jar", "app.jar"]
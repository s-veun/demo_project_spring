# ប្រើ JDK 21 សម្រាប់ Build
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# ចម្លងឯកសារចាំបាច់
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Build JAR file
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

# កំណត់ឈ្មោះ JAR ដែលទើប Build រួចឱ្យទៅជា app.jar ដើម្បីងាយស្រួលហៅរត់
RUN cp build/libs/*.jar app.jar

# បង្កើត User ដើម្បីសុវត្ថិភាព
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# បង្ខំបិទ Google Cloud តាមរយៈ Java Opts និងកំណត់ Port
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Dspring.autoconfigure.exclude=com.google.cloud.spring.autoconfigure.sql.GcpCloudSqlAutoConfiguration"
ENV SERVER_PORT=8088

EXPOSE 8088

# រត់ Application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
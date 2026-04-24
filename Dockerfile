# ប្រើ JDK 21 សម្រាប់ការ Build និង Run ក្នុងពេលតែមួយ
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# ចម្លងឯកសារ Gradle និង Source Code
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Build JAR file និងប្ដូរឈ្មោះទៅជា app.jar
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon
RUN cp build/libs/*.jar app.jar

# បង្កើត User ដើម្បីសុវត្ថិភាព
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# ការកំណត់ Environment និង Port
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Dspring.autoconfigure.exclude=com.google.cloud.spring.autoconfigure.sql.GcpCloudSqlAutoConfiguration"
ENV SERVER_PORT=8088

EXPOSE 8088

# ពាក្យបញ្ជាសម្រាប់រត់ App
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# ចម្លងឯកសារចាំបាច់
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Build JAR និងកំណត់ឈ្មោះឱ្យងាយស្រួល
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon
RUN cp build/libs/*.jar app.jar

# បង្កើត User ដើម្បីសុវត្ថិភាព
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# កំណត់ Environment ដើម្បីបិទ Google Cloud និងកំណត់ Port
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Dspring.autoconfigure.exclude=com.google.cloud.spring.autoconfigure.sql.GcpCloudSqlAutoConfiguration"
# Railway នឹងផ្ដល់ PORT មកឱ្យយើងដោយស្វ័យប្រវត្តិ
EXPOSE 8088

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
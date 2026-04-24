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

# បង្កើត User
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# កំណត់ Port ឱ្យត្រូវនឹង application.properties (8088)
ENV SERVER_PORT=8088
EXPOSE 8088

ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=default -jar app.jar"]
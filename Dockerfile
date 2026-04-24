
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .


RUN chmod +x gradlew
COPY src src
RUN ./gradlew bootJar --no-daemon


FROM eclipse-temurin:21-jre-alpine
WORKDIR /app


RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring


COPY --from=build /app/build/libs/*.jar app.jar


ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Dspring.autoconfigure.exclude=com.google.cloud.spring.autoconfigure.sql.GcpCloudSqlAutoConfiguration"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
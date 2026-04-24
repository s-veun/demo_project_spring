# ... (ផ្នែក build រក្សាទុកដដែល) ...

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# កែប្រែត្រង់នេះ៖ បន្ថែមការបង្ខំបិទ GcpCloudSql
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC \
    -Dspring.autoconfigure.exclude=com.google.cloud.spring.autoconfigure.sql.GcpCloudSqlAutoConfiguration"

# ប្រសិនបើអ្នកមិនទាន់មាន application-railway.properties ត្រឹមត្រូវទេ
# គួរដក ENV SPRING_PROFILES_ACTIVE ចេញសិន ឬប្តូរមកប្រើ default វិញ
ENV SPRING_PROFILES_ACTIVE=default

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
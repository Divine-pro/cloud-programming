# Using the new industry-standard Eclipse Temurin image for Java 25
FROM eclipse-temurin:25-jdk-alpine

# Copy your JAR file
COPY target/acp-demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
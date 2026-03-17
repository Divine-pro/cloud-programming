# Stage 1: Build the application using Maven inside Docker
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copy the pom and source code
COPY pom.xml .
COPY src ./src
# Package the jar (bypassing your local Windows environment)
RUN mvn clean package -DskipTests

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:21-jdk
WORKDIR /app
# Grab the freshly built jar from Stage 1
COPY --from=build /app/target/*.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
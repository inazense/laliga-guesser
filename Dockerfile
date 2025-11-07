# Use OpenJDK 25 as base image
FROM eclipse-temurin:25-jdk AS build

# Set working directory
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY src src

# Copy data file
COPY laliga.csv .

# Build the application (skip tests for faster builds)
RUN ./gradlew clean build -x test

# Create final runtime image
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the built JAR from build stage (using wildcard to handle any version)
COPY --from=build /app/build/libs/*.jar app.jar

# Copy data file
COPY laliga.csv .

# Create models directory
RUN mkdir -p models

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

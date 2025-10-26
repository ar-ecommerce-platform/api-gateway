# -------------------------
# 1. Build stage
# -------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper and build files first (better caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Copy only source code
COPY src ./src

# Build the jar (skip tests, since they run in CI separately)
RUN ./gradlew build -x test --no-daemon

# -------------------------
# 2. Runtime stage
# -------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl (for health checks and debug)
RUN apk add --no-cache curl

# Copy only the jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose service port
EXPOSE 8888

# Run the app
CMD ["java", "-jar", "app.jar"]

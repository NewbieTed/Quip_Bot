# Stage 1: Build the application with Gradle caching
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Pre-copy Gradle wrapper files for caching Gradle distribution
COPY gradlew .
COPY gradle gradle

# Pre-fetch Gradle distribution to cache it
RUN ./gradlew --version

# Copy project files
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Build the application
RUN ./gradlew bootJar

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Copy migration resources explicitly for Flyway
COPY --from=build /app/build/resources/main/db/migration /app/db/migration
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

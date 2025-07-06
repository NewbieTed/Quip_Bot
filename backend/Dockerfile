# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
RUN ./gradlew bootJar

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Copy migration resources explicitly for Flyway
COPY --from=build /app/build/resources/main/db/migration /app/db/migration
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

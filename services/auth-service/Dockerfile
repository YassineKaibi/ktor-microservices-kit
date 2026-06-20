# Stage 1: Cache dependencies
FROM gradle:8.12-jdk21 AS deps
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
RUN gradle dependencies --no-daemon || true

# Stage 2: Build fat JAR
FROM deps AS build
COPY src ./src
RUN gradle buildFatJar --no-daemon

# Stage 3: Runtime
FROM --platform=linux/amd64 amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar app.jar
EXPOSE 8081
# MaxRAMPercentage=75.0 is provisional; retune once pod memory limits are set.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

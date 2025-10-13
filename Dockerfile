############################################
# 1) Build stage
############################################
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Gradle files first for better layer caching
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .
COPY gradle ./gradle

# Project sources
COPY src ./src

RUN chmod +x ./gradlew \
  && ./gradlew --no-daemon build -x test

############################################
# 2) Runtime stage
############################################
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]



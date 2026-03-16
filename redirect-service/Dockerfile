FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY ../gradlew .
COPY ../gradle gradle
COPY ../build.gradle .
COPY ../settings.gradle .
COPY ../common common
COPY ../redirect-service redirect-service
RUN chmod +x gradlew && ./gradlew :redirect-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/redirect-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

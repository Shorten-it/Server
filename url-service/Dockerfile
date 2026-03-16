FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY ../gradlew .
COPY ../gradle gradle
COPY ../build.gradle .
COPY ../settings.gradle .
COPY ../common common
COPY ../url-service url-service
RUN chmod +x gradlew && ./gradlew :url-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/url-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

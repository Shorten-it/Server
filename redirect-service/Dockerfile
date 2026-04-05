FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Gradle 래퍼 및 빌드 설정 복사 (의존성 캐싱 레이어)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY common/build.gradle common/build.gradle
COPY redirect-service/build.gradle redirect-service/build.gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 빌드 (코드 변경 시에만 재실행)
COPY common common
COPY redirect-service redirect-service
RUN ./gradlew :redirect-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/redirect-service/build/libs/*.jar app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

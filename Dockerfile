FROM openjdk:17 AS build
WORKDIR /api_service

COPY . .
RUN ./gradlew build -x test --no-daemon

FROM openjdk:17
WORKDIR /api_service
COPY --from=build /api_service/build/libs/*.jar api_service.jar
CMD ["java", "-jar", "api_service.jar"]
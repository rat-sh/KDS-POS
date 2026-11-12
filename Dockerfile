FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S kitchenflow && adduser -S kitchenflow -G kitchenflow
COPY --from=build /workspace/target/kds-0.0.1-SNAPSHOT.jar app.jar
USER kitchenflow
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

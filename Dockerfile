FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/vpn-fleet-manager.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

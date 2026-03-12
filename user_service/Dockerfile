FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY lombok.config .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:25.0.2_10-jre
WORKDIR /app
COPY --from=build /app/target/user_service-0.0.1-SNAPSHOT.jar user_service.jar
ENTRYPOINT ["java", "-jar", "user_service.jar"]

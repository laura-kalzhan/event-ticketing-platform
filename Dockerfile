FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY config config
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q -DskipTests -Dcheckstyle.skip package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system ticketing
COPY --from=build /app/target/ticketing-1.0.0.jar app.jar
USER ticketing
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

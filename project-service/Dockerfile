# Use the lightweight Java 21 environment
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw package -DskipTests
FROM eclipse-temurin:21-jre-alpine
COPY --from=build /workspace/target/*.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

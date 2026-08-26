# Use the lightweight Java 21 environment
FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp

# Copy the compiled jar file into the image
COPY target/*.jar app.jar

# Command to run the application
ENTRYPOINT ["java","-jar","/app.jar"]
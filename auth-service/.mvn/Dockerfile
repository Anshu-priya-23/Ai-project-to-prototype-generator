# Use the lightweight Java 21 environment
FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp

# Copy the compiled jar file into the image
# Note: Ensure your 'mvnw clean package' command successfully 
# created a jar file in the target/ folder of this specific service
COPY target/*.jar app.jar

# Command to run the application
ENTRYPOINT ["java","-jar","/app.jar"]
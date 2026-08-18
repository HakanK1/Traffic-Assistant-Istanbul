# From the official Eclipse Temurin image, appropriate for the project, we arepulling the Java 25 image
# Using the standard JRE for smooth operation of the ONNX (Artificial Intelligence) engine
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copying the compiled JAR file from the target directory to the container's working directory
COPY target/*.jar app.jar

# Exposing port 8080 for the application to be accessible from outside the container
EXPOSE 8080

# Command to run when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
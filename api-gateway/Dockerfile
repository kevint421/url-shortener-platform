# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy all POM files
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY url-service/pom.xml url-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml

# Copy source code
COPY common/src common/src
COPY url-service/src url-service/src
COPY api-gateway/src api-gateway/src

# Build everything from root 
RUN mvn clean install -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/api-gateway/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=docker"]
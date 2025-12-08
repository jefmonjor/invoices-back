# Multi-stage Dockerfile for Invoices Monolith
# Optimized for Railway deployment

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B || mvn dependency:resolve -B

# Copy source code
COPY src ./src

# Build the application with optimizations:
# - Skip tests for faster builds
# - Use multiple threads for parallel compilation (2 threads)
# - Batch mode for non-interactive builds
# - Increased memory for Maven to prevent OOM during compilation
# - Quick compilation mode for faster builds
RUN MAVEN_OPTS="-Xmx1024m -XX:+TieredCompilation -XX:TieredStopAtLevel=1" \
    mvn clean package -DskipTests -B -T 2C -Dmaven.compiler.debug=false -Dmaven.compiler.debuglevel=none

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthchecks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -s /bin/sh -D spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership to spring user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

# JVM options optimized for containers and production
# - G1GC: Better for heap sizes > 256MB, lower pause times
# - UseStringDeduplication: Reduces memory for duplicate strings (common in JSON)
# - MaxRAMPercentage: Uses 75% of container memory for heap
# - UseContainerSupport: Respects container memory limits
# - ExitOnOutOfMemoryError: Fail fast on OOM instead of hanging
# - Faster entropy source for security operations
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseStringDeduplication \
    -XX:+UseContainerSupport \
    -XX:+ExitOnOutOfMemoryError \
    -Xss512k \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]

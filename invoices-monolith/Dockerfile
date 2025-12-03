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
USER spring:spring

# Expose port (Railway injects PORT dynamically)
EXPOSE 8080

# Environment for JVM optimization
ENV JAVA_OPTS="-Xmx300m -Xms200m -XX:MaxMetaspaceSize=400m -Xss256k -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -Djava.security.egd=file:/dev/./urandom"

# Health check (Railway manages health checks, but good to have)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Run the application - Read PORT from Railway environment
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]

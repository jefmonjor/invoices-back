# =================================================================
# Multi-stage Dockerfile for Eureka Server
# =================================================================

FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="invoices-backend"
LABEL service="eureka-server"
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring
EXPOSE 8761
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1
ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

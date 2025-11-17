# =================================================================
# Multi-stage Dockerfile for User Service
# Stage 1: Build - Compila la aplicación con Maven
# Stage 2: Runtime - Ejecuta la aplicación con JRE minimal
# =================================================================

# =================================================================
# STAGE 1: BUILD
# =================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copiar solo pom.xml primero (para aprovechar cache de Docker)
COPY pom.xml .

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar el resto del código fuente
COPY src ./src

# Compilar y empaquetar (skip tests para build más rápido)
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# =================================================================
# STAGE 2: RUNTIME
# =================================================================
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL maintainer="invoices-backend"
LABEL service="user-service"
LABEL version="0.0.1-SNAPSHOT"

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Directorio de trabajo
WORKDIR /app

# Copiar JAR desde stage de build
COPY --from=build /app/target/*.jar app.jar

# Cambiar ownership
RUN chown spring:spring app.jar

# Cambiar a usuario no-root
USER spring:spring

# Exponer puerto
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

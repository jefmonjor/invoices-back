#!/bin/bash

# Script para ejecutar la aplicación localmente con las mismas credenciales de Railway
# Usa los servicios remotos (Neon PostgreSQL, Upstash Redis, Cloudflare R2)

echo "🚀 Iniciando Invoices Backend en LOCAL..."
echo ""
echo "📦 Servicios remotos:"
echo "  - PostgreSQL: Neon (EU West)"
echo "  - Redis: Upstash"
echo "  - S3: Cloudflare R2"
echo ""

# Navegar al directorio del monolito
cd invoices-monolith

# Variables de entorno (mismas que Railway)
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8080

# PostgreSQL (Neon) - Pooled connection
export SPRING_DATASOURCE_URL="jdbc:postgresql://ep-delicate-snow-abyzqltv-pooler.eu-west-2.aws.neon.tech:5432/neondb?sslmode=require"
export DB_USERNAME="neondb_owner"
export DB_PASSWORD="npg_02GsdHFqhfoU"

# Redis (Upstash)
export REDIS_HOST="subtle-parrot-38179.upstash.io"
export REDIS_PORT=6379
export REDIS_PASSWORD="ApUjAAIgcDI37a9MyM6T1LPJbUI4964n8CwccbGkioWuVe2WQwrM6A"
export REDIS_SSL=true

# JWT Configuration
export JWT_SECRET="local-development-secret-key-change-in-production-min-32-chars"
export JWT_EXPIRATION_MS=3600000
export JWT_ISSUER="invoices-backend-local"

# S3 / Cloudflare R2
export S3_ENDPOINT="https://ac29c1ccf8f12dc453bdec1c87ddcffb.r2.cloudflarestorage.com"
export S3_ACCESS_KEY="6534534b1dfc4ae849e1d01f952cd06c"
export S3_SECRET_KEY="5bc3d93666a9fec20955fefa01b51c1d85f2b4e044233426b52dbaf7f514f246"
export S3_BUCKET_NAME="invoices-documents"
export S3_REGION="auto"
export S3_PATH_STYLE_ACCESS=true

# CORS (permite frontend local)
export CORS_ALLOWED_ORIGINS="http://localhost:3000,http://localhost:5173,http://localhost:8080"

# Logging
export LOG_LEVEL_ROOT=INFO
export LOG_LEVEL_APP=DEBUG

echo "✅ Variables de entorno configuradas"
echo ""
echo "🔨 Compilando con Maven..."

# Compilar (sin tests para ir más rápido)
mvn clean package -DskipTests -B

if [ $? -ne 0 ]; then
    echo "❌ Error en compilación"
    exit 1
fi

echo ""
echo "✅ Compilación exitosa"
echo ""
echo "🚀 Iniciando aplicación..."
echo "📍 URL: http://localhost:8080"
echo "📚 Swagger: http://localhost:8080/swagger-ui.html"
echo "💚 Health: http://localhost:8080/actuator/health"
echo ""
echo "💡 Presiona Ctrl+C para detener"
echo ""

# Ejecutar el JAR
java -jar target/invoices-monolith-*.jar

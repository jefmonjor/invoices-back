#!/bin/bash

# ===================================================================
# Script para desplegar TODOS los microservicios a Fly.io
# ===================================================================

set -e

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Desplegando TODOS los microservicios${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

# Cargar variables de entorno
if [ -f .env.production ]; then
    echo -e "${GREEN}✓${NC} Cargando variables de .env.production..."
    set -a
    source .env.production
    set +a
else
    echo -e "${RED}✗${NC} No se encontró .env.production"
    exit 1
fi

# Verificar fly CLI
if ! command -v fly &> /dev/null; then
    echo -e "${RED}✗${NC} Fly CLI no está instalado"
    exit 1
fi

# Función para desplegar un servicio
deploy_service() {
    local service_name=$1
    local app_name=$2
    local port=$3
    local db_url_var=$4

    echo ""
    echo -e "${BLUE}=========================================${NC}"
    echo -e "${BLUE}Desplegando: $service_name${NC}"
    echo -e "${BLUE}=========================================${NC}"

    cd "$service_name"

    # Verificar si la app ya existe
    APP_EXISTS=$(fly apps list | grep -c "$app_name" || true)

    if [ "$APP_EXISTS" -eq "0" ]; then
        echo -e "${YELLOW}→${NC} Creando app '$app_name'..."

        # Crear fly.toml si no existe
        cat > fly.toml << EOF
app = "$app_name"
primary_region = "ams"

[build]
  dockerfile = "Dockerfile"

[env]
  SPRING_PROFILES_ACTIVE = "prod"
  SERVER_PORT = "$port"

[http_service]
  internal_port = $port
  force_https = true
  auto_stop_machines = false
  auto_start_machines = true
  min_machines_running = 1

[[services]]
  protocol = "tcp"
  internal_port = $port

  [[services.ports]]
    port = 80
    handlers = ["http"]
    force_https = true

  [[services.ports]]
    port = 443
    handlers = ["tls", "http"]

  [[services.http_checks]]
    interval = "30s"
    timeout = "5s"
    grace_period = "20s"
    method = "GET"
    path = "/actuator/health"
    protocol = "http"

[[vm]]
  size = "shared-cpu-1x"
  memory = "512mb"
EOF

        # Crear app
        fly launch --name "$app_name" --region ams --no-deploy --copy-config --yes

        echo -e "${GREEN}✓${NC} App creada"
    else
        echo -e "${GREEN}✓${NC} App '$app_name' ya existe"
    fi

    # Configurar secrets base
    echo -e "${YELLOW}→${NC} Configurando secrets base..."

    fly secrets set \
        SPRING_PROFILES_ACTIVE=prod \
        JWT_SECRET="$JWT_SECRET" \
        JWT_ISSUER=invoices-backend \
        EUREKA_CLIENT_ENABLED=false \
        SPRING_DATASOURCE_URL="${!db_url_var}" \
        -a "$app_name"

    # Configurar secrets específicos por servicio
    if [ "$service_name" == "document-service" ]; then
        echo -e "${YELLOW}→${NC} Configurando Cloudflare R2 para Document Service..."
        fly secrets set \
            S3_ENDPOINT="$R2_ENDPOINT" \
            S3_ACCESS_KEY="$R2_ACCESS_KEY_ID" \
            S3_SECRET_KEY="$R2_SECRET_ACCESS_KEY" \
            S3_BUCKET_NAME="$R2_BUCKET_NAME" \
            S3_REGION="$R2_REGION" \
            S3_PATH_STYLE_ACCESS=true \
            -a "$app_name"
    fi

    if [ "$service_name" == "trace-service" ]; then
        echo -e "${YELLOW}→${NC} Configurando Upstash Kafka para Trace Service..."

        # Crear JAAS config para SASL authentication
        JAAS_CONFIG="org.apache.kafka.common.security.scram.ScramLoginModule required username='$KAFKA_USERNAME' password='$KAFKA_PASSWORD';"

        fly secrets set \
            KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP_SERVERS" \
            KAFKA_SECURITY_PROTOCOL="$KAFKA_SECURITY_PROTOCOL" \
            KAFKA_SASL_MECHANISM="$KAFKA_SASL_MECHANISM" \
            KAFKA_SASL_JAAS_CONFIG="$JAAS_CONFIG" \
            KAFKA_TOPIC_INVOICE_EVENTS="$KAFKA_TOPIC_INVOICE_EVENTS" \
            KAFKA_TOPIC_USER_EVENTS="$KAFKA_TOPIC_USER_EVENTS" \
            KAFKA_TOPIC_AUDIT_TRAIL="$KAFKA_TOPIC_AUDIT_TRAIL" \
            -a "$app_name"
    fi

    # Desplegar
    echo -e "${YELLOW}→${NC} Desplegando..."
    fly deploy --ha=false --strategy immediate -a "$app_name"

    echo -e "${GREEN}✓${NC} $service_name desplegado exitosamente"

    cd ..
}

# Desplegar servicios
echo -e "${YELLOW}Desplegando microservicios esenciales (3 servicios: User, Invoice, Document)...${NC}"
echo -e "${YELLOW}Esto tomará aproximadamente 10-15 minutos${NC}"
echo ""

# 1. User Service (ESENCIAL - Autenticación y usuarios)
deploy_service "user-service" "invoices-user-service" "8082" "USER_DB_URL"

# 2. Invoice Service (ESENCIAL - Core business: crear/ver/editar facturas)
deploy_service "invoice-service" "invoices-invoice-service" "8081" "INVOICE_DB_URL"

# 3. Document Service (Almacenamiento de PDFs en Cloudflare R2)
deploy_service "document-service" "invoices-document-service" "8083" "DOCUMENT_DB_URL"

# 4. Trace Service (Auditoría completa con Upstash Kafka) - COMENTADO (Upstash no disponible)
# deploy_service "trace-service" "invoices-trace-service" "8084" "TRACE_DB_URL"

echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${GREEN}✓ SERVICIOS ESENCIALES DESPLEGADOS${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

echo -e "${GREEN}URLs de tus servicios:${NC}"
echo "Gateway:  https://invoices-backend.fly.dev (punto de entrada único)"
echo "User:     https://invoices-user-service.fly.dev (autenticación)"
echo "Invoice:  https://invoices-invoice-service.fly.dev (facturas)"
echo "Document: https://invoices-document-service.fly.dev (almacenamiento PDFs)"
echo ""

echo -e "${YELLOW}NO desplegado (sin Upstash Kafka):${NC}"
echo "Trace:    Sistema de auditoría (requiere Kafka)"
echo ""

echo -e "${GREEN}Servicios externos integrados:${NC}"
echo "Cloudflare R2: Almacenamiento de PDFs (10 GB gratis)"
echo "Neon PostgreSQL: 4 bases de datos (2 GB gratis)"
echo ""

echo -e "${GREEN}Verificar health checks:${NC}"
echo "curl https://invoices-user-service.fly.dev/actuator/health"
echo "curl https://invoices-invoice-service.fly.dev/actuator/health"
echo "curl https://invoices-document-service.fly.dev/actuator/health"
echo ""

echo -e "${GREEN}Test de login (usuario admin creado automáticamente):${NC}"
echo 'curl -X POST https://invoices-backend.fly.dev/api/auth/login \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{"email":"admin@invoices.com","password":"admin123"}'"'"
echo ""

echo -e "${YELLOW}Nota:${NC} Las migraciones de Flyway se ejecutaron automáticamente"
echo "Usuario admin creado: admin@invoices.com / admin123"
echo ""

echo -e "${BLUE}Total de VMs desplegadas: 4 (Gateway, User, Invoice, Document)${NC}"
echo -e "${BLUE}Consumo estimado: Bajo (consumo por uso)${NC}"
echo -e "${BLUE}Modelo: Fly.io consumo + Cloudflare R2 (todo gratis)${NC}"
echo "✅ Sistema funcional listo con almacenamiento de PDFs"

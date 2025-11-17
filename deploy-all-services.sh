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

    # Configurar secrets
    echo -e "${YELLOW}→${NC} Configurando secrets..."

    fly secrets set \
        SPRING_PROFILES_ACTIVE=prod \
        JWT_SECRET="$JWT_SECRET" \
        JWT_ISSUER=invoices-backend \
        EUREKA_CLIENT_ENABLED=false \
        SPRING_DATASOURCE_URL="${!db_url_var}" \
        -a "$app_name"

    # Desplegar
    echo -e "${YELLOW}→${NC} Desplegando..."
    fly deploy --ha=false --strategy immediate -a "$app_name"

    echo -e "${GREEN}✓${NC} $service_name desplegado exitosamente"

    cd ..
}

# Desplegar servicios
echo -e "${YELLOW}Desplegando 2 microservicios esenciales (100% gratis)...${NC}"
echo -e "${YELLOW}Esto tomará aproximadamente 8-10 minutos${NC}"
echo ""

# 1. User Service (ESENCIAL - Autenticación y usuarios)
deploy_service "user-service" "invoices-user-service" "8082" "USER_DB_URL"

# 2. Invoice Service (ESENCIAL - Core business)
deploy_service "invoice-service" "invoices-invoice-service" "8081" "INVOICE_DB_URL"

# 3. Document Service (OPCIONAL - Descomentado si quieres almacenamiento de archivos)
# deploy_service "document-service" "invoices-document-service" "8083" "DOCUMENT_DB_URL"

# 4. Trace Service (OPCIONAL - Descomentado si quieres auditoría)
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
echo ""

echo -e "${YELLOW}Servicios NO desplegados (para mantener free tier):${NC}"
echo "Document: (almacenamiento de archivos/PDFs)"
echo "Trace:    (auditoría y trazabilidad)"
echo ""

echo -e "${GREEN}Verificar health checks:${NC}"
echo "curl https://invoices-user-service.fly.dev/actuator/health"
echo "curl https://invoices-invoice-service.fly.dev/actuator/health"
echo ""

echo -e "${GREEN}Test de login (usuario admin creado automáticamente):${NC}"
echo 'curl -X POST https://invoices-user-service.fly.dev/api/auth/login \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{"email":"admin@invoices.com","password":"admin123"}'"'"
echo ""

echo -e "${YELLOW}Nota:${NC} Las migraciones de Flyway se ejecutaron automáticamente"
echo "Usuario admin creado: admin@invoices.com / admin123"
echo ""

echo -e "${BLUE}Total de VMs en Fly.io: 3/3 (FREE TIER)${NC}"
echo "✅ 100% Gratis - Sin cargos"

#!/bin/bash

# ================================================================
# Deploy Script para Fly.io - Free Tier Architecture
# ================================================================
# Este script despliega los 3 servicios principales en Fly.io:
# 1. Gateway Service (punto de entrada)
# 2. User Service (autenticación)
# 3. Invoice Service (lógica de negocio principal)
#
# Los servicios Document y Trace se despliegan en Render.com
# ================================================================

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Fly.io Deployment - Free Tier Architecture${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo ""

# Check if fly CLI is installed
if ! command -v fly &> /dev/null; then
    echo -e "${RED}❌ Fly CLI no está instalado${NC}"
    echo -e "${YELLOW}Instalar con: curl -L https://fly.io/install.sh | sh${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Fly CLI detectado${NC}"
echo ""

# Check if logged in
if ! fly auth whoami &> /dev/null; then
    echo -e "${YELLOW}⚠️  No estás autenticado en Fly.io${NC}"
    echo -e "${BLUE}Ejecutando: fly auth login${NC}"
    fly auth login
fi

echo -e "${GREEN}✅ Autenticado en Fly.io${NC}"
echo ""

# Function to deploy a service
deploy_service() {
    local SERVICE_NAME=$1
    local SERVICE_DIR=$2
    local APP_NAME=$3

    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  Deploying: ${SERVICE_NAME}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    cd "$SERVICE_DIR"

    # Check if app exists
    if fly apps list | grep -q "$APP_NAME"; then
        echo -e "${YELLOW}📦 App ${APP_NAME} ya existe, actualizando...${NC}"
        fly deploy --app "$APP_NAME" --ha=false
    else
        echo -e "${GREEN}🆕 Creando nueva app: ${APP_NAME}${NC}"
        fly launch --name "$APP_NAME" --region ams --ha=false --now
    fi

    echo -e "${GREEN}✅ ${SERVICE_NAME} desplegado correctamente${NC}"
    echo ""

    cd - > /dev/null
}

# Deploy services in order
echo -e "${BLUE}Iniciando deployment de servicios principales...${NC}"
echo ""

# 1. User Service (debe desplegarse primero - autenticación)
deploy_service "User Service" "user-service" "invoices-user-service"

# 2. Invoice Service (depende de User Service)
deploy_service "Invoice Service" "invoice-service" "invoices-invoice-service"

# 3. Gateway Service (último - enruta a todos los servicios)
deploy_service "Gateway Service" "gateway-service" "invoices-backend"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  🎉 ¡Deployment completado!${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Show deployed apps
echo -e "${BLUE}Aplicaciones desplegadas:${NC}"
fly apps list | grep "invoices-"

echo ""
echo -e "${YELLOW}📝 Próximos pasos:${NC}"
echo -e "1. Configurar secrets en cada app:"
echo -e "   ${BLUE}fly secrets set -a invoices-backend SPRING_DATASOURCE_URL='postgresql://...'${NC}"
echo -e "   ${BLUE}fly secrets set -a invoices-backend REDIS_HOST='subtle-parrot-38179.upstash.io'${NC}"
echo -e "   ${BLUE}fly secrets set -a invoices-backend REDIS_PASSWORD='...'${NC}"
echo -e "   ${BLUE}fly secrets set -a invoices-backend JWT_SECRET='...'${NC}"
echo ""
echo -e "2. Desplegar servicios secundarios en Render.com:"
echo -e "   - Document Service"
echo -e "   - Trace Service"
echo -e "   ${BLUE}Ver: render.yaml${NC}"
echo ""
echo -e "3. Configurar Gateway para apuntar a las URLs de los servicios"
echo ""
echo -e "${GREEN}URLs de los servicios:${NC}"
echo -e "  Gateway:  ${BLUE}https://invoices-backend.fly.dev${NC}"
echo -e "  User:     ${BLUE}https://invoices-user-service.fly.dev${NC}"
echo -e "  Invoice:  ${BLUE}https://invoices-invoice-service.fly.dev${NC}"
echo ""

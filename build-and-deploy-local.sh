#!/bin/bash

# 🚀 Build Locally and Deploy to Fly.io
# This script builds the JAR on your Mac and then deploys it to Fly.io
# This is MUCH faster than building on Fly.io servers

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

APP_NAME="invoices-monolith"

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

clear

echo -e "${MAGENTA}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║   🏗️  BUILD LOCAL + DEPLOY RÁPIDO - FLY.IO 🚀         ║"
echo "║                                                        ║"
echo "║        Build en tu Mac + Deploy de JAR pre-built      ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}\n"

# Verificar Maven
echo -e "${BLUE}[1/4]${NC} Verificando Maven..."
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven no está instalado${NC}"
    echo -e "${YELLOW}Instálalo con: brew install maven${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Maven instalado${NC}"
mvn --version | head -1
echo ""

# Verificar Fly CLI
echo -e "${BLUE}[2/4]${NC} Verificando Fly CLI..."
FLY_CMD=""
if command -v fly &> /dev/null; then
    FLY_CMD="fly"
elif command -v flyctl &> /dev/null; then
    FLY_CMD="flyctl"
else
    echo -e "${RED}❌ Fly CLI no está instalado${NC}"
    echo -e "${YELLOW}Ejecuta primero: ./deploy-macos.sh${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Fly CLI instalado${NC}\n"

# Build local
echo -e "${BLUE}[3/4]${NC} Compilando aplicación localmente..."
echo -e "${CYAN}════════════════════════════════════════════${NC}"
echo -e "${YELLOW}⏳ Compilando con Maven...${NC}"
echo -e "${CYAN}════════════════════════════════════════════${NC}\n"

cd invoices-monolith

# Clean and build
if mvn clean package -DskipTests -B; then
    echo -e "\n${GREEN}✅ Compilación exitosa${NC}\n"
else
    echo -e "\n${RED}❌ Error en la compilación${NC}\n"
    exit 1
fi

# Verificar JAR
JAR_FILE=$(ls target/*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}❌ No se encontró el JAR compilado${NC}"
    exit 1
fi

JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
echo -e "${GREEN}✅ JAR compilado: ${JAR_FILE} (${JAR_SIZE})${NC}\n"

# Confirmar deployment
echo -e "${MAGENTA}════════════════════════════════════════════${NC}"
echo -e "${YELLOW}⚠️  CONFIRMAR DEPLOYMENT A PRODUCCIÓN${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════${NC}\n"

echo -e "${CYAN}Información del deployment:${NC}"
echo -e "  • JAR compilado:  ${GREEN}${JAR_FILE}${NC}"
echo -e "  • Tamaño:         ${GREEN}${JAR_SIZE}${NC}"
echo -e "  • App:            ${GREEN}${APP_NAME}${NC}"
echo -e "  • Método:         ${GREEN}JAR pre-compilado (más rápido)${NC}\n"

read -p "$(echo -e ${YELLOW}¿Continuar con el deployment? [y/N]:${NC} )" CONFIRM

if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo -e "${YELLOW}Deployment cancelado${NC}"
    exit 0
fi

# Deploy con Dockerfile.prebuilt
echo -e "\n${BLUE}[4/4]${NC} Desplegando a Fly.io..."
echo -e "${CYAN}════════════════════════════════════════════${NC}"
echo -e "${YELLOW}⏳ Esto será MUCHO más rápido (1-3 min)...${NC}"
echo -e "${CYAN}════════════════════════════════════════════${NC}\n"

if $FLY_CMD deploy --dockerfile Dockerfile.prebuilt -a $APP_NAME; then
    echo -e "\n${GREEN}════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ DEPLOYMENT EXITOSO${NC}"
    echo -e "${GREEN}════════════════════════════════════════════${NC}\n"

    # Health check
    echo -e "${CYAN}Verificando health check...${NC}"
    sleep 10

    HEALTH_URL="https://${APP_NAME}.fly.dev/actuator/health"
    HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" || echo "000")

    if [ "$HEALTH_RESPONSE" = "200" ]; then
        echo -e "${GREEN}✅ Health check OK (200)${NC}\n"
    else
        echo -e "${YELLOW}⚠️  Health check: ${HEALTH_RESPONSE} (esperando...)${NC}\n"
    fi

    echo -e "${CYAN}📱 URL Backend:${NC} ${GREEN}https://${APP_NAME}.fly.dev${NC}"
    echo -e "${CYAN}📊 Health:${NC}      ${GREEN}https://${APP_NAME}.fly.dev/actuator/health${NC}"
    echo -e "${CYAN}📚 Swagger:${NC}     ${GREEN}https://${APP_NAME}.fly.dev/swagger-ui.html${NC}\n"

else
    echo -e "\n${RED}════════════════════════════════════════════${NC}"
    echo -e "${RED}❌ DEPLOYMENT FALLÓ${NC}"
    echo -e "${RED}════════════════════════════════════════════${NC}\n"
    exit 1
fi

echo -e "${MAGENTA}════════════════════════════════════════════${NC}"
echo -e "${GREEN}✨ ¡Deployment completado! ✨${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════${NC}\n"

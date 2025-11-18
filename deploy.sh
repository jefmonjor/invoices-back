#!/bin/bash

# 🚀 Script de Deployment Completo para Fly.io
# Ejecuta: ./deploy.sh

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# App name
APP_NAME="invoices-monolith"

clear

echo -e "${MAGENTA}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║        🚀 DEPLOYMENT A PRODUCCIÓN - FLY.IO 🚀          ║"
echo "║                                                        ║"
echo "║              Invoices Backend Monolith                ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}\n"

# Verificar que fly CLI esté instalado
echo -e "${BLUE}[1/6]${NC} Verificando Fly CLI..."
if ! command -v fly &> /dev/null; then
    echo -e "${RED}❌ Fly CLI no está instalado${NC}"
    echo -e "${YELLOW}Instálalo con: curl -L https://fly.io/install.sh | sh${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Fly CLI instalado${NC}\n"

# Verificar que esté logueado
echo -e "${BLUE}[2/6]${NC} Verificando autenticación..."
if ! fly auth whoami &> /dev/null; then
    echo -e "${RED}❌ No estás logueado en Fly.io${NC}"
    echo -e "${YELLOW}Ejecuta: fly auth login${NC}"
    exit 1
fi
FLY_USER=$(fly auth whoami 2>&1 | head -1)
echo -e "${GREEN}✅ Autenticado como: ${CYAN}${FLY_USER}${NC}\n"

# Verificar si la app existe
echo -e "${BLUE}[3/6]${NC} Verificando app en Fly.io..."
if fly apps list | grep -q "$APP_NAME"; then
    echo -e "${GREEN}✅ App '${APP_NAME}' encontrada${NC}\n"
else
    echo -e "${YELLOW}⚠️  App '${APP_NAME}' no existe${NC}"
    echo -e "${YELLOW}Creando app...${NC}"
    fly apps create $APP_NAME
    echo -e "${GREEN}✅ App creada${NC}\n"
fi

# Verificar secrets
echo -e "${BLUE}[4/6]${NC} Verificando secrets configurados..."
SECRET_COUNT=$(fly secrets list -a $APP_NAME 2>/dev/null | grep -c "NAME" || echo "0")

if [ "$SECRET_COUNT" -eq "0" ]; then
    echo -e "${RED}❌ No hay secrets configurados${NC}"
    echo -e "${YELLOW}⚠️  Debes ejecutar primero: ./configure-secrets.sh${NC}"
    read -p "¿Quieres ejecutar configure-secrets.sh ahora? (y/n): " RUN_CONFIG
    if [ "$RUN_CONFIG" = "y" ] || [ "$RUN_CONFIG" = "Y" ]; then
        ./configure-secrets.sh
    else
        echo -e "${RED}Abortando deployment${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ Secrets configurados${NC}"
    echo -e "${CYAN}Lista de secrets:${NC}"
    fly secrets list -a $APP_NAME | head -10
    echo ""
fi

# Confirmar deployment
echo -e "${MAGENTA}════════════════════════════════════════════${NC}"
echo -e "${YELLOW}⚠️  IMPORTANTE: ESTÁS A PUNTO DE DEPLOYAR A PRODUCCIÓN${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════${NC}\n"

echo -e "${CYAN}Configuración del deployment:${NC}"
echo -e "  • App name: ${GREEN}${APP_NAME}${NC}"
echo -e "  • Region: ${GREEN}Amsterdam (ams)${NC}"
echo -e "  • Memory: ${GREEN}512MB${NC}"
echo -e "  • Database: ${GREEN}neondb (ep-delicate-snow-abyzqltv)${NC}"
echo -e "  • Redis: ${GREEN}Upstash (subtle-parrot-38179)${NC}"
echo -e "  • Storage: ${GREEN}Cloudflare R2${NC}"
echo -e "  • Frontend: ${GREEN}https://invoices-frontend-vert.vercel.app${NC}\n"

read -p "$(echo -e ${YELLOW}¿Continuar con el deployment? [y/N]:${NC} )" CONFIRM

if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo -e "${YELLOW}Deployment cancelado${NC}"
    exit 0
fi

# Deploy
echo -e "\n${BLUE}[5/6]${NC} Iniciando deployment..."
echo -e "${CYAN}════════════════════════════════════════════${NC}"
echo -e "${YELLOW}⏳ Esto tomará aproximadamente 5-10 minutos...${NC}"
echo -e "${CYAN}════════════════════════════════════════════${NC}\n"

cd /home/user/invoices-back

# Run deployment
if fly deploy -c invoices-monolith/fly.toml -a $APP_NAME; then
    echo -e "\n${GREEN}════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ DEPLOYMENT EXITOSO${NC}"
    echo -e "${GREEN}════════════════════════════════════════════${NC}\n"
else
    echo -e "\n${RED}════════════════════════════════════════════${NC}"
    echo -e "${RED}❌ DEPLOYMENT FALLÓ${NC}"
    echo -e "${RED}════════════════════════════════════════════${NC}\n"
    echo -e "${YELLOW}Ver logs para más detalles:${NC}"
    echo -e "${CYAN}fly logs -a ${APP_NAME}${NC}\n"
    exit 1
fi

# Verificar deployment
echo -e "${BLUE}[6/6]${NC} Verificando deployment..."
echo -e "${CYAN}Esperando que la app esté lista...${NC}"
sleep 10

# Health check
echo -e "\n${CYAN}Probando health endpoint...${NC}"
HEALTH_URL="https://${APP_NAME}.fly.dev/actuator/health"
HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" || echo "000")

if [ "$HEALTH_RESPONSE" = "200" ]; then
    echo -e "${GREEN}✅ Health check OK (200)${NC}"
else
    echo -e "${YELLOW}⚠️  Health check returned: ${HEALTH_RESPONSE}${NC}"
    echo -e "${YELLOW}La app podría estar iniciando. Espera 30-60 segundos.${NC}"
fi

# Show info
echo -e "\n${MAGENTA}════════════════════════════════════════════${NC}"
echo -e "${MAGENTA}     🎉 DEPLOYMENT COMPLETADO 🎉${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════${NC}\n"

echo -e "${CYAN}URLs de la aplicación:${NC}"
echo -e "  📡 Backend API:     ${GREEN}https://${APP_NAME}.fly.dev${NC}"
echo -e "  📊 Health Check:    ${GREEN}https://${APP_NAME}.fly.dev/actuator/health${NC}"
echo -e "  📚 Swagger UI:      ${GREEN}https://${APP_NAME}.fly.dev/swagger-ui.html${NC}"
echo -e "  📖 API Docs:        ${GREEN}https://${APP_NAME}.fly.dev/api-docs${NC}"
echo -e "  🎨 Frontend:        ${GREEN}https://invoices-frontend-vert.vercel.app${NC}\n"

echo -e "${CYAN}Comandos útiles:${NC}"
echo -e "  Ver logs:           ${YELLOW}fly logs -a ${APP_NAME}${NC}"
echo -e "  Ver status:         ${YELLOW}fly status -a ${APP_NAME}${NC}"
echo -e "  Abrir dashboard:    ${YELLOW}fly open -a ${APP_NAME}${NC}"
echo -e "  SSH a la instancia: ${YELLOW}fly ssh console -a ${APP_NAME}${NC}\n"

echo -e "${CYAN}Test rápido:${NC}"
echo -e "${YELLOW}curl https://${APP_NAME}.fly.dev/actuator/health${NC}\n"

echo -e "${CYAN}Credenciales admin (⚠️  cambiar después del primer login):${NC}"
echo -e "  Email:    ${GREEN}admin@invoices.com${NC}"
echo -e "  Password: ${GREEN}admin123${NC}\n"

echo -e "${MAGENTA}════════════════════════════════════════════${NC}"
echo -e "${GREEN}✨ ¡Tu aplicación está ahora en producción! ✨${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════${NC}\n"

# Preguntar si quiere ver los logs
read -p "$(echo -e ${YELLOW}¿Ver logs en tiempo real? [y/N]:${NC} )" VIEW_LOGS

if [ "$VIEW_LOGS" = "y" ] || [ "$VIEW_LOGS" = "Y" ]; then
    echo -e "\n${CYAN}Mostrando logs (Ctrl+C para salir)...${NC}\n"
    fly logs -a $APP_NAME
fi

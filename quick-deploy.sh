#!/bin/bash

# 🚀 Quick Deploy - Deployment rápido con optimizaciones
# Este script verifica autenticación y despliega a Fly.io

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
echo "║      🚀 DEPLOYMENT RÁPIDO - FLY.IO ⚡                  ║"
echo "║                                                        ║"
echo "║          Con optimizaciones de build                  ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}\n"

# Verificar Fly CLI
echo -e "${BLUE}[1/4]${NC} Verificando Fly CLI..."
FLY_CMD=""
if command -v fly &> /dev/null; then
    FLY_CMD="fly"
elif command -v flyctl &> /dev/null; then
    FLY_CMD="flyctl"
else
    echo -e "${RED}❌ Fly CLI no está instalado${NC}"
    echo -e "${YELLOW}Instálalo con: brew install flyctl${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Fly CLI instalado${NC}\n"

# Verificar autenticación
echo -e "${BLUE}[2/4]${NC} Verificando autenticación..."
AUTH_CHECK=$($FLY_CMD auth whoami 2>&1)
AUTH_EXIT_CODE=$?

if [ $AUTH_EXIT_CODE -ne 0 ]; then
    echo -e "${YELLOW}⚠️  Token expirado o inválido${NC}"
    echo -e "${CYAN}Es necesario re-autenticarse${NC}\n"

    read -p "$(echo -e ${YELLOW}¿Quieres autenticarte ahora? [y/N]:${NC} )" AUTH_NOW

    if [ "$AUTH_NOW" = "y" ] || [ "$AUTH_NOW" = "Y" ]; then
        echo -e "\n${CYAN}Limpiando sesión anterior...${NC}"
        $FLY_CMD auth logout &> /dev/null || true

        echo -e "${CYAN}Abriendo navegador para autenticación...${NC}\n"
        $FLY_CMD auth login

        # Verificar que la autenticación funcionó
        if $FLY_CMD auth whoami &> /dev/null; then
            USER_EMAIL=$($FLY_CMD auth whoami 2>/dev/null || echo "unknown")
            echo -e "\n${GREEN}✅ Autenticación exitosa: ${USER_EMAIL}${NC}\n"
        else
            echo -e "${RED}❌ Error en la autenticación${NC}"
            exit 1
        fi
    else
        echo -e "${RED}Deployment cancelado${NC}"
        exit 1
    fi
else
    USER_EMAIL=$(echo "$AUTH_CHECK" | head -1)
    echo -e "${GREEN}✅ Autenticado como: ${USER_EMAIL}${NC}\n"
fi

# Pull últimos cambios
echo -e "${BLUE}[3/4]${NC} Actualizando código..."
CURRENT_BRANCH=$(git branch --show-current)
echo -e "${CYAN}Branch actual: ${CURRENT_BRANCH}${NC}"

if git pull origin "$CURRENT_BRANCH" 2>/dev/null; then
    echo -e "${GREEN}✅ Código actualizado${NC}\n"
else
    echo -e "${YELLOW}⚠️  No se pudo hacer pull (probablemente ya estás actualizado)${NC}\n"
fi

# Verificar app existe
echo -e "${BLUE}[4/4]${NC} Verificando app en Fly.io..."
if $FLY_CMD status -a $APP_NAME &> /dev/null; then
    echo -e "${GREEN}✅ App encontrada: ${APP_NAME}${NC}\n"
else
    echo -e "${RED}❌ App no encontrada: ${APP_NAME}${NC}"
    echo -e "${YELLOW}Ejecuta primero: ./deploy-macos.sh${NC}"
    exit 1
fi

# Mostrar info
echo -e "${MAGENTA}════════════════════════════════════════════${NC}"
echo -e "${YELLOW}⚠️  CONFIRMAR DEPLOYMENT${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════${NC}\n"

echo -e "${CYAN}Optimizaciones aplicadas:${NC}"
echo -e "  ✅ JAR optimizado (~85-90MB, antes 107MB)"
echo -e "  ✅ Dependencias reducidas (AWS SDK eliminado)"
echo -e "  ✅ Timeout extendido (20 minutos)"
echo -e "  ✅ Build paralelo activado"
echo -e "  ✅ Layered JARs habilitado\n"

echo -e "${CYAN}Información del deployment:${NC}"
echo -e "  • App:              ${GREEN}${APP_NAME}${NC}"
echo -e "  • Region:           ${GREEN}Amsterdam (ams)${NC}"
echo -e "  • Método:           ${GREEN}Build remoto optimizado${NC}"
echo -e "  • Tiempo estimado:  ${GREEN}8-12 minutos${NC}\n"

read -p "$(echo -e ${YELLOW}¿Continuar con el deployment? [y/N]:${NC} )" CONFIRM

if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo -e "${YELLOW}Deployment cancelado${NC}"
    exit 0
fi

# Deploy con reintentos
echo -e "\n${CYAN}════════════════════════════════════════════${NC}"
echo -e "${YELLOW}⏳ Desplegando a Fly.io...${NC}"
echo -e "${CYAN}════════════════════════════════════════════${NC}\n"

cd invoices-monolith

# Intentar deployment con reintentos en caso de error de red
MAX_RETRIES=2
RETRY_COUNT=0
DEPLOY_SUCCESS=false

while [ $RETRY_COUNT -le $MAX_RETRIES ]; do
    if [ $RETRY_COUNT -gt 0 ]; then
        echo -e "${YELLOW}Reintento $RETRY_COUNT de $MAX_RETRIES...${NC}\n"
        sleep 5
    fi

    if $FLY_CMD deploy -a $APP_NAME; then
        DEPLOY_SUCCESS=true
        break
    else
        DEPLOY_EXIT_CODE=$?
        RETRY_COUNT=$((RETRY_COUNT + 1))

        if [ $RETRY_COUNT -le $MAX_RETRIES ]; then
            echo -e "\n${YELLOW}⚠️  Error en deployment (intento $RETRY_COUNT de $((MAX_RETRIES + 1)))${NC}"
            echo -e "${CYAN}Esperando antes de reintentar...${NC}\n"
        fi
    fi
done

if [ "$DEPLOY_SUCCESS" = true ]; then
    echo -e "\n${GREEN}════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ DEPLOYMENT EXITOSO${NC}"
    echo -e "${GREEN}════════════════════════════════════════════${NC}\n"

    # Health check
    echo -e "${CYAN}Verificando health check...${NC}"
    sleep 15

    HEALTH_URL="https://${APP_NAME}.fly.dev/actuator/health"
    HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null || echo "000")

    if [ "$HEALTH_RESPONSE" = "200" ]; then
        echo -e "${GREEN}✅ Health check OK (200)${NC}\n"
    else
        echo -e "${YELLOW}⚠️  Health check: ${HEALTH_RESPONSE} (la app puede tardar 1-2 min en iniciar)${NC}\n"
    fi

    echo -e "${MAGENTA}════════════════════════════════════════════${NC}"
    echo -e "${GREEN}URLs de tu aplicación:${NC}"
    echo -e "${MAGENTA}════════════════════════════════════════════${NC}\n"
    echo -e "${CYAN}📱 Backend:${NC}  ${GREEN}https://${APP_NAME}.fly.dev${NC}"
    echo -e "${CYAN}📊 Health:${NC}   ${GREEN}https://${APP_NAME}.fly.dev/actuator/health${NC}"
    echo -e "${CYAN}📚 Swagger:${NC}  ${GREEN}https://${APP_NAME}.fly.dev/swagger-ui.html${NC}\n"

    echo -e "${CYAN}Comandos útiles:${NC}"
    echo -e "  • Ver logs:    ${YELLOW}fly logs -a ${APP_NAME}${NC}"
    echo -e "  • Ver status:  ${YELLOW}fly status -a ${APP_NAME}${NC}"
    echo -e "  • SSH:         ${YELLOW}fly ssh console -a ${APP_NAME}${NC}\n"

else
    echo -e "\n${RED}════════════════════════════════════════════${NC}"
    echo -e "${RED}❌ DEPLOYMENT FALLÓ después de $((MAX_RETRIES + 1)) intentos${NC}"
    echo -e "${RED}════════════════════════════════════════════${NC}\n"

    echo -e "${YELLOW}Posibles causas:${NC}"
    echo -e "  1. ${CYAN}Token expirado${NC} - Ejecuta: ${YELLOW}fly auth logout && fly auth login${NC}"
    echo -e "  2. ${CYAN}Timeout de build${NC} (si tarda >20 min)"
    echo -e "  3. ${CYAN}Error de compilación${NC} Maven"
    echo -e "  4. ${CYAN}Problemas de red${NC} - Intenta con otra conexión\n"

    echo -e "${YELLOW}Soluciones sugeridas:${NC}"
    echo -e "  1. Re-autenticar: ${CYAN}fly auth logout && fly auth login${NC}"
    echo -e "  2. Ver logs:      ${CYAN}fly logs -a ${APP_NAME}${NC}"
    echo -e "  3. Verificar app: ${CYAN}fly status -a ${APP_NAME}${NC}\n"

    echo -e "${MAGENTA}Si el error es de autorización:${NC}"
    echo -e "  ${YELLOW}fly auth logout${NC}"
    echo -e "  ${YELLOW}fly auth login${NC}"
    echo -e "  ${YELLOW}bash quick-deploy.sh${NC}\n"

    exit 1
fi

echo -e "${MAGENTA}════════════════════════════════════════════${NC}"
echo -e "${GREEN}✨ ¡Todo listo! ✨${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════${NC}\n"

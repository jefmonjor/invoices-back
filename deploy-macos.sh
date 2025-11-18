#!/bin/bash

# 🚀 Script de Deployment Simplificado para macOS
# Ejecuta: ./deploy-macos.sh

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

# Get script directory (macOS compatible)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

clear

echo -e "${MAGENTA}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║   🍎 DEPLOYMENT PARA macOS - INVOICES BACKEND 🍎       ║"
echo "║                                                        ║"
echo "║              Script de Deployment Completo            ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}\n"

# Función para verificar si un comando existe
command_exists() {
    command -v "$1" &> /dev/null
}

# [1] Verificar Homebrew
echo -e "${BLUE}[1/7]${NC} Verificando Homebrew..."
if ! command_exists brew; then
    echo -e "${YELLOW}⚠️  Homebrew no está instalado${NC}"
    echo -e "${YELLOW}Instalando Homebrew...${NC}"
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    echo -e "${GREEN}✅ Homebrew instalado${NC}\n"
else
    echo -e "${GREEN}✅ Homebrew instalado${NC}\n"
fi

# [2] Verificar Fly CLI
echo -e "${BLUE}[2/7]${NC} Verificando Fly CLI..."
if ! command_exists fly && ! command_exists flyctl; then
    echo -e "${YELLOW}⚠️  Fly CLI no está instalado${NC}"
    echo -e "${YELLOW}¿Cómo quieres instalarlo?${NC}"
    echo -e "  1) Usando Homebrew (recomendado para macOS)"
    echo -e "  2) Usando curl"
    read -p "Opción [1/2]: " INSTALL_METHOD

    if [ "$INSTALL_METHOD" = "1" ]; then
        echo -e "${CYAN}Instalando con Homebrew...${NC}"
        brew install flyctl
    else
        echo -e "${CYAN}Instalando con curl...${NC}"
        curl -L https://fly.io/install.sh | sh

        # Añadir al PATH para macOS
        echo -e "${YELLOW}Añadiendo flyctl al PATH...${NC}"
        if [ -f "$HOME/.zshrc" ]; then
            echo 'export FLYCTL_INSTALL="$HOME/.fly"' >> "$HOME/.zshrc"
            echo 'export PATH="$FLYCTL_INSTALL/bin:$PATH"' >> "$HOME/.zshrc"
            export FLYCTL_INSTALL="$HOME/.fly"
            export PATH="$FLYCTL_INSTALL/bin:$PATH"
            echo -e "${GREEN}✅ PATH actualizado en .zshrc${NC}"
        elif [ -f "$HOME/.bash_profile" ]; then
            echo 'export FLYCTL_INSTALL="$HOME/.fly"' >> "$HOME/.bash_profile"
            echo 'export PATH="$FLYCTL_INSTALL/bin:$PATH"' >> "$HOME/.bash_profile"
            export FLYCTL_INSTALL="$HOME/.fly"
            export PATH="$FLYCTL_INSTALL/bin:$PATH"
            echo -e "${GREEN}✅ PATH actualizado en .bash_profile${NC}"
        fi
    fi
    echo -e "${GREEN}✅ Fly CLI instalado${NC}\n"
else
    echo -e "${GREEN}✅ Fly CLI instalado${NC}\n"
fi

# Detectar comando fly
FLY_CMD=""
if command_exists fly; then
    FLY_CMD="fly"
elif command_exists flyctl; then
    FLY_CMD="flyctl"
else
    echo -e "${RED}❌ No se encontró fly/flyctl después de la instalación${NC}"
    echo -e "${YELLOW}Por favor, reinicia tu terminal y ejecuta: source ~/.zshrc${NC}"
    exit 1
fi

# [3] Verificar autenticación
echo -e "${BLUE}[3/7]${NC} Verificando autenticación en Fly.io..."
if ! $FLY_CMD auth whoami &> /dev/null; then
    echo -e "${YELLOW}⚠️  No estás logueado en Fly.io${NC}"
    echo -e "${CYAN}Abriendo navegador para login...${NC}"
    $FLY_CMD auth login
fi

FLY_USER=$($FLY_CMD auth whoami 2>&1 | head -1)
echo -e "${GREEN}✅ Autenticado como: ${CYAN}${FLY_USER}${NC}\n"

# [4] Verificar si la app existe
echo -e "${BLUE}[4/7]${NC} Verificando app en Fly.io..."
if $FLY_CMD apps list | grep -q "$APP_NAME"; then
    echo -e "${GREEN}✅ App '${APP_NAME}' encontrada${NC}\n"
else
    echo -e "${YELLOW}⚠️  App '${APP_NAME}' no existe${NC}"
    echo -e "${YELLOW}Creando app...${NC}"
    $FLY_CMD apps create $APP_NAME
    echo -e "${GREEN}✅ App creada${NC}\n"
fi

# [5] Verificar secrets
echo -e "${BLUE}[5/7]${NC} Verificando configuración de secrets..."
SECRET_COUNT=$($FLY_CMD secrets list -a $APP_NAME 2>/dev/null | grep -c "NAME" || echo "0")

if [ "$SECRET_COUNT" -eq "0" ]; then
    echo -e "${RED}❌ No hay secrets configurados${NC}"
    echo -e "${YELLOW}⚠️  Es necesario configurar los secrets primero${NC}\n"
    read -p "¿Quieres ejecutar configure-secrets.sh ahora? (y/n): " RUN_CONFIG
    if [ "$RUN_CONFIG" = "y" ] || [ "$RUN_CONFIG" = "Y" ]; then
        chmod +x ./configure-secrets.sh
        ./configure-secrets.sh
    else
        echo -e "${RED}Abortando deployment. Ejecuta primero:${NC}"
        echo -e "${YELLOW}./configure-secrets.sh${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ Secrets configurados (${SECRET_COUNT} secrets)${NC}\n"
fi

# [6] Confirmar deployment
echo -e "${MAGENTA}════════════════════════════════════════════${NC}"
echo -e "${YELLOW}⚠️  CONFIRMAR DEPLOYMENT A PRODUCCIÓN${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════${NC}\n"

echo -e "${CYAN}Información del deployment:${NC}"
echo -e "  • Directorio: ${GREEN}${SCRIPT_DIR}${NC}"
echo -e "  • App name:   ${GREEN}${APP_NAME}${NC}"
echo -e "  • Region:     ${GREEN}Amsterdam (ams)${NC}"
echo -e "  • Database:   ${GREEN}NeonDB PostgreSQL${NC}"
echo -e "  • Redis:      ${GREEN}Upstash${NC}"
echo -e "  • Storage:    ${GREEN}Cloudflare R2${NC}\n"

read -p "$(echo -e ${YELLOW}¿Continuar con el deployment? [y/N]:${NC} )" CONFIRM

if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo -e "${YELLOW}Deployment cancelado${NC}"
    exit 0
fi

# [7] Deploy
echo -e "\n${BLUE}[6/7]${NC} Ejecutando deployment..."
echo -e "${CYAN}════════════════════════════════════════════${NC}"
echo -e "${YELLOW}⏳ Esto puede tomar 5-10 minutos...${NC}"
echo -e "${CYAN}════════════════════════════════════════════${NC}\n"

if $FLY_CMD deploy -c invoices-monolith/fly.toml -a $APP_NAME; then
    echo -e "\n${GREEN}════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ DEPLOYMENT EXITOSO${NC}"
    echo -e "${GREEN}════════════════════════════════════════════${NC}\n"
else
    echo -e "\n${RED}════════════════════════════════════════════${NC}"
    echo -e "${RED}❌ DEPLOYMENT FALLÓ${NC}"
    echo -e "${RED}════════════════════════════════════════════${NC}\n"
    echo -e "${YELLOW}Ver logs:${NC}"
    echo -e "${CYAN}$FLY_CMD logs -a ${APP_NAME}${NC}\n"
    exit 1
fi

# [8] Verificar deployment
echo -e "${BLUE}[7/7]${NC} Verificando deployment..."
sleep 10

# Health check
HEALTH_URL="https://${APP_NAME}.fly.dev/actuator/health"
HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" || echo "000")

if [ "$HEALTH_RESPONSE" = "200" ]; then
    echo -e "${GREEN}✅ Health check OK (200)${NC}"
else
    echo -e "${YELLOW}⚠️  Health check: ${HEALTH_RESPONSE} (la app podría estar iniciando)${NC}"
fi

# Mostrar información final
echo -e "\n${MAGENTA}════════════════════════════════════════════${NC}"
echo -e "${MAGENTA}     🎉 DEPLOYMENT COMPLETADO 🎉${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════${NC}\n"

echo -e "${CYAN}📱 URLs:${NC}"
echo -e "  Backend:     ${GREEN}https://${APP_NAME}.fly.dev${NC}"
echo -e "  Health:      ${GREEN}https://${APP_NAME}.fly.dev/actuator/health${NC}"
echo -e "  Swagger:     ${GREEN}https://${APP_NAME}.fly.dev/swagger-ui.html${NC}"
echo -e "  Frontend:    ${GREEN}https://invoices-frontend-vert.vercel.app${NC}\n"

echo -e "${CYAN}🔧 Comandos útiles:${NC}"
echo -e "  Logs:        ${YELLOW}$FLY_CMD logs -a ${APP_NAME}${NC}"
echo -e "  Status:      ${YELLOW}$FLY_CMD status -a ${APP_NAME}${NC}"
echo -e "  Dashboard:   ${YELLOW}$FLY_CMD open -a ${APP_NAME}${NC}"
echo -e "  SSH:         ${YELLOW}$FLY_CMD ssh console -a ${APP_NAME}${NC}\n"

echo -e "${CYAN}🧪 Test rápido:${NC}"
echo -e "${YELLOW}curl https://${APP_NAME}.fly.dev/actuator/health${NC}\n"

# Preguntar si ver logs
read -p "$(echo -e ${YELLOW}¿Ver logs en tiempo real? [y/N]:${NC} )" VIEW_LOGS

if [ "$VIEW_LOGS" = "y" ] || [ "$VIEW_LOGS" = "Y" ]; then
    echo -e "\n${CYAN}Logs (Ctrl+C para salir)...${NC}\n"
    $FLY_CMD logs -a $APP_NAME
fi

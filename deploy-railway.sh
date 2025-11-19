#!/bin/bash

# 🚀 Deploy a Railway - Script Completo
# Ejecuta: ./deploy-railway.sh

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

clear

echo -e "${MAGENTA}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║      🚂 DEPLOYMENT A RAILWAY - INVOICES BACKEND 🚂    ║"
echo "║                                                        ║"
echo "║              Deploy Automático y Rápido               ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}\n"

# Verificar Railway CLI
echo -e "${BLUE}[1/4]${NC} Verificando Railway CLI..."
if ! command -v railway &> /dev/null; then
    echo -e "${YELLOW}⚠️  Railway CLI no está instalado${NC}"
    echo -e "${CYAN}Instalando Railway CLI...${NC}\n"

    # Instalar Railway CLI
    bash <(curl -fsSL https://railway.app/install.sh)

    echo -e "${GREEN}✅ Railway CLI instalado${NC}\n"
else
    echo -e "${GREEN}✅ Railway CLI instalado${NC}\n"
fi

# Login a Railway
echo -e "${BLUE}[2/4]${NC} Verificando autenticación..."
if ! railway whoami &> /dev/null; then
    echo -e "${YELLOW}⚠️  No estás autenticado en Railway${NC}"
    echo -e "${CYAN}Abriendo navegador para login...${NC}\n"
    railway login
    echo -e "${GREEN}✅ Autenticado en Railway${NC}\n"
else
    USER_INFO=$(railway whoami 2>/dev/null || echo "Usuario")
    echo -e "${GREEN}✅ Autenticado como: ${USER_INFO}${NC}\n"
fi

# Verificar si hay JAR compilado
echo -e "${BLUE}[3/4]${NC} Verificando build local..."
if [ -f "invoices-monolith/target/invoices-monolith-1.0.0.jar" ]; then
    JAR_SIZE=$(du -h invoices-monolith/target/invoices-monolith-1.0.0.jar | cut -f1)
    echo -e "${GREEN}✅ JAR encontrado: ${JAR_SIZE}${NC}\n"
else
    echo -e "${YELLOW}⚠️  No hay JAR compilado${NC}"
    echo -e "${CYAN}Compilando proyecto...${NC}\n"
    ./build-local-fast.sh
fi

# Deploy a Railway
echo -e "${BLUE}[4/4]${NC} Desplegando a Railway..."
echo -e "${CYAN}Esto puede tomar 3-5 minutos...${NC}\n"

# Verificar si el proyecto está vinculado
if [ ! -f ".railway/config.json" ]; then
    echo -e "${YELLOW}⚠️  Proyecto no vinculado${NC}"
    echo -e "${CYAN}Vinculando proyecto...${NC}\n"
    railway link
fi

# Deploy
railway up

echo -e "\n${GREEN}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║              ✅ DEPLOYMENT EXITOSO ✅                 ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}\n"

echo -e "${CYAN}Próximos pasos:${NC}"
echo -e "  1. Configura las variables de entorno en Railway:"
echo -e "     ${YELLOW}railway variables set SPRING_DATASOURCE_URL=\"jdbc:postgresql://...\"${NC}"
echo -e "     ${YELLOW}railway variables set JWT_SECRET=\"\$(openssl rand -base64 32)\"${NC}"
echo -e ""
echo -e "  2. Ver logs:"
echo -e "     ${YELLOW}railway logs${NC}"
echo -e ""
echo -e "  3. Abrir app:"
echo -e "     ${YELLOW}railway open${NC}"
echo -e ""

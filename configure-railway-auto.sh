#!/bin/bash

# 🚀 Configuración Automática de Railway con Credenciales Existentes
# Usa las mismas credenciales de Neon, Upstash y R2 ya configuradas

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

clear

echo -e "${MAGENTA}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║    🚂 CONFIGURACIÓN AUTOMÁTICA - RAILWAY 🚂           ║"
echo "║                                                        ║"
echo "║         Usando credenciales ya configuradas           ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}\n"

# Verificar Railway CLI
echo -e "${BLUE}[1/3]${NC} Verificando Railway CLI..."
if ! command -v railway &> /dev/null; then
    echo -e "${YELLOW}⚠️  Railway CLI no está instalado${NC}"
    echo -e "${CYAN}Instalando...${NC}\n"
    bash <(curl -fsSL https://railway.app/install.sh)
    echo -e "${GREEN}✅ Railway CLI instalado${NC}\n"
else
    echo -e "${GREEN}✅ Railway CLI instalado${NC}\n"
fi

# Verificar autenticación
echo -e "${BLUE}[2/3]${NC} Verificando autenticación..."
if ! railway whoami &> /dev/null; then
    echo -e "${YELLOW}⚠️  No estás autenticado en Railway${NC}"
    echo -e "${CYAN}Abriendo navegador para login...${NC}\n"
    railway login
    echo -e "${GREEN}✅ Autenticado${NC}\n"
else
    USER_INFO=$(railway whoami 2>/dev/null || echo "Usuario")
    echo -e "${GREEN}✅ Autenticado como: ${USER_INFO}${NC}\n"
fi

# Credenciales ya configuradas
echo -e "${BLUE}[3/3]${NC} Configurando variables de entorno...\n"

echo -e "${CYAN}Usando credenciales existentes:${NC}"
echo -e "  ${GREEN}✅${NC} PostgreSQL: Neon (neondb)"
echo -e "  ${GREEN}✅${NC} Redis: Upstash"
echo -e "  ${GREEN}✅${NC} Storage: Cloudflare R2"
echo ""

# PostgreSQL (Neon)
DB_HOST="ep-delicate-snow-abyzqltv-pooler.eu-west-2.aws.neon.tech"
DB_NAME="neondb"
DB_USER="neondb_owner"
DB_PASS="npg_02GsdHFqhfoU"
JDBC_URL="jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}?sslmode=require"

# JWT Secret (generar nuevo)
JWT_SECRET=$(openssl rand -base64 32)

# Redis (Upstash)
REDIS_HOST="subtle-parrot-38179.upstash.io"
REDIS_PORT="6379"
REDIS_PASSWORD="ApUjAAIgcDI37a9MyM6T1LPJbUI4964n8CwccbGkioWuVe2WQwrM6A"

# S3/R2 (Cloudflare)
S3_ENDPOINT="https://ac29c1ccf8f12dc453bdec1c87ddcffb.r2.cloudflarestorage.com"
S3_ACCESS_KEY="6534534b1dfc4ae849e1d01f952cd06c"
S3_SECRET_KEY="5bc3d93666a9fec20955fefa01b51c1d85f2b4e044233426b52dbaf7f514f246"
S3_BUCKET_NAME="invoices-documents"

# Confirmar
echo -e "${YELLOW}¿Configurar estas variables en Railway? [y/N]:${NC} "
read CONFIRM

if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo -e "${RED}Configuración cancelada${NC}"
    exit 0
fi

echo -e "\n${CYAN}Configurando variables...${NC}\n"

# Configurar todas las variables
railway variables set SPRING_PROFILES_ACTIVE="prod"
railway variables set SPRING_DATASOURCE_URL="$JDBC_URL"
railway variables set SPRING_DATASOURCE_USERNAME="$DB_USER"
railway variables set SPRING_DATASOURCE_PASSWORD="$DB_PASS"
railway variables set JWT_SECRET="$JWT_SECRET"
railway variables set JWT_EXPIRATION_MS="3600000"
railway variables set JWT_ISSUER="invoices-backend-prod"
railway variables set REDIS_HOST="$REDIS_HOST"
railway variables set REDIS_PORT="$REDIS_PORT"
railway variables set REDIS_PASSWORD="$REDIS_PASSWORD"
railway variables set REDIS_SSL="true"
railway variables set S3_ENDPOINT="$S3_ENDPOINT"
railway variables set S3_ACCESS_KEY="$S3_ACCESS_KEY"
railway variables set S3_SECRET_KEY="$S3_SECRET_KEY"
railway variables set S3_BUCKET_NAME="$S3_BUCKET_NAME"
railway variables set S3_REGION="auto"
railway variables set S3_PATH_STYLE_ACCESS="true"
railway variables set CORS_ALLOWED_ORIGINS="https://invoices-frontend-vert.vercel.app,http://localhost:3000,http://localhost:5173"
railway variables set JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"

echo -e "\n${GREEN}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║         ✅ CONFIGURACIÓN COMPLETADA ✅                 ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}\n"

echo -e "${CYAN}Verificar variables:${NC}"
echo -e "  ${YELLOW}railway variables${NC}"
echo ""

echo -e "${CYAN}Siguiente paso - Deploy:${NC}"
echo -e "  ${YELLOW}railway up${NC}"
echo -e "  O desde GitHub:"
echo -e "  1. ${YELLOW}git push origin main${NC}"
echo -e "  2. Ve a ${YELLOW}https://railway.app${NC} → Deploy from GitHub"
echo ""

echo -e "${CYAN}Ver logs:${NC}"
echo -e "  ${YELLOW}railway logs${NC}"
echo ""

echo -e "${CYAN}Abrir app:${NC}"
echo -e "  ${YELLOW}railway open${NC}"
echo ""

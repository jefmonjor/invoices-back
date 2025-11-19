#!/bin/bash

# 🔧 Configurar Variables de Neon en Railway
# Este script ayuda a configurar las variables de entorno en Railway

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

clear

echo -e "${CYAN}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║    🔧 CONFIGURAR NEON + RAILWAY - VARIABLES 🔧        ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}\n"

# Verificar Railway CLI
if ! command -v railway &> /dev/null; then
    echo -e "${RED}❌ Railway CLI no está instalado${NC}"
    echo -e "${YELLOW}Instálalo con: bash <(curl -fsSL https://railway.app/install.sh)${NC}"
    exit 1
fi

# Verificar autenticación
if ! railway whoami &> /dev/null; then
    echo -e "${YELLOW}⚠️  No estás autenticado en Railway${NC}"
    echo -e "${CYAN}Ejecuta: railway login${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Railway CLI listo${NC}\n"

# Obtener información de Neon
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}1. NEON POSTGRESQL${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}\n"

echo -e "${CYAN}Obtén tu connection string de Neon:${NC}"
echo -e "  1. Ve a ${YELLOW}https://console.neon.tech${NC}"
echo -e "  2. Selecciona tu proyecto"
echo -e "  3. Copia el ${YELLOW}Connection String${NC}"
echo ""

read -p "$(echo -e ${GREEN}Connection String de Neon: ${NC})" NEON_URL

if [ -z "$NEON_URL" ]; then
    echo -e "${RED}❌ Connection string vacío${NC}"
    exit 1
fi

# Extraer componentes de la URL
# Formato: postgresql://user:password@host:port/dbname?sslmode=require
if [[ $NEON_URL =~ postgresql://([^:]+):([^@]+)@([^:]+):([^/]+)/([^\?]+) ]]; then
    DB_USER="${BASH_REMATCH[1]}"
    DB_PASS="${BASH_REMATCH[2]}"
    DB_HOST="${BASH_REMATCH[3]}"
    DB_PORT="${BASH_REMATCH[4]}"
    DB_NAME="${BASH_REMATCH[5]}"

    JDBC_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=require"

    echo -e "\n${GREEN}✅ Conexión parseada:${NC}"
    echo -e "  Host: ${DB_HOST}"
    echo -e "  Port: ${DB_PORT}"
    echo -e "  Database: ${DB_NAME}"
    echo -e "  User: ${DB_USER}"
else
    echo -e "${RED}❌ Formato de URL inválido${NC}"
    exit 1
fi

# JWT Secret
echo -e "\n${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}2. JWT SECRET${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}\n"

JWT_SECRET=$(openssl rand -base64 32)
echo -e "${GREEN}✅ JWT Secret generado automáticamente${NC}"

# Redis
echo -e "\n${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}3. REDIS (Upstash)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}\n"

read -p "$(echo -e ${GREEN}Redis Host (ej: xxx.upstash.io): ${NC})" REDIS_HOST
read -p "$(echo -e ${GREEN}Redis Port (default 6379): ${NC})" REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}
read -s -p "$(echo -e ${GREEN}Redis Password: ${NC})" REDIS_PASSWORD
echo ""

# S3/R2
echo -e "\n${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}4. CLOUDFLARE R2 (S3)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}\n"

read -p "$(echo -e ${GREEN}S3 Endpoint: ${NC})" S3_ENDPOINT
read -p "$(echo -e ${GREEN}S3 Access Key: ${NC})" S3_ACCESS_KEY
read -s -p "$(echo -e ${GREEN}S3 Secret Key: ${NC})" S3_SECRET_KEY
echo ""
read -p "$(echo -e ${GREEN}S3 Bucket Name: ${NC})" S3_BUCKET_NAME

# Confirmar
echo -e "\n${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}RESUMEN DE CONFIGURACIÓN${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}\n"

echo -e "${CYAN}PostgreSQL (Neon):${NC}"
echo -e "  URL: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=require"
echo -e "  User: ${DB_USER}"
echo ""
echo -e "${CYAN}Redis:${NC}"
echo -e "  Host: ${REDIS_HOST}"
echo -e "  Port: ${REDIS_PORT}"
echo ""
echo -e "${CYAN}S3/R2:${NC}"
echo -e "  Endpoint: ${S3_ENDPOINT}"
echo -e "  Bucket: ${S3_BUCKET_NAME}"
echo ""

read -p "$(echo -e ${YELLOW}¿Configurar estas variables en Railway? [y/N]: ${NC})" CONFIRM

if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo -e "${RED}Configuración cancelada${NC}"
    exit 0
fi

# Configurar variables en Railway
echo -e "\n${CYAN}Configurando variables en Railway...${NC}\n"

railway variables set SPRING_PROFILES_ACTIVE="prod"
railway variables set SPRING_DATASOURCE_URL="$JDBC_URL"
railway variables set SPRING_DATASOURCE_USERNAME="$DB_USER"
railway variables set SPRING_DATASOURCE_PASSWORD="$DB_PASS"
railway variables set JWT_SECRET="$JWT_SECRET"
railway variables set REDIS_HOST="$REDIS_HOST"
railway variables set REDIS_PORT="$REDIS_PORT"
railway variables set REDIS_PASSWORD="$REDIS_PASSWORD"
railway variables set REDIS_SSL="true"
railway variables set S3_ENDPOINT="$S3_ENDPOINT"
railway variables set S3_ACCESS_KEY="$S3_ACCESS_KEY"
railway variables set S3_SECRET_KEY="$S3_SECRET_KEY"
railway variables set S3_BUCKET_NAME="$S3_BUCKET_NAME"
railway variables set JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"

echo -e "\n${GREEN}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║         ✅ VARIABLES CONFIGURADAS EXITOSAMENTE ✅      ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}\n"

echo -e "${CYAN}Verificar variables:${NC}"
echo -e "  ${YELLOW}railway variables${NC}"
echo ""
echo -e "${CYAN}Ver logs:${NC}"
echo -e "  ${YELLOW}railway logs${NC}"
echo ""
echo -e "${CYAN}Deploy:${NC}"
echo -e "  ${YELLOW}railway up${NC}"
echo ""

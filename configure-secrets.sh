#!/bin/bash

# 🔐 Script de Configuración de Secrets para Fly.io
# Este script configura TODOS los secrets necesarios para el backend

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# App name
APP_NAME="invoices-monolith"

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}🔐 Configuración de Secrets${NC}"
echo -e "${BLUE}================================${NC}\n"

# Verificar que fly CLI esté instalado
if ! command -v fly &> /dev/null; then
    echo -e "${RED}❌ Fly CLI no está instalado${NC}"
    echo -e "${YELLOW}Instálalo con: curl -L https://fly.io/install.sh | sh${NC}"
    exit 1
fi

# Verificar que esté logueado
if ! fly auth whoami &> /dev/null; then
    echo -e "${RED}❌ No estás logueado en Fly.io${NC}"
    echo -e "${YELLOW}Ejecuta: fly auth login${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Fly CLI instalado y autenticado${NC}\n"

# Usar la nueva base de datos creada
DB_NAME="neondb"
DB_HOST="ep-delicate-snow-abyzqltv-pooler.eu-west-2.aws.neon.tech"
DB_USER="neondb_owner"
DB_PASS="npg_02GsdHFqhfoU"

echo -e "${BLUE}📝 Usando nueva base de datos: ${DB_NAME}${NC}"
echo -e "${BLUE}📝 Host: ${DB_HOST}${NC}\n"

# Pedir JWT_SECRET
echo -e "${YELLOW}🔑 Genera un JWT Secret seguro:${NC}"
echo -e "${BLUE}openssl rand -base64 32${NC}\n"
JWT_SECRET=$(openssl rand -base64 32)
echo -e "${GREEN}✅ JWT Secret generado automáticamente${NC}"
echo -e "Valor: ${BLUE}${JWT_SECRET}${NC}\n"

read -p "Presiona Enter para continuar..."

# Configurar secrets
echo -e "\n${BLUE}================================${NC}"
echo -e "${BLUE}📦 Configurando secrets...${NC}"
echo -e "${BLUE}================================${NC}\n"

# JWT
echo -e "${YELLOW}[1/17]${NC} Configurando JWT_SECRET..."
fly secrets set JWT_SECRET="$JWT_SECRET" -a $APP_NAME

echo -e "${YELLOW}[2/17]${NC} Configurando JWT_EXPIRATION_MS..."
fly secrets set JWT_EXPIRATION_MS="3600000" -a $APP_NAME

echo -e "${YELLOW}[3/17]${NC} Configurando JWT_ISSUER..."
fly secrets set JWT_ISSUER="invoices-backend-prod" -a $APP_NAME

# PostgreSQL
echo -e "${YELLOW}[4/17]${NC} Configurando SPRING_DATASOURCE_URL..."
fly secrets set SPRING_DATASOURCE_URL="postgresql://${DB_USER}:${DB_PASS}@${DB_HOST}/${DB_NAME}?sslmode=require" -a $APP_NAME

echo -e "${YELLOW}[5/17]${NC} Configurando DB_USERNAME..."
fly secrets set DB_USERNAME="${DB_USER}" -a $APP_NAME

echo -e "${YELLOW}[6/17]${NC} Configurando DB_PASSWORD..."
fly secrets set DB_PASSWORD="${DB_PASS}" -a $APP_NAME

# Redis
echo -e "${YELLOW}[7/17]${NC} Configurando REDIS_HOST..."
fly secrets set REDIS_HOST="subtle-parrot-38179.upstash.io" -a $APP_NAME

echo -e "${YELLOW}[8/17]${NC} Configurando REDIS_PORT..."
fly secrets set REDIS_PORT="6379" -a $APP_NAME

echo -e "${YELLOW}[9/17]${NC} Configurando REDIS_PASSWORD..."
fly secrets set REDIS_PASSWORD="ApUjAAIgcDI37a9MyM6T1LPJbUI4964n8CwccbGkioWuVe2WQwrM6A" -a $APP_NAME

echo -e "${YELLOW}[10/17]${NC} Configurando REDIS_SSL..."
fly secrets set REDIS_SSL="true" -a $APP_NAME

# S3/R2
echo -e "${YELLOW}[11/17]${NC} Configurando S3_ENDPOINT..."
fly secrets set S3_ENDPOINT="https://ac29c1ccf8f12dc453bdec1c87ddcffb.r2.cloudflarestorage.com" -a $APP_NAME

echo -e "${YELLOW}[12/17]${NC} Configurando S3_ACCESS_KEY..."
fly secrets set S3_ACCESS_KEY="6534534b1dfc4ae849e1d01f952cd06c" -a $APP_NAME

echo -e "${YELLOW}[13/17]${NC} Configurando S3_SECRET_KEY..."
fly secrets set S3_SECRET_KEY="5bc3d93666a9fec20955fefa01b51c1d85f2b4e044233426b52dbaf7f514f246" -a $APP_NAME

echo -e "${YELLOW}[14/17]${NC} Configurando S3_BUCKET_NAME..."
fly secrets set S3_BUCKET_NAME="invoices-documents" -a $APP_NAME

echo -e "${YELLOW}[15/17]${NC} Configurando S3_REGION..."
fly secrets set S3_REGION="auto" -a $APP_NAME

echo -e "${YELLOW}[16/17]${NC} Configurando S3_PATH_STYLE_ACCESS..."
fly secrets set S3_PATH_STYLE_ACCESS="true" -a $APP_NAME

# CORS
echo -e "${YELLOW}[17/17]${NC} Configurando CORS_ALLOWED_ORIGINS..."
fly secrets set CORS_ALLOWED_ORIGINS="https://invoices-frontend-vert.vercel.app" -a $APP_NAME

echo -e "\n${GREEN}================================${NC}"
echo -e "${GREEN}✅ Todos los secrets configurados!${NC}"
echo -e "${GREEN}================================${NC}\n"

# Listar secrets
echo -e "${BLUE}📋 Lista de secrets configurados:${NC}\n"
fly secrets list -a $APP_NAME

echo -e "\n${GREEN}🎉 ¡Listo! Ahora puedes hacer el deploy:${NC}"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo -e "${BLUE}cd ${SCRIPT_DIR}${NC}"
echo -e "${BLUE}./deploy.sh${NC}"
echo -e "${BLUE}# O manualmente: fly deploy -c invoices-monolith/fly.toml -a $APP_NAME${NC}\n"

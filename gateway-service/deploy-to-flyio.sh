#!/bin/bash

# ===================================================================
# Script de Deployment del Gateway Service a Fly.io
# ===================================================================

set -e  # Exit on error

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=================================${NC}"
echo -e "${GREEN}Deploying Gateway Service to Fly.io${NC}"
echo -e "${GREEN}=================================${NC}"
echo ""

# Cargar variables de entorno
if [ -f ../.env.production ]; then
    echo -e "${GREEN}✓${NC} Cargando variables de .env.production..."
    set -a
    source ../.env.production
    set +a
else
    echo -e "${RED}✗${NC} No se encontró .env.production"
    echo "Por favor crea el archivo .env.production en el directorio raíz"
    exit 1
fi

# Verificar fly CLI
if ! command -v fly &> /dev/null; then
    echo -e "${RED}✗${NC} Fly CLI no está instalado o no está en el PATH"
    echo "Ejecuta: export PATH=\"/Users/Jefferson/.fly/bin:\$PATH\""
    exit 1
fi

# Verificar autenticación
echo -e "${GREEN}✓${NC} Verificando autenticación en Fly.io..."
if ! fly auth whoami &> /dev/null; then
    echo -e "${RED}✗${NC} No estás autenticado en Fly.io"
    echo "Ejecuta: fly auth login"
    exit 1
fi

echo -e "${GREEN}✓${NC} Autenticado como: $(fly auth whoami)"
echo ""

# Verificar si la app ya existe
APP_EXISTS=$(fly apps list | grep -c "invoices-backend" || true)

if [ "$APP_EXISTS" -eq "0" ]; then
    echo -e "${YELLOW}→${NC} La app 'invoices-backend' no existe. Creando..."

    # Crear la app
    fly launch \
        --name invoices-backend \
        --region ams \
        --no-deploy \
        --copy-config \
        --yes

    echo -e "${GREEN}✓${NC} App creada exitosamente"
else
    echo -e "${GREEN}✓${NC} La app 'invoices-backend' ya existe"
fi

echo ""
echo -e "${YELLOW}→${NC} Configurando variables de entorno (secrets)..."

# Configurar secrets
fly secrets set \
    SPRING_PROFILES_ACTIVE=prod \
    JWT_SECRET="$JWT_SECRET" \
    JWT_ISSUER=invoices-backend \
    JWT_EXPIRATION_MS=3600000 \
    CORS_ALLOWED_ORIGINS="$CORS_ALLOWED_ORIGINS" \
    EUREKA_CLIENT_ENABLED=false \
    -a invoices-backend

# Configurar database URL (solo userdb para el gateway)
fly secrets set \
    SPRING_DATASOURCE_URL="$USER_DB_URL" \
    -a invoices-backend

echo -e "${GREEN}✓${NC} Secrets configurados"
echo ""

# Desplegar
echo -e "${YELLOW}→${NC} Desplegando a Fly.io (esto tomará 3-5 minutos)..."
echo ""

fly deploy \
    --ha=false \
    --strategy immediate \
    -a invoices-backend

echo ""
echo -e "${GREEN}=================================${NC}"
echo -e "${GREEN}✓ Deployment completado!${NC}"
echo -e "${GREEN}=================================${NC}"
echo ""

# Verificar status
echo -e "${YELLOW}→${NC} Verificando estado del deployment..."
fly status -a invoices-backend

echo ""
echo -e "${GREEN}URL de tu backend:${NC}"
echo "https://invoices-backend.fly.dev"
echo ""

# Test de health check
echo -e "${YELLOW}→${NC} Verificando health check..."
sleep 5

if curl -f https://invoices-backend.fly.dev/actuator/health &> /dev/null; then
    echo -e "${GREEN}✓${NC} Backend respondiendo correctamente!"
else
    echo -e "${YELLOW}⚠${NC} El backend aún está iniciando, espera 1-2 minutos y verifica:"
    echo "curl https://invoices-backend.fly.dev/actuator/health"
fi

echo ""
echo -e "${GREEN}Próximos pasos:${NC}"
echo "1. Configurar en Vercel:"
echo "   Variable: VITE_API_BASE_URL"
echo "   Valor: https://invoices-backend.fly.dev/api"
echo ""
echo "2. Ver logs en tiempo real:"
echo "   fly logs -a invoices-backend"
echo ""
echo "3. Abrir dashboard:"
echo "   fly dashboard -a invoices-backend"

#!/bin/bash

# ===================================================================
# Script para redesplegar servicios en Fly.io con los cambios
# ===================================================================

set -e  # Exit on error

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}Redesplegando servicios en Fly.io${NC}"
echo -e "${GREEN}=====================================${NC}"

# Verificar que Fly CLI esté instalado
if ! command -v fly &> /dev/null; then
    echo -e "${RED}ERROR: Fly CLI no está instalado${NC}"
    echo "Instalar con: curl -L https://fly.io/install.sh | sh"
    exit 1
fi

# Verificar autenticación
if ! fly auth whoami &> /dev/null; then
    echo -e "${RED}ERROR: No estás autenticado en Fly.io${NC}"
    echo "Ejecuta: fly auth login"
    exit 1
fi

# Función para redesplegar un servicio
redeploy_service() {
    local service_dir=$1
    local app_name=$2

    echo ""
    echo -e "${GREEN}====================================${NC}"
    echo -e "${GREEN}Redesplegando: $app_name${NC}"
    echo -e "${GREEN}====================================${NC}"

    cd "$service_dir"

    # Redesplegar
    echo -e "${YELLOW}Construyendo y desplegando...${NC}"
    fly deploy --remote-only --ha=false

    # Verificar estado
    sleep 5
    echo -e "${YELLOW}Verificando estado...${NC}"
    fly status

    cd - > /dev/null

    echo -e "${GREEN}✅ $app_name desplegado${NC}"
}

# Main
echo ""
echo "Este script redesplegará los siguientes servicios:"
echo "  1. invoices-invoice-service (puerto 8081)"
echo "  2. invoices-user-service (puerto 8082)"
echo "  3. invoices-document-service (puerto 8083)"
echo ""
read -p "¿Continuar? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelado"
    exit 0
fi

# Redesplegar servicios
redeploy_service "invoice-service" "invoices-invoice-service"
redeploy_service "user-service" "invoices-user-service"
redeploy_service "document-service" "invoices-document-service"

echo ""
echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}✅ Todos los servicios redesplegados${NC}"
echo -e "${GREEN}=====================================${NC}"
echo ""
echo "Para ver logs en tiempo real:"
echo "  fly logs -a invoices-invoice-service"
echo "  fly logs -a invoices-user-service"
echo "  fly logs -a invoices-document-service"
echo ""
echo "Para ver el dashboard:"
echo "  fly dashboard -a invoices-invoice-service"
echo ""

#!/bin/bash

# ===================================================================
# Script de Deployment para Fly.io
# ===================================================================
#
# Este script automatiza el deployment del backend a Fly.io
#
# Prerequisitos:
# 1. Fly.io CLI instalado: curl -L https://fly.io/install.sh | sh
# 2. Autenticado en Fly.io: fly auth login
# 3. Variables de entorno configuradas (ver .env.production.example)
#
# Uso:
#   ./deploy-flyio.sh gateway     # Desplegar solo gateway
#   ./deploy-flyio.sh all         # Desplegar todos los servicios
#
# ===================================================================

set -e  # Exit on error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para logging
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar que Fly CLI esté instalado
if ! command -v fly &> /dev/null; then
    log_error "Fly CLI no está instalado"
    echo "Instalar con: curl -L https://fly.io/install.sh | sh"
    exit 1
fi

# Verificar autenticación
if ! fly auth whoami &> /dev/null; then
    log_error "No estás autenticado en Fly.io"
    echo "Ejecuta: fly auth login"
    exit 1
fi

# Cargar variables de entorno si existen
if [ -f .env.production ]; then
    log_info "Cargando variables de .env.production..."
    set -a
    source .env.production
    set +a
else
    log_warn "No se encontró .env.production, usa .env.production.example como base"
fi

# Función para desplegar un servicio
deploy_service() {
    local service_name=$1
    local service_dir=$2
    local fly_app_name=$3

    log_info "====================================="
    log_info "Desplegando: $service_name"
    log_info "====================================="

    cd "$service_dir"

    # Verificar si la app ya existe
    if fly apps list | grep -q "$fly_app_name"; then
        log_info "App '$fly_app_name' ya existe, actualizando..."
        fly deploy
    else
        log_info "Creando nueva app '$fly_app_name'..."
        fly launch --name "$fly_app_name" --no-deploy --region mia

        # Configurar secrets
        log_info "Configurando variables de entorno..."

        if [ "$service_name" == "gateway" ]; then
            fly secrets set \
                SPRING_PROFILES_ACTIVE=prod \
                JWT_SECRET="${JWT_SECRET}" \
                CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS}" \
                EUREKA_CLIENT_ENABLED=false
        else
            fly secrets set \
                SPRING_PROFILES_ACTIVE=prod \
                JWT_SECRET="${JWT_SECRET}" \
                EUREKA_CLIENT_ENABLED=false
        fi

        # Deploy
        log_info "Desplegando..."
        fly deploy
    fi

    # Verificar health
    log_info "Verificando health check..."
    sleep 5
    if fly status | grep -q "healthy"; then
        log_info "✅ $service_name deployado exitosamente"
    else
        log_warn "⚠️  $service_name desplegado pero health check pendiente"
    fi

    cd ..
}

# Main script
case "${1:-gateway}" in
    gateway)
        log_info "Desplegando solo Gateway Service..."
        deploy_service "gateway" "./gateway-service" "invoices-backend"
        ;;

    all)
        log_info "Desplegando todos los servicios..."

        deploy_service "gateway" "./gateway-service" "invoices-backend"
        deploy_service "user-service" "./user-service" "invoices-user-service"
        deploy_service "invoice-service" "./invoice-service" "invoices-invoice-service"

        log_info ""
        log_info "====================================="
        log_info "✅ Deployment completado!"
        log_info "====================================="
        ;;

    *)
        log_error "Opción inválida: $1"
        echo "Uso: $0 {gateway|all}"
        exit 1
        ;;
esac

# Mostrar URLs
log_info ""
log_info "====================================="
log_info "URLs de tus servicios:"
log_info "====================================="
fly apps list | grep invoices

log_info ""
log_info "Para ver logs en tiempo real:"
log_info "  fly logs -a invoices-backend"
log_info ""
log_info "Para abrir el dashboard:"
log_info "  fly dashboard -a invoices-backend"

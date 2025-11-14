#!/bin/bash

#############################################
# Script para configurar variables de entorno
# Uso: source ./setup-env.sh [profile]
#############################################

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROFILE=${1:-dev}

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════╗"
echo "║   🔧 Environment Setup - Profile: $PROFILE"
echo "╔════════════════════════════════════════════╗"
echo -e "${NC}"

# Config Server
export CONFIG_PROFILE=$PROFILE
echo -e "${GREEN}✅ CONFIG_PROFILE=${CONFIG_PROFILE}${NC}"

# Trace Service - Dead Letter Queue (CRÍTICO)
export KAFKA_INVOICE_DLQ_TOPIC=invoice-events-dlq
echo -e "${GREEN}✅ KAFKA_INVOICE_DLQ_TOPIC=${KAFKA_INVOICE_DLQ_TOPIC}${NC}"

# Configuraciones específicas por profile
case $PROFILE in
    dev)
        echo -e "\n${YELLOW}📝 Configuración Development:${NC}"
        export LOG_LEVEL_ROOT=DEBUG
        export LOG_LEVEL_APP=DEBUG
        export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
        export MINIO_ENDPOINT=http://localhost:9000
        export MINIO_ACCESS_KEY=minioadmin
        export MINIO_SECRET_KEY=minioadmin
        export MINIO_BUCKET_NAME=invoices-dev

        echo -e "${GREEN}  - Logging: DEBUG${NC}"
        echo -e "${GREEN}  - Kafka: localhost:9092${NC}"
        echo -e "${GREEN}  - MinIO: localhost:9000${NC}"
        ;;

    test)
        echo -e "\n${YELLOW}📝 Configuración Test:${NC}"
        export LOG_LEVEL_ROOT=INFO
        export LOG_LEVEL_APP=DEBUG
        export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
        export MINIO_ENDPOINT=http://localhost:9000
        export MINIO_ACCESS_KEY=minioadmin
        export MINIO_SECRET_KEY=minioadmin
        export MINIO_BUCKET_NAME=invoices-test

        echo -e "${GREEN}  - Logging: INFO${NC}"
        echo -e "${GREEN}  - Kafka: localhost:9092${NC}"
        echo -e "${GREEN}  - MinIO: localhost:9000${NC}"
        ;;

    prod)
        echo -e "\n${YELLOW}📝 Configuración Production:${NC}"
        export LOG_LEVEL_ROOT=WARN
        export LOG_LEVEL_APP=INFO

        # En producción, estas deben venir de secrets manager
        if [ -z "$KAFKA_BOOTSTRAP_SERVERS" ]; then
            echo -e "${YELLOW}⚠️  WARNING: KAFKA_BOOTSTRAP_SERVERS no configurado${NC}"
        fi

        if [ -z "$MINIO_ENDPOINT" ]; then
            echo -e "${YELLOW}⚠️  WARNING: MINIO_ENDPOINT no configurado${NC}"
        fi

        echo -e "${GREEN}  - Logging: WARN${NC}"
        echo -e "${YELLOW}  - NOTA: Configurar secrets de producción manualmente${NC}"
        ;;

    *)
        echo -e "${YELLOW}⚠️  WARNING: Profile desconocido: $PROFILE${NC}"
        echo -e "${YELLOW}    Perfiles válidos: dev, test, prod${NC}"
        ;;
esac

# Mostrar todas las variables configuradas
echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Variables de Entorno Configuradas:${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo -e "${GREEN}Config Server:${NC}"
echo "  CONFIG_PROFILE=$CONFIG_PROFILE"
echo "  LOG_LEVEL_ROOT=$LOG_LEVEL_ROOT"
echo "  LOG_LEVEL_APP=$LOG_LEVEL_APP"

echo -e "\n${GREEN}Trace Service (CRÍTICO):${NC}"
echo "  KAFKA_INVOICE_DLQ_TOPIC=$KAFKA_INVOICE_DLQ_TOPIC"
echo "  KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS:-'not set'}"

echo -e "\n${GREEN}Document Service:${NC}"
echo "  MINIO_ENDPOINT=${MINIO_ENDPOINT:-'not set'}"
echo "  MINIO_BUCKET_NAME=${MINIO_BUCKET_NAME:-'not set'}"

echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ Variables configuradas para profile: $PROFILE${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

# Instrucciones
echo -e "${YELLOW}💡 Próximos pasos:${NC}"
echo "  1. Iniciar servicios: docker-compose up -d"
echo "  2. Ejecutar tests: ./run-tests.sh all"
echo "  3. Ver reportes: ./run-tests.sh report all"
echo ""
echo -e "${YELLOW}📚 Para más información:${NC}"
echo "  - Variables de entorno: cat ENVIRONMENT_VARIABLES.md"
echo "  - Guía de testing: cat TESTING_GUIDE.md"

#!/bin/bash

# 🚀 Build Local Rápido - Compila el JAR localmente
# Uso: ./build-local-fast.sh

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/invoices-monolith"

clear

echo -e "${MAGENTA}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║      🔨 BUILD LOCAL RÁPIDO - MAVEN ⚡                  ║"
echo "║                                                        ║"
echo "║          Compilación optimizada del proyecto          ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}\n"

# Verificar Maven
echo -e "${BLUE}[1/3]${NC} Verificando Maven..."
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven no está instalado${NC}"
    echo -e "${YELLOW}Instálalo con: brew install maven${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Maven instalado: $(mvn -v | head -1)${NC}\n"

# Limpiar builds anteriores
echo -e "${BLUE}[2/3]${NC} Limpiando builds anteriores..."
if [ -d "target" ]; then
    rm -rf target
    echo -e "${GREEN}✅ Directorio target limpiado${NC}\n"
else
    echo -e "${GREEN}✅ No hay builds anteriores${NC}\n"
fi

# Build con optimizaciones
echo -e "${BLUE}[3/3]${NC} Compilando proyecto..."
echo -e "${CYAN}Esto puede tomar 2-3 minutos...${NC}\n"
echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}"

# Build con optimizaciones para velocidad
MAVEN_OPTS="-Xmx2048m -XX:+TieredCompilation -XX:TieredStopAtLevel=1" \
    mvn clean package -DskipTests -B -T 2C \
    -Dmaven.compiler.debug=false \
    -Dmaven.compiler.debuglevel=none \
    -Dmaven.javadoc.skip=true

BUILD_EXIT_CODE=$?

echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}\n"

if [ $BUILD_EXIT_CODE -eq 0 ]; then
    # Obtener nombre del JAR
    JAR_FILE=$(ls target/*.jar 2>/dev/null | head -1)
    JAR_SIZE=$(du -h "$JAR_FILE" 2>/dev/null | cut -f1)

    echo -e "${GREEN}"
    echo "╔════════════════════════════════════════════════════════╗"
    echo "║                                                        ║"
    echo "║              ✅ BUILD EXITOSO ✅                       ║"
    echo "║                                                        ║"
    echo "╚════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo -e "${GREEN}JAR generado:${NC} ${JAR_FILE}"
    echo -e "${GREEN}Tamaño:${NC} ${JAR_SIZE}\n"

    echo -e "${CYAN}Opciones para deployment:${NC}"
    echo -e "  1) Deploy rápido a Fly.io con JAR pre-compilado:"
    echo -e "     ${YELLOW}cd invoices-monolith && fly deploy --dockerfile Dockerfile.prebuilt${NC}"
    echo -e ""
    echo -e "  2) Ejecutar localmente:"
    echo -e "     ${YELLOW}cd invoices-monolith && java -jar ${JAR_FILE}${NC}"
    echo -e ""
else
    echo -e "${RED}"
    echo "╔════════════════════════════════════════════════════════╗"
    echo "║                                                        ║"
    echo "║              ❌ BUILD FALLÓ ❌                         ║"
    echo "║                                                        ║"
    echo "╚════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo -e "${YELLOW}Revisa los errores anteriores${NC}\n"
    exit 1
fi

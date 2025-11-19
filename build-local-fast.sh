#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/invoices-monolith"

clear

echo -e "${MAGENTA}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║      🔨 BUILD LOCAL RÁPIDO - MAVEN ⚡                  ║${NC}"
echo -e "${MAGENTA}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

echo -e "${BLUE}[1/3]${NC} Verificando Maven..."
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven no está instalado${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Maven instalado${NC}"
echo ""

echo -e "${BLUE}[2/3]${NC} Limpiando builds anteriores..."
if [ -d "target" ]; then
    rm -rf target
    echo -e "${GREEN}✅ Directorio target limpiado${NC}"
else
    echo -e "${GREEN}✅ No hay builds anteriores${NC}"
fi
echo ""

echo -e "${BLUE}[3/3]${NC} Compilando proyecto..."
echo -e "${CYAN}Esto puede tomar 2-3 minutos...${NC}"
echo ""

MAVEN_OPTS="-Xmx2048m -XX:+TieredCompilation -XX:TieredStopAtLevel=1" \
    mvn clean package -DskipTests -B -T 2C \
    -Dmaven.compiler.debug=false \
    -Dmaven.compiler.debuglevel=none \
    -Dmaven.javadoc.skip=true

BUILD_EXIT_CODE=$?

echo ""

if [ $BUILD_EXIT_CODE -eq 0 ]; then
    JAR_FILE=$(ls target/*.jar 2>/dev/null | head -1)
    JAR_SIZE=$(du -h "$JAR_FILE" 2>/dev/null | cut -f1)

    echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║              ✅ BUILD EXITOSO ✅                       ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${GREEN}JAR generado:${NC} ${JAR_FILE}"
    echo -e "${GREEN}Tamaño:${NC} ${JAR_SIZE}"
    echo ""
    echo -e "${CYAN}Siguiente paso:${NC}"
    echo -e "  ${YELLOW}fly deploy --dockerfile Dockerfile.prebuilt${NC}"
    echo ""
else
    echo -e "${RED}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║              ❌ BUILD FALLÓ ❌                         ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    exit 1
fi

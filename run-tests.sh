#!/bin/bash

# ============================================================================
# Script de ejecución de tests para Invoices Monolith
# ============================================================================

set -e  # Exit on error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Banner
echo -e "${BLUE}"
echo "╔════════════════════════════════════════════╗"
echo "║   🧪 Invoices Monolith - Test Runner      ║"
echo "╚════════════════════════════════════════════╝"
echo -e "${NC}"

# Función para ejecutar tests
run_tests() {
    echo -e "${CYAN}📦 Navegando al directorio del monolito...${NC}"
    cd invoices-monolith

    echo -e "${CYAN}🧹 Limpiando builds anteriores...${NC}"
    mvn clean -q

    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}🧪 Ejecutando todos los tests...${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    if mvn test; then
        echo ""
        echo -e "${GREEN}✅ Tests completados exitosamente${NC}"

        echo ""
        echo -e "${CYAN}📊 Generando reporte de cobertura con JaCoCo...${NC}"
        mvn jacoco:report -q

        echo ""
        echo -e "${GREEN}✅ Reporte de cobertura generado${NC}"
    else
        echo ""
        echo -e "${RED}❌ Tests fallaron${NC}"
        cd ..
        exit 1
    fi

    cd ..
}

# Función para abrir reportes
open_reports() {
    local report_path="invoices-monolith/target/site/jacoco/index.html"

    if [ -f "$report_path" ]; then
        echo ""
        echo -e "${YELLOW}📊 Abriendo reporte JaCoCo...${NC}"

        # Detectar OS y abrir reporte
        case "$(uname -s)" in
            Darwin*)
                open "$report_path"
                ;;
            Linux*)
                xdg-open "$report_path" 2>/dev/null || echo -e "${YELLOW}Por favor, abre manualmente: $report_path${NC}"
                ;;
            MINGW*|MSYS*|CYGWIN*)
                start "$report_path"
                ;;
            *)
                echo -e "${YELLOW}Sistema operativo no reconocido. Abre manualmente: $report_path${NC}"
                ;;
        esac
    else
        echo -e "${YELLOW}❌ Reporte no encontrado: $report_path${NC}"
        echo -e "${YELLOW}Ejecuta los tests primero: ./run-tests.sh${NC}"
    fi
}

# Función para limpiar
clean_all() {
    echo -e "${YELLOW}🧹 Limpiando targets del monolito...${NC}"
    cd invoices-monolith
    mvn clean -q
    cd ..
    echo -e "${GREEN}✅ Limpieza completada${NC}"
}

# Función para mostrar ayuda
show_help() {
    echo -e "${YELLOW}Uso:${NC}"
    echo "  ./run-tests.sh [opción]"
    echo ""
    echo -e "${YELLOW}Opciones:${NC}"
    echo "  (sin opción)    Ejecutar todos los tests del monolito"
    echo "  report          Abrir reporte JaCoCo"
    echo "  clean           Limpiar todos los targets"
    echo "  help            Mostrar esta ayuda"
    echo ""
    echo -e "${YELLOW}Ejemplos:${NC}"
    echo "  ./run-tests.sh           # Ejecutar todos los tests"
    echo "  ./run-tests.sh report    # Ver reporte de cobertura"
    echo "  ./run-tests.sh clean     # Limpiar builds"
}

# Función para mostrar resumen
show_summary() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}✅ EJECUCIÓN COMPLETADA${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${YELLOW}📊 Reportes generados:${NC}"
    echo -e "   • JaCoCo Coverage: ${CYAN}invoices-monolith/target/site/jacoco/index.html${NC}"
    echo -e "   • Surefire Reports: ${CYAN}invoices-monolith/target/surefire-reports/${NC}"
    echo ""
    echo -e "${YELLOW}💡 Comandos útiles:${NC}"
    echo -e "   • Ver reporte: ${CYAN}./run-tests.sh report${NC}"
    echo -e "   • Limpiar: ${CYAN}./run-tests.sh clean${NC}"
    echo ""
}

# Función principal
main() {
    local command=${1:-test}

    case $command in
        test|"")
            run_tests
            show_summary
            ;;

        report)
            open_reports
            ;;

        clean)
            clean_all
            ;;

        help|--help|-h)
            show_help
            ;;

        *)
            echo -e "${RED}❌ ERROR: Comando desconocido: $command${NC}"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# Ejecutar
main "$@"

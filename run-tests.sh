#!/bin/bash

#############################################
# Script para ejecutar tests y ver reportes
# Uso: ./run-tests.sh [servicio]
#############################################

set -e  # Exit on error

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Banner
echo -e "${BLUE}"
echo "╔════════════════════════════════════════════╗"
echo "║   🧪 Invoices Backend - Test Runner       ║"
echo "╔════════════════════════════════════════════╗"
echo -e "${NC}"

# Función para verificar Docker
check_docker() {
    echo -e "${YELLOW}🐳 Verificando Docker...${NC}"
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}❌ ERROR: Docker no está corriendo${NC}"
        echo -e "${YELLOW}Por favor, inicia Docker Desktop y vuelve a intentar${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Docker está corriendo${NC}"
}

# Función para ejecutar tests de un servicio
run_service_tests() {
    local service=$1
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Testing: ${service}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

    cd "$service"

    # Ejecutar tests
    if mvn clean test; then
        echo -e "\n${GREEN}✅ Tests passed for ${service}${NC}"

        # Verificar JaCoCo
        if mvn jacoco:check; then
            echo -e "${GREEN}✅ Coverage meets requirements for ${service}${NC}"
        else
            echo -e "${RED}❌ Coverage below minimum for ${service}${NC}"
        fi
    else
        echo -e "\n${RED}❌ Tests failed for ${service}${NC}"
        cd ..
        return 1
    fi

    cd ..
}

# Función para abrir reportes JaCoCo
open_reports() {
    local service=$1
    local report_path="${service}/target/site/jacoco/index.html"

    if [ -f "$report_path" ]; then
        echo -e "${GREEN}📊 Abriendo reporte JaCoCo para ${service}...${NC}"

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
        echo -e "${RED}❌ Reporte no encontrado: $report_path${NC}"
        echo -e "${YELLOW}Ejecuta los tests primero: ./run-tests.sh ${service}${NC}"
    fi
}

# Función para mostrar ayuda
show_help() {
    echo -e "${YELLOW}Uso:${NC}"
    echo "  ./run-tests.sh [opción]"
    echo ""
    echo -e "${YELLOW}Opciones:${NC}"
    echo "  all              Ejecutar todos los tests"
    echo "  document         Ejecutar tests de Document Service"
    echo "  trace            Ejecutar tests de Trace Service"
    echo "  gateway          Ejecutar tests de Gateway Service"
    echo "  report [service] Abrir reporte JaCoCo del servicio"
    echo "  clean            Limpiar todos los targets"
    echo "  help             Mostrar esta ayuda"
    echo ""
    echo -e "${YELLOW}Ejemplos:${NC}"
    echo "  ./run-tests.sh all"
    echo "  ./run-tests.sh document"
    echo "  ./run-tests.sh report trace"
    echo "  ./run-tests.sh clean"
}

# Función para limpiar
clean_all() {
    echo -e "${YELLOW}🧹 Limpiando targets...${NC}"
    for service in document-service trace-service gateway-service; do
        if [ -d "$service" ]; then
            echo -e "  Limpiando ${service}..."
            cd "$service"
            mvn clean -q
            cd ..
        fi
    done
    echo -e "${GREEN}✅ Limpieza completada${NC}"
}

# Función principal
main() {
    local command=${1:-help}

    case $command in
        all)
            check_docker
            echo -e "${BLUE}🚀 Ejecutando TODOS los tests...${NC}\n"

            # Ejecutar tests en secuencia
            run_service_tests "document-service"
            run_service_tests "trace-service"
            run_service_tests "gateway-service"

            echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
            echo -e "${GREEN}✅ TODOS LOS TESTS COMPLETADOS${NC}"
            echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

            echo -e "${YELLOW}📊 Para ver los reportes JaCoCo:${NC}"
            echo -e "  ./run-tests.sh report document"
            echo -e "  ./run-tests.sh report trace"
            echo -e "  ./run-tests.sh report gateway"
            ;;

        document)
            check_docker
            run_service_tests "document-service"
            echo -e "\n${YELLOW}Para ver el reporte: ./run-tests.sh report document${NC}"
            ;;

        trace)
            check_docker
            run_service_tests "trace-service"
            echo -e "\n${YELLOW}Para ver el reporte: ./run-tests.sh report trace${NC}"
            ;;

        gateway)
            run_service_tests "gateway-service"
            echo -e "\n${YELLOW}Para ver el reporte: ./run-tests.sh report gateway${NC}"
            ;;

        report)
            local service_name=${2:-}
            if [ -z "$service_name" ]; then
                echo -e "${RED}❌ ERROR: Especifica un servicio${NC}"
                echo -e "${YELLOW}Ejemplo: ./run-tests.sh report document${NC}"
                exit 1
            fi

            case $service_name in
                document)
                    open_reports "document-service"
                    ;;
                trace)
                    open_reports "trace-service"
                    ;;
                gateway)
                    open_reports "gateway-service"
                    ;;
                all)
                    open_reports "document-service"
                    open_reports "trace-service"
                    open_reports "gateway-service"
                    ;;
                *)
                    echo -e "${RED}❌ ERROR: Servicio desconocido: $service_name${NC}"
                    echo -e "${YELLOW}Servicios válidos: document, trace, gateway, all${NC}"
                    exit 1
                    ;;
            esac
            ;;

        clean)
            clean_all
            ;;

        help|--help|-h)
            show_help
            ;;

        *)
            echo -e "${RED}❌ ERROR: Comando desconocido: $command${NC}\n"
            show_help
            exit 1
            ;;
    esac
}

# Ejecutar
main "$@"

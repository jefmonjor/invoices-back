#!/bin/bash

# Script para crear Pull Request de producción
# Mergea la branch de fixes hacia main/master

set -e

echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║     📝 CREAR PULL REQUEST PARA PRODUCCIÓN              ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variables
BRANCH_NAME="claude/review-agent-errors-018NYMM9tidter6F6bDnidwW"
PR_TITLE="Fix: Backend Compilation Errors & Production Deployment Readiness"

echo -e "${BLUE}Branch actual:${NC}"
git branch --show-current
echo ""

echo -e "${BLUE}Últimos commits a incluir en el PR:${NC}"
git log --oneline -4
echo ""

# Verificar que no haya cambios sin commitear
if [[ -n $(git status -s) ]]; then
    echo -e "${YELLOW}⚠️  Hay cambios sin commitear:${NC}"
    git status -s
    echo ""
    read -p "¿Quieres continuar de todas formas? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Cancelado."
        exit 1
    fi
fi

echo ""
echo "════════════════════════════════════════════════════════"
echo "  OPCIONES PARA CREAR EL PULL REQUEST"
echo "════════════════════════════════════════════════════════"
echo ""
echo "Elige una opción:"
echo ""
echo "  1) Usar GitHub CLI (gh) - RECOMENDADO"
echo "  2) Crear manualmente en GitHub Web"
echo "  3) Ver URL directa de GitHub"
echo "  4) Cancelar"
echo ""
read -p "Selecciona una opción (1-4): " option

case $option in
    1)
        echo ""
        echo -e "${BLUE}Verificando GitHub CLI...${NC}"

        if ! command -v gh &> /dev/null; then
            echo -e "${YELLOW}⚠️  GitHub CLI no está instalado.${NC}"
            echo ""
            echo "Instala GitHub CLI:"
            echo "  macOS: brew install gh"
            echo "  Linux: https://github.com/cli/cli/blob/trunk/docs/install_linux.md"
            echo ""
            exit 1
        fi

        echo -e "${GREEN}✓${NC} GitHub CLI encontrado"
        echo ""

        # Verificar autenticación
        if ! gh auth status &> /dev/null; then
            echo -e "${YELLOW}⚠️  No estás autenticado con GitHub CLI${NC}"
            echo ""
            echo "Ejecuta: gh auth login"
            echo ""
            exit 1
        fi

        echo -e "${GREEN}✓${NC} Autenticado con GitHub"
        echo ""

        # Determinar base branch (main o master)
        echo -e "${BLUE}Detectando branch principal...${NC}"

        # Intentar obtener default branch del repositorio
        BASE_BRANCH=$(gh repo view --json defaultBranchRef --jq .defaultBranchRef.name 2>/dev/null || echo "main")

        echo -e "${GREEN}✓${NC} Base branch: ${BASE_BRANCH}"
        echo ""

        # Crear PR
        echo -e "${BLUE}Creando Pull Request...${NC}"
        echo ""

        gh pr create \
            --title "$PR_TITLE" \
            --body-file PR_DESCRIPTION.md \
            --base "$BASE_BRANCH" \
            --head "$BRANCH_NAME"

        echo ""
        echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}✅ Pull Request creado exitosamente!${NC}"
        echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
        echo ""
        echo "Siguiente paso:"
        echo "  1. Revisa el PR en GitHub"
        echo "  2. Si todo está bien, aprueba y mergea"
        echo "  3. Ejecuta: fly deploy -a invoices-monolith"
        echo ""
        ;;

    2)
        echo ""
        echo -e "${BLUE}Instrucciones para crear PR manualmente:${NC}"
        echo ""
        echo "1. Ve a GitHub:"
        echo "   https://github.com/jefmonjor/invoices-back/compare"
        echo ""
        echo "2. Selecciona:"
        echo "   base: main (o master)"
        echo "   compare: $BRANCH_NAME"
        echo ""
        echo "3. Click en 'Create pull request'"
        echo ""
        echo "4. Copia el contenido de PR_DESCRIPTION.md como descripción"
        echo "   Archivo: $(pwd)/PR_DESCRIPTION.md"
        echo ""
        echo "5. Click en 'Create pull request'"
        echo ""
        ;;

    3)
        echo ""
        echo -e "${BLUE}URL Directa de GitHub:${NC}"
        echo ""
        echo "https://github.com/jefmonjor/invoices-back/compare/main...$BRANCH_NAME"
        echo ""
        echo "O si el branch principal es 'master':"
        echo "https://github.com/jefmonjor/invoices-back/compare/master...$BRANCH_NAME"
        echo ""
        echo -e "${YELLOW}Nota:${NC} Copia el contenido de PR_DESCRIPTION.md como descripción"
        echo "      Archivo: $(pwd)/PR_DESCRIPTION.md"
        echo ""
        ;;

    4)
        echo ""
        echo "Cancelado."
        exit 0
        ;;

    *)
        echo ""
        echo "Opción inválida. Cancelado."
        exit 1
        ;;
esac

echo ""
echo "════════════════════════════════════════════════════════"
echo ""
echo -e "${BLUE}Resumen de commits incluidos en el PR:${NC}"
echo ""
git log --oneline -4
echo ""
echo "════════════════════════════════════════════════════════"

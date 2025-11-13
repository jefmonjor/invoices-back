#!/bin/bash

# Script para crear las Pull Requests pendientes
# Requiere: GitHub CLI (gh) instalado y autenticado
# Uso: ./create_prs.sh

set -e

echo "======================================"
echo "Creando Pull Requests Pendientes"
echo "======================================"
echo ""

# Verificar que gh está instalado
if ! command -v gh &> /dev/null; then
    echo "❌ ERROR: GitHub CLI (gh) no está instalado"
    echo ""
    echo "Instalar desde: https://cli.github.com/"
    echo ""
    echo "O crea las PRs manualmente desde:"
    echo "- PR #1: https://github.com/jefmonjor/invoices-back/compare/master...claude/revisa-que-011CV4nceDGc2JJoj53PvbEZ"
    echo "- PR #2: https://github.com/jefmonjor/invoices-back/compare/master...claude/clean-architecture-readme-011CV4goSCKxmAXeB3kTR9yy"
    exit 1
fi

# Verificar que estamos en el repositorio correcto
if [ ! -d ".git" ]; then
    echo "❌ ERROR: Este script debe ejecutarse desde la raíz del repositorio"
    exit 1
fi

echo "✅ GitHub CLI detectado"
echo ""

# PR #1: Documentación
echo "📝 Creando PR #1: Documentación Técnica..."
echo ""

gh pr create \
  --head claude/revisa-que-011CV4nceDGc2JJoj53PvbEZ \
  --base master \
  --title "docs: añadir documentación técnica completa y mejores prácticas" \
  --body-file PR1_BODY.md \
  --label documentation,enhancement

PR1_STATUS=$?

if [ $PR1_STATUS -eq 0 ]; then
    echo "✅ PR #1 creada exitosamente"
else
    echo "⚠️  PR #1 falló (puede que ya exista)"
fi

echo ""
echo "---"
echo ""

# PR #2: Clean Architecture + Tests
echo "🏗️  Creando PR #2: Clean Architecture + Tests..."
echo ""

gh pr create \
  --head claude/clean-architecture-readme-011CV4goSCKxmAXeB3kTR9yy \
  --base master \
  --title "feat: implementar Clean Architecture en invoice-service con 90%+ test coverage" \
  --body-file PR2_BODY.md \
  --label enhancement,architecture,testing,needs-discussion

PR2_STATUS=$?

if [ $PR2_STATUS -eq 0 ]; then
    echo "✅ PR #2 creada exitosamente"
else
    echo "⚠️  PR #2 falló (puede que ya exista)"
fi

echo ""
echo "======================================"
echo "Resumen"
echo "======================================"
echo ""

if [ $PR1_STATUS -eq 0 ] && [ $PR2_STATUS -eq 0 ]; then
    echo "✅ Todas las PRs creadas exitosamente"
    echo ""
    echo "Ver PRs en:"
    echo "https://github.com/jefmonjor/invoices-back/pulls"
elif [ $PR1_STATUS -eq 0 ] || [ $PR2_STATUS -eq 0 ]; then
    echo "⚠️  Algunas PRs fueron creadas"
    echo ""
    echo "Ver PRs en:"
    echo "https://github.com/jefmonjor/invoices-back/pulls"
else
    echo "⚠️  No se pudo crear ninguna PR"
    echo ""
    echo "Posibles razones:"
    echo "- Las PRs ya existen"
    echo "- No tienes permisos"
    echo "- Problema de conectividad"
    echo ""
    echo "Crea las PRs manualmente desde GitHub UI:"
    echo "- PR #1: https://github.com/jefmonjor/invoices-back/compare/master...claude/revisa-que-011CV4nceDGc2JJoj53PvbEZ"
    echo "- PR #2: https://github.com/jefmonjor/invoices-back/compare/master...claude/clean-architecture-readme-011CV4goSCKxmAXeB3kTR9yy"
fi

echo ""
echo "======================================"
echo "Próximos Pasos"
echo "======================================"
echo ""
echo "1. Revisar las PRs en GitHub"
echo "2. Asignar revisores"
echo "3. Ejecutar tests: cd invoice-service && mvn test"
echo "4. Mergear PR #1 primero (sin conflictos)"
echo "5. Decidir estrategia para PR #2 (ver PR_INSTRUCTIONS.md)"
echo ""
echo "Para más información, ver: PR_INSTRUCTIONS.md"
echo ""

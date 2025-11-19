#!/bin/bash

# Script para crear el Pull Request de Railway deployment fixes
# Ejecutar con: bash create-railway-pr.sh

set -e

echo "🚀 Creando Pull Request para Railway deployment fixes..."
echo ""

# Verificar que gh está instalado
if ! command -v gh &> /dev/null; then
    echo "❌ Error: GitHub CLI (gh) no está instalado"
    echo ""
    echo "📋 Instrucciones para crear el PR manualmente:"
    echo ""
    echo "1. Ve a: https://github.com/jefmonjor/invoices-back/compare/main...claude/setup-spring-boot-invoices-01Xzi9FpmYqnjMKXXiyutfY7"
    echo ""
    echo "2. Haz clic en 'Create pull request'"
    echo ""
    echo "3. Título:"
    echo "   fix: Railway deployment and local development setup"
    echo ""
    echo "4. Copia la descripción del archivo PR_DESCRIPTION.md"
    echo ""
    echo "5. Haz clic en 'Create pull request'"
    echo ""
    echo "6. Haz clic en 'Merge pull request'"
    echo ""
    echo "7. Haz clic en 'Confirm merge'"
    echo ""
    exit 1
fi

# Crear el PR
gh pr create \
  --title "fix: Railway deployment and local development setup" \
  --base main \
  --head claude/setup-spring-boot-invoices-01Xzi9FpmYqnjMKXXiyutfY7 \
  --body-file PR_DESCRIPTION.md

echo ""
echo "✅ Pull Request creado exitosamente!"
echo ""
echo "🔄 Próximos pasos:"
echo "1. Revisa el PR en GitHub"
echo "2. Haz merge del PR"
echo "3. Railway desplegará automáticamente"
echo ""

#!/bin/bash
# Script de reparación de Flyway para producción
# Ejecutar este script en tu entorno local con las credenciales correctas

# IMPORTANTE: Reemplaza estas variables con tus credenciales reales
DB_URL="jdbc:postgresql://ep-delicate-snow-abyzqltv-pooler.eu-west-2.aws.neon.tech:5432/neondb?sslmode=require"
DB_USER="TU_USUARIO_AQUI"  # ⚠️ CAMBIAR
DB_PASSWORD="TU_PASSWORD_AQUI"  # ⚠️ CAMBIAR

echo "========================================"
echo "Flyway Repair - Producción"
echo "========================================"
echo ""
echo "Base de datos: $DB_URL"
echo "Usuario: $DB_USER"
echo ""
echo "⚠️  ADVERTENCIA: Esto actualizará los checksums en la base de datos de producción"
echo ""
read -p "¿Continuar? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Operación cancelada."
    exit 1
fi

echo ""
echo "Ejecutando flyway:repair..."
echo ""

cd /Users/Jefferson/Documents/proyecto/invoices-back/invoices-monolith

mvn flyway:repair \
    -Dflyway.url="$DB_URL" \
    -Dflyway.user="$DB_USER" \
    -Dflyway.password="$DB_PASSWORD"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Flyway repair completado exitosamente"
    echo ""
    echo "Siguiente paso: Redeploy de la aplicación en Railway"
else
    echo ""
    echo "❌ Error al ejecutar flyway:repair"
    echo "Verifica las credenciales de la base de datos"
    exit 1
fi

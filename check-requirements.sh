#!/bin/bash

# ================================================================
# QUICK DEPLOYMENT CHECKLIST
# ================================================================
# Usa este script para verificar que tienes todo listo antes de desplegar
# ================================================================

echo "🔍 Verificando configuración previa..."
echo ""

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Contador
READY=0
TOTAL=0

# 1. Neon PostgreSQL
echo "📊 1. Neon PostgreSQL"
TOTAL=$((TOTAL+1))
echo "   ¿Tienes las 4 databases creadas? (userdb, invoicedb, documentdb, tracedb)"
echo "   URL: https://console.neon.tech/"
read -p "   [y/n]: " neon
if [ "$neon" = "y" ]; then
    READY=$((READY+1))
    echo -e "   ${GREEN}✓ Neon configurado${NC}"
else
    echo -e "   ${RED}✗ Pendiente configurar Neon${NC}"
fi
echo ""

# 2. Upstash Redis
echo "🔴 2. Upstash Redis"
TOTAL=$((TOTAL+1))
echo "   Host actual: subtle-parrot-38179.upstash.io"
echo "   ¿Tienes el password de Redis?"
read -p "   [y/n]: " redis
if [ "$redis" = "y" ]; then
    READY=$((READY+1))
    echo -e "   ${GREEN}✓ Redis configurado${NC}"
else
    echo -e "   ${RED}✗ Pendiente password de Redis${NC}"
fi
echo ""

# 3. Cloudflare R2
echo "☁️  3. Cloudflare R2"
TOTAL=$((TOTAL+1))
echo "   ¿Creaste el bucket 'invoices-documents'?"
echo "   ¿Tienes Access Key y Secret Key?"
read -p "   [y/n]: " r2
if [ "$r2" = "y" ]; then
    READY=$((READY+1))
    echo -e "   ${GREEN}✓ R2 configurado${NC}"
else
    echo -e "   ${RED}✗ Pendiente configurar R2${NC}"
fi
echo ""

# 4. JWT Secret
echo "🔐 4. JWT Secret"
TOTAL=$((TOTAL+1))
echo "   ¿Generaste un JWT secret?"
echo "   Comando: openssl rand -base64 64 | tr -d '\n'"
read -p "   [y/n]: " jwt
if [ "$jwt" = "y" ]; then
    READY=$((READY+1))
    echo -e "   ${GREEN}✓ JWT secret generado${NC}"
else
    echo -e "   ${RED}✗ Pendiente generar JWT${NC}"
    echo "   Ejecuta: openssl rand -base64 64 | tr -d '\n'"
fi
echo ""

# 5. Fly.io CLI
echo "✈️  5. Fly.io CLI"
TOTAL=$((TOTAL+1))
if command -v fly &> /dev/null; then
    READY=$((READY+1))
    echo -e "   ${GREEN}✓ Fly CLI instalado${NC}"
else
    echo -e "   ${RED}✗ Fly CLI no instalado${NC}"
    echo "   Instalar: curl -L https://fly.io/install.sh | sh"
fi
echo ""

# 6. Fly.io Login
echo "🔑 6. Fly.io Login"
TOTAL=$((TOTAL+1))
if fly auth whoami &> /dev/null; then
    READY=$((READY+1))
    echo -e "   ${GREEN}✓ Autenticado en Fly.io${NC}"
else
    echo -e "   ${RED}✗ No autenticado en Fly.io${NC}"
    echo "   Ejecuta: fly auth login"
fi
echo ""

# 7. Render Account
echo "🎨 7. Render Account"
TOTAL=$((TOTAL+1))
echo "   ¿Tienes cuenta en Render.com?"
read -p "   [y/n]: " render
if [ "$render" = "y" ]; then
    READY=$((READY+1))
    echo -e "   ${GREEN}✓ Cuenta de Render lista${NC}"
else
    echo -e "   ${RED}✗ Pendiente crear cuenta${NC}"
    echo "   Regístrate en: https://dashboard.render.com/register"
fi
echo ""

# Resumen
echo "════════════════════════════════════════════════════════"
echo -e "📊 RESUMEN: ${GREEN}$READY${NC}/$TOTAL requisitos completados"
echo "════════════════════════════════════════════════════════"

if [ $READY -eq $TOTAL ]; then
    echo -e "${GREEN}✅ ¡Todo listo para desplegar!${NC}"
    echo ""
    echo "Próximo paso: ./deploy-flyio-free-tier.sh"
else
    echo -e "${YELLOW}⚠️  Completa los requisitos pendientes antes de continuar${NC}"
    echo ""
    echo "Requisitos faltantes: $((TOTAL-READY))"
fi
echo ""

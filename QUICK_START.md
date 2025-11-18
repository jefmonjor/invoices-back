# ⚡ QUICK START - DEPLOYMENT EN 3 PASOS

## 🎯 TU SITUACIÓN ACTUAL

✅ Código revisado y corregido (11 problemas resueltos)
✅ Base de datos Neon creada: `neondb`
✅ Redis Upstash configurado
✅ Cloudflare R2 con credenciales
✅ Frontend Vercel: `https://invoices-frontend-vert.vercel.app`
⚠️ Falta: Crear bucket R2 y hacer deployment

---

## 🚀 PASO 1: Crear Bucket en R2 (2 min)

```bash
# 1. Ve a: https://dash.cloudflare.com/
# 2. R2 → Create bucket
# 3. Nombre: invoices-documents
# 4. Click "Create bucket"
```

---

## 🚀 PASO 2: Configurar Secrets (3 min)

```bash
cd /home/user/invoices-back

# Crear app en Fly.io
fly apps create invoices-monolith

# Configurar TODOS los secrets automáticamente
./configure-secrets.sh
```

El script:
- ✅ Genera JWT_SECRET automáticamente
- ✅ Configura PostgreSQL (neondb)
- ✅ Configura Redis (Upstash)
- ✅ Configura R2 (Cloudflare)
- ✅ Configura CORS (Vercel)

---

## 🚀 PASO 3: Deploy (5-10 min)

```bash
# Deploy a producción
./deploy.sh
```

El script te mostrará:
- ✅ Validaciones pre-deployment
- ✅ Confirmación antes de deployar
- ✅ Progreso del deployment
- ✅ Health checks
- ✅ URLs de la aplicación

---

## ✅ VERIFICACIÓN

```bash
# Health check
curl https://invoices-monolith.fly.dev/actuator/health

# Login
curl -X POST https://invoices-monolith.fly.dev/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@invoices.com", "password": "admin123"}'
```

---

## 🔀 CREAR PULL REQUEST

```bash
# Ejecutar script para crear PR
./create-pr.sh
```

O directamente:
👉 https://github.com/jefmonjor/invoices-back/compare/master...claude/review-agent-errors-018NYMM9tidter6F6bDnidwW?expand=1

---

## 📊 URLs FINALES

Una vez desplegado:

- **Backend**: https://invoices-monolith.fly.dev
- **Frontend**: https://invoices-frontend-vert.vercel.app
- **Swagger**: https://invoices-monolith.fly.dev/swagger-ui.html
- **Health**: https://invoices-monolith.fly.dev/actuator/health

---

## 🎨 CONFIGURAR FRONTEND EN VERCEL

1. Ve a: https://vercel.com/dashboard
2. Tu proyecto → Settings → Environment Variables
3. Agregar:

```
VITE_API_BASE_URL=https://invoices-monolith.fly.dev
```

4. Redeploy

---

## 📝 RESUMEN DE CAMBIOS

### ✅ Correcciones (11 problemas)
- 3 Críticos (compilación, timestamps, VAT)
- 2 Altos (circuit breaker, config)
- 4 Medios (race conditions, rate limiting)
- 2 Bajos (docs, scripts)

### 📦 Scripts Agregados
- `configure-secrets.sh` - Config automática de secrets
- `deploy.sh` - Deployment interactivo
- `create-pr.sh` - Helper para crear PR

### 📚 Documentación
- `DEPLOYMENT_CHECKLIST.md` - Checklist completo
- `DEPLOYMENT_COMMANDS.md` - Referencia de comandos
- `QUICK_START.md` - Esta guía

---

## 💰 COSTO

**$0/mes** - Todo en free tier ✨

---

## 🆘 AYUDA

Si algo falla:

```bash
# Ver logs
fly logs -a invoices-monolith

# Ver status
fly status -a invoices-monolith

# Ver secrets
fly secrets list -a invoices-monolith
```

Ver troubleshooting completo en: `DEPLOYMENT_COMMANDS.md`

---

## ⏱️ TIEMPO TOTAL

- Crear bucket R2: 2 min
- Configurar secrets: 3 min
- Deploy: 5-10 min
- Verificación: 2 min
- **TOTAL: ~15-20 minutos**

---

## 🎉 ¡LISTO!

Tu aplicación de facturas estará corriendo en producción, 100% gratis.

**¿Preguntas?** Ver documentación completa en los archivos `DEPLOYMENT_*.md`

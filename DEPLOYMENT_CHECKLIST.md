# ✅ CHECKLIST DE DEPLOYMENT - PRODUCCIÓN

## 📋 PRE-REQUISITOS (Ya tienes todo ✅)

- [x] Cuenta en Fly.io
- [x] Cuenta en Neon PostgreSQL
- [x] Cuenta en Upstash Redis
- [x] Cuenta en Cloudflare R2
- [x] Frontend en Vercel
- [x] Credenciales de R2 generadas
- [x] Redis configurado
- [x] PostgreSQL configurado

---

## 🚀 PASOS RÁPIDOS (20 minutos)

### 1️⃣ Crear bucket en Cloudflare R2 (2 min)

```bash
# Ve a: https://dash.cloudflare.com/
# R2 → Create bucket → Nombre: "invoices-documents"
```

- [ ] Bucket `invoices-documents` creado

---

### 2️⃣ Decidir qué base de datos usar (1 min)

**Opción A - RECOMENDADA:** Nueva DB limpia
```
En Neon Console → Create Database → Nombre: "invoices"
```

**Opción B:** Usar `invoicedb` existente (puede tener conflictos)

- [ ] Decidido: usar DB __________

---

### 3️⃣ Crear app en Fly.io (1 min)

```bash
fly apps create invoices-monolith
```

- [ ] App creada en Fly.io

---

### 4️⃣ OPCIÓN A: Script Automático (5 min) 🎯 RECOMENDADO

```bash
cd /home/user/invoices-back

# Ejecutar script (configura TODOS los secrets automáticamente)
./configure-secrets.sh
```

El script te preguntará qué DB usar y configurará todo automáticamente.

- [ ] Secrets configurados con script

**O si prefieres hacerlo manual → OPCIÓN B**

---

### 4️⃣ OPCIÓN B: Configuración Manual (10 min)

Seguir comandos en: `DEPLOYMENT_COMMANDS.md` sección "PASO 3"

- [ ] Secrets configurados manualmente

---

### 5️⃣ Deploy a producción (5-10 min)

```bash
cd /home/user/invoices-back

# Deploy (primera vez tarda ~5-10 min)
fly deploy -c invoices-monolith/fly.toml -a invoices-monolith
```

**Espera a ver:**
```
✓ Instance started successfully
✓ Health checks passing
```

- [ ] Deploy completado sin errores

---

### 6️⃣ Verificar deployment (3 min)

```bash
# Ver logs
fly logs -a invoices-monolith

# Busca estos mensajes ✅:
# - "Flyway migration completed successfully"
# - "HikariPool-1 - Start completed"
# - "Started InvoicesMonolithApplication"
```

- [ ] Logs muestran startup exitoso
- [ ] Sin errores de conexión a DB
- [ ] Sin errores de conexión a Redis
- [ ] Sin errores de conexión a R2

---

### 7️⃣ Test de endpoints (2 min)

```bash
# Health check
curl https://invoices-monolith.fly.dev/actuator/health

# Login
curl -X POST https://invoices-monolith.fly.dev/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@invoices.com", "password": "admin123"}'
```

- [ ] Health check responde UP
- [ ] Login funciona y devuelve token

---

### 8️⃣ Configurar frontend Vercel (2 min)

```bash
# En Vercel Dashboard → Tu proyecto → Settings → Environment Variables
# Agregar:

VITE_API_BASE_URL=https://invoices-monolith.fly.dev

# O si usas Next.js:
NEXT_PUBLIC_API_BASE_URL=https://invoices-monolith.fly.dev
```

Luego redeploy el frontend desde Vercel dashboard.

- [ ] Variable configurada en Vercel
- [ ] Frontend redeployado

---

## 🎉 ¡DEPLOYMENT COMPLETO!

Una vez que todos los items estén marcados ✅:

### URLs Finales:

- **Backend:** https://invoices-monolith.fly.dev
- **Frontend:** https://invoices-frontend-vert.vercel.app
- **Swagger:** https://invoices-monolith.fly.dev/swagger-ui.html
- **Health:** https://invoices-monolith.fly.dev/actuator/health

### Credenciales Admin:

- Email: `admin@invoices.com`
- Password: `admin123`

### Costo Total:

**$0/mes** 🎊

---

## 🐛 Si algo falla...

Ver sección de troubleshooting en: `DEPLOYMENT_COMMANDS.md`

O ver logs en tiempo real:

```bash
fly logs -a invoices-monolith --follow
```

---

## 📚 Recursos:

- **Documentación completa:** `FREE_SERVICES_SETUP.md`
- **Comandos detallados:** `DEPLOYMENT_COMMANDS.md`
- **Script automático:** `configure-secrets.sh`

---

## 🔄 Próximos deployments (más rápidos):

Una vez configurado todo, futuros deploys son simples:

```bash
# 1. Hacer cambios en el código
# 2. Commit y push
git add .
git commit -m "feat: nueva funcionalidad"
git push

# 3. Deploy (tarda ~3 min)
fly deploy -c invoices-monolith/fly.toml -a invoices-monolith
```

¡Eso es todo! 🚀

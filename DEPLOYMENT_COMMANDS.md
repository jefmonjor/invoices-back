# 🚀 COMANDOS DE DEPLOYMENT - PRODUCTION

## ⚠️ ANTES DE EMPEZAR

1. **Generar JWT Secret:**
```bash
openssl rand -base64 32
```
Guarda el resultado, lo necesitarás abajo.

2. **Crear bucket en Cloudflare R2:**
   - Ve a: https://dash.cloudflare.com/
   - R2 → Create bucket
   - Nombre: `invoices-documents`

3. **Decidir qué base de datos usar:**
   - Opción A: Crear nueva DB "invoices" en Neon (recomendado)
   - Opción B: Usar "invoicedb" existente

---

## 📦 PASO 1: Verificar que estés en Fly.io

```bash
# Verificar que tienes Fly CLI instalado
fly version

# Login (si no estás logueado)
fly auth login

# Ver tus apps
fly apps list
```

---

## 📦 PASO 2: Crear App en Fly.io (si no existe)

```bash
# Crear app (solo la primera vez)
fly apps create invoices-monolith

# O si prefieres otro nombre:
# fly apps create mi-invoices-backend
```

---

## 🔐 PASO 3: Configurar Secrets en Fly.io

**IMPORTANTE:** Reemplaza los valores marcados con `<<<` según corresponda.

### 3.1 JWT Security

```bash
# Reemplaza con el resultado de: openssl rand -base64 32
fly secrets set JWT_SECRET="<<< PEGA_AQUI_TU_JWT_SECRET >>>" -a invoices-monolith

fly secrets set JWT_EXPIRATION_MS="3600000" -a invoices-monolith
fly secrets set JWT_ISSUER="invoices-backend-prod" -a invoices-monolith
```

### 3.2 PostgreSQL (Neon)

**Si creaste nueva DB "invoices":**
```bash
fly secrets set SPRING_DATASOURCE_URL="postgresql://neondb_owner:npg_MT7IHNPGYZ9y@ep-proud-breeze-abi4429i-pooler.eu-west-2.aws.neon.tech/invoices?sslmode=require" -a invoices-monolith
```

**Si usas "invoicedb" existente:**
```bash
fly secrets set SPRING_DATASOURCE_URL="postgresql://neondb_owner:npg_MT7IHNPGYZ9y@ep-proud-breeze-abi4429i-pooler.eu-west-2.aws.neon.tech/invoicedb?sslmode=require" -a invoices-monolith
```

**Credenciales DB (comunes para ambas opciones):**
```bash
fly secrets set DB_USERNAME="neondb_owner" -a invoices-monolith
fly secrets set DB_PASSWORD="npg_MT7IHNPGYZ9y" -a invoices-monolith
```

### 3.3 Redis (Upstash)

```bash
fly secrets set REDIS_HOST="subtle-parrot-38179.upstash.io" -a invoices-monolith
fly secrets set REDIS_PORT="6379" -a invoices-monolith
fly secrets set REDIS_PASSWORD="ApUjAAIgcDI37a9MyM6T1LPJbUI4964n8CwccbGkioWuVe2WQwrM6A" -a invoices-monolith
fly secrets set REDIS_SSL="true" -a invoices-monolith
```

### 3.4 Cloudflare R2 (Storage)

```bash
fly secrets set S3_ENDPOINT="https://ac29c1ccf8f12dc453bdec1c87ddcffb.r2.cloudflarestorage.com" -a invoices-monolith
fly secrets set S3_ACCESS_KEY="6534534b1dfc4ae849e1d01f952cd06c" -a invoices-monolith
fly secrets set S3_SECRET_KEY="5bc3d93666a9fec20955fefa01b51c1d85f2b4e044233426b52dbaf7f514f246" -a invoices-monolith
fly secrets set S3_BUCKET_NAME="invoices-documents" -a invoices-monolith
fly secrets set S3_REGION="auto" -a invoices-monolith
fly secrets set S3_PATH_STYLE_ACCESS="true" -a invoices-monolith
```

### 3.5 CORS (Frontend)

```bash
fly secrets set CORS_ALLOWED_ORIGINS="https://invoices-frontend-vert.vercel.app" -a invoices-monolith
```

### 3.6 Verificar Secrets

```bash
# Ver lista de secrets configurados (sin valores)
fly secrets list -a invoices-monolith
```

Deberías ver:
```
NAME                       DIGEST          CREATED AT
CORS_ALLOWED_ORIGINS       xxxxx           just now
DB_PASSWORD                xxxxx           just now
DB_USERNAME                xxxxx           just now
JWT_EXPIRATION_MS          xxxxx           just now
JWT_ISSUER                 xxxxx           just now
JWT_SECRET                 xxxxx           just now
REDIS_HOST                 xxxxx           just now
REDIS_PASSWORD             xxxxx           just now
REDIS_PORT                 xxxxx           just now
REDIS_SSL                  xxxxx           just now
S3_ACCESS_KEY              xxxxx           just now
S3_BUCKET_NAME             xxxxx           just now
S3_ENDPOINT                xxxxx           just now
S3_PATH_STYLE_ACCESS       xxxxx           just now
S3_REGION                  xxxxx           just now
S3_SECRET_KEY              xxxxx           just now
SPRING_DATASOURCE_URL      xxxxx           just now
```

---

## 🚀 PASO 4: Deploy a Fly.io

```bash
# Asegúrate de estar en el directorio correcto
cd /home/user/invoices-back

# Deploy (tomará 5-10 minutos la primera vez)
fly deploy -c invoices-monolith/fly.toml -a invoices-monolith
```

**Durante el deploy verás:**
```
==> Building image
==> Creating release
==> Monitoring deployment
...
✓ Instance started successfully
```

---

## ✅ PASO 5: Verificar Deployment

### 5.1 Ver logs en tiempo real

```bash
fly logs -a invoices-monolith
```

**Busca estos mensajes positivos:**
```
✓ Flyway migration completed successfully
✓ HikariPool-1 - Start completed
✓ Started InvoicesMonolithApplication
✓ Tomcat started on port 8080
```

### 5.2 Verificar status

```bash
fly status -a invoices-monolith
```

**Esperado:**
```
Instances
ID       PROCESS VERSION REGION  STATE   CHECKS  CREATED
xxxxx    app     v1      ams     started 1 total just now
```

### 5.3 Test de Health Check

```bash
# Obtener URL de tu app
fly info -a invoices-monolith

# Test health endpoint (reemplaza con tu URL real)
curl https://invoices-monolith.fly.dev/actuator/health
```

**Esperado:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### 5.4 Test de Login

```bash
# Test de autenticación
curl -X POST https://invoices-monolith.fly.dev/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@invoices.com",
    "password": "admin123"
  }'
```

**Esperado:**
```json
{
  "token": "eyJhbGci...",
  "type": "Bearer",
  "expiresIn": 3600000
}
```

---

## 🔧 PASO 6: Configurar Frontend en Vercel

1. Ve a tu proyecto en Vercel: https://vercel.com/dashboard
2. Settings → Environment Variables
3. Agregar:

```bash
VITE_API_BASE_URL=https://invoices-monolith.fly.dev
```

O si usas Next.js:

```bash
NEXT_PUBLIC_API_BASE_URL=https://invoices-monolith.fly.dev
```

4. Redeploy el frontend para aplicar cambios

---

## 🐛 TROUBLESHOOTING

### Error: "Failed to connect to database"

**Verificar:**
```bash
# Ver logs filtrados
fly logs -a invoices-monolith | grep -i "database\|postgres\|flyway"

# Verificar que el secret esté correcto
fly secrets list -a invoices-monolith | grep DATASOURCE
```

**Solución común:**
- Verificar que la DB existe en Neon
- Verificar que el connection string incluya `?sslmode=require`

---

### Error: "Redis connection timeout"

**Verificar:**
```bash
# Ver logs
fly logs -a invoices-monolith | grep -i redis

# Verificar secrets
fly secrets list -a invoices-monolith | grep REDIS
```

**Solución:**
- Verificar que REDIS_SSL="true" esté configurado
- Verificar que Upstash Redis esté activo

---

### Error: "Failed to connect to R2"

**Verificar:**
```bash
# Ver logs
fly logs -a invoices-monolith | grep -i "minio\|s3\|storage"

# Verificar secrets
fly secrets list -a invoices-monolith | grep S3
```

**Solución:**
- Verificar que el bucket "invoices-documents" existe en Cloudflare
- Verificar que las credenciales sean correctas

---

### Error: "Application crashed - out of memory"

**Solución:** Escalar a más memoria

```bash
# Escalar a 1GB (sigue siendo gratis)
fly scale memory 1024 -a invoices-monolith

# Verificar
fly status -a invoices-monolith
```

---

### Ver logs completos

```bash
# Todos los logs
fly logs -a invoices-monolith

# Solo errores
fly logs -a invoices-monolith | grep -i "error\|exception\|failed"

# Logs en tiempo real
fly logs -a invoices-monolith --follow
```

---

## 📊 URLs FINALES

Una vez desplegado:

- **Backend API:** https://invoices-monolith.fly.dev
- **Frontend:** https://invoices-frontend-vert.vercel.app
- **Swagger UI:** https://invoices-monolith.fly.dev/swagger-ui.html
- **API Docs:** https://invoices-monolith.fly.dev/api-docs
- **Health Check:** https://invoices-monolith.fly.dev/actuator/health

---

## 🎯 ENDPOINTS PRINCIPALES

```bash
# Auth
POST /api/auth/login
POST /api/auth/register

# Users
GET  /api/users
POST /api/users
GET  /api/users/{id}
PUT  /api/users/{id}

# Invoices
GET  /api/invoices
POST /api/invoices
GET  /api/invoices/{id}
PUT  /api/invoices/{id}
DELETE /api/invoices/{id}

# Documents
GET  /api/documents
POST /api/documents
GET  /api/documents/{id}

# Audit Logs
GET  /api/audit-logs
GET  /api/audit-logs/{id}
```

---

## 🔒 CREDENCIALES ADMIN

**⚠️ IMPORTANTE:** Cambiar después del primer login

- Email: `admin@invoices.com`
- Password: `admin123`

---

## 🎉 ¡DEPLOYMENT COMPLETADO!

Tu aplicación de facturas está ahora desplegada en producción completamente gratis:

✅ Backend en Fly.io
✅ Frontend en Vercel
✅ PostgreSQL en Neon
✅ Redis en Upstash
✅ Storage en Cloudflare R2

**Costo total:** $0/mes 🎊

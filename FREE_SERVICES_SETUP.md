# 🆓 Servicios Gratuitos para Invoices Monolith

Esta guía te muestra cómo configurar **TODOS** los servicios necesarios para ejecutar el monolito de facturas de forma **completamente gratuita**.

---

## 📋 Resumen de Servicios Gratuitos

| Servicio | Proveedor | Free Tier | Uso |
|----------|-----------|-----------|-----|
| **Backend** | Fly.io | 3 VMs (256MB RAM) | Monolito Spring Boot |
| **Base de Datos** | Neon PostgreSQL | 1 DB (500MB) | Base de datos única consolidada |
| **Object Storage** | Cloudflare R2 | 10 GB | Almacenamiento de PDFs |
| **Event Streaming** | Upstash Redis | 10K comandos/día | Sistema de eventos/auditoría |
| **Frontend** | Vercel | Unlimited | UI React (opcional) |

**✅ Todo 100% gratis para uso básico**

---

## 🎯 Arquitectura Final

```
Frontend (Vercel)
        ↓
Invoices Monolith (Fly.io)
   ┌────┼────┬─────┬─────┬─────┐
   │    │    │     │     │     │
  User Invoice Doc Trace Auth
(módulos internos del monolito)
   │    │    │     │     │
   └────┴────┴─────┴─────┘
        ↓      ↓     ↓
    PostgreSQL  R2   Redis
    (Neon)    (CF) (Upstash)
    (1 DB)
```

**✅ Ventajas del Monolito:**
- Una sola aplicación para desplegar
- Una sola base de datos
- Redis Streams en lugar de Kafka (más simple)
- Menor complejidad operacional

---

## 💾 Parte 1: Neon PostgreSQL (Base de Datos)

### ¿Qué es Neon?

Base de datos PostgreSQL serverless con **500 MB gratis** - perfecto para desarrollo y uso básico.

### Paso 1: Crear cuenta en Neon

1. Visita: https://neon.tech/
2. Sign up con GitHub o Email
3. No necesitas tarjeta de crédito

### Paso 2: Crear base de datos

```bash
# 1. En Neon Console: https://console.neon.tech/

# 2. Click "Create Project"
#    - Project name: invoices-monolith
#    - Region: AWS eu-west-2 (Londres)
#    - PostgreSQL version: 16

# 3. Crear la base de datos principal
#    - Database name: invoices
#    - Owner: neondb_owner
```

### Paso 3: Obtener connection string

```bash
# 1. En el proyecto creado, ir a "Dashboard"

# 2. Copiar el "Connection String":
postgresql://neondb_owner:xxxxx@ep-xxxx-xxx.eu-west-2.aws.neon.tech/invoices?sslmode=require
```

### Paso 4: Esquema de la base de datos

El monolito usa **UNA SOLA base de datos** con todas las tablas:

```sql
invoices (database)
├── users              # Usuarios y autenticación
├── user_roles         # Roles de usuarios
├── companies          # Empresas emisoras
├── clients            # Clientes receptores
├── invoices           # Facturas
├── invoice_items      # Ítems de facturas
├── documents          # Documentos PDF
└── audit_logs         # Logs de auditoría
```

**Las migraciones Flyway las crean automáticamente al arrancar.**

### Paso 5: Variables de entorno para Neon

Agregar a tu `.env`:

```bash
# Neon PostgreSQL
SPRING_DATASOURCE_URL=postgresql://neondb_owner:password@ep-xxx.eu-west-2.aws.neon.tech/invoices?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=your_password_here
```

**📖 Guía detallada:** Ver [NEON_DATABASE_SETUP.md](./NEON_DATABASE_SETUP.md)

---

## 📨 Parte 2: Upstash Redis (Event Streaming)

### ¿Qué es Upstash Redis?

Redis serverless con **10,000 comandos gratis al día** - perfecto para eventos asíncronos con Redis Streams.

### Paso 1: Crear cuenta en Upstash

1. Visita: https://console.upstash.com/
2. Sign up con GitHub o Email
3. No necesitas tarjeta de crédito

### Paso 2: Crear base de datos Redis

```bash
# 1. En Upstash Console, ir a "Redis"

# 2. Click "Create Database"

# 3. Configurar:
#    - Name: invoices-events
#    - Type: Regional (free tier)
#    - Region: eu-west-1 (Irlanda)
#    - TLS: Enabled (recommended)

# 4. Click "Create"
```

### Paso 3: Obtener credenciales

```bash
# 1. En el database creado, ir a "Details"

# 2. Copiar:
#    - Endpoint: enhanced-lemur-12345.upstash.io
#    - Port: 6379
#    - Password: AxxxYxxxZxxxQxxx...
```

### Paso 4: Redis Streams para eventos

El monolito usa **Redis Streams** para comunicación asíncrona entre módulos:

**Streams creados automáticamente:**
- `invoice-events` - Eventos de facturas (creación, actualización, pago, eliminación)
- `invoice-events-dlq` - Dead Letter Queue para eventos fallidos

**Consumer Groups:**
- `trace-group` - El módulo Trace consume eventos para auditoría

### Paso 5: Variables de entorno para Redis

Agregar a tu `.env`:

```bash
# Upstash Redis
REDIS_HOST=enhanced-lemur-12345.upstash.io
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
REDIS_SSL=true

# Redis Streams (opcionales, tienen valores por defecto)
REDIS_STREAM_INVOICE_EVENTS=invoice-events
REDIS_STREAM_INVOICE_DLQ=invoice-events-dlq
REDIS_CONSUMER_GROUP=trace-group
REDIS_CONSUMER_NAME=trace-consumer
```

---

## 🚀 Parte 3: Cloudflare R2 (Almacenamiento de PDFs)

### ¿Qué es Cloudflare R2?

Alternativa a Amazon S3 con **10 GB gratis permanentemente** y **sin cargos por ancho de banda**.

### Paso 1: Crear cuenta en Cloudflare

1. Visita: https://dash.cloudflare.com/sign-up
2. Crea una cuenta gratuita
3. No necesitas agregar tarjeta de crédito para el free tier

### Paso 2: Crear bucket de R2

```bash
# 1. Ir al dashboard de Cloudflare
# https://dash.cloudflare.com/

# 2. En el menú lateral, seleccionar "R2"

# 3. Click en "Create bucket"
#    - Nombre: invoices-documents
#    - Región: Automatic (recommended)
#    - Click "Create bucket"
```

### Paso 3: Crear API Token

```bash
# 1. En R2 dashboard, click "Manage R2 API Tokens"

# 2. Click "Create API Token"

# 3. Configurar:
#    - Token name: invoices-backend
#    - Permissions: Object Read & Write
#    - TTL: Forever
#    - Buckets: invoices-documents

# 4. Click "Create API Token"

# 5. GUARDAR ESTOS VALORES (solo se muestran una vez):
#    - Access Key ID: XXXXXXXXXXXXXXXXXXXX
#    - Secret Access Key: YYYYYYYYYYYYYYYYYYYY
#    - Endpoint: https://ACCOUNT_ID.r2.cloudflarestorage.com
```

### Paso 4: Variables de entorno para R2

Agregar a tu `.env`:

```bash
# Cloudflare R2 Storage
S3_ENDPOINT=https://your-account-id.r2.cloudflarestorage.com
S3_ACCESS_KEY=your-access-key-id
S3_SECRET_KEY=your-secret-access-key
S3_BUCKET_NAME=invoices-documents
S3_REGION=auto
```

**💡 Alternativa local:** Para desarrollo usa MinIO (ver `docker-compose.yml`)

---

## ☁️ Parte 4: Fly.io (Backend Monolito)

### ¿Qué es Fly.io?

Plataforma de deployment con **3 VMs gratis (256MB RAM c/u)** - perfecto para un monolito Java.

### Paso 1: Instalar Fly CLI

```bash
# Mac/Linux
curl -L https://fly.io/install.sh | sh

# Agregar al PATH
export PATH="/home/user/.fly/bin:$PATH"

# Verificar instalación
fly version
```

### Paso 2: Autenticarse

```bash
fly auth login
```

### Paso 3: Crear aplicación en Fly.io

```bash
cd /home/user/invoices-back/invoices-monolith

# Crear aplicación (primera vez)
fly apps create invoices-monolith

# O usar el nombre que prefieras
fly apps create my-invoices-backend
```

### Paso 4: Configurar secrets

```bash
# JWT Security
fly secrets set JWT_SECRET="your-super-secret-jwt-key-min-32-chars-base64" -a invoices-monolith

# Database
fly secrets set SPRING_DATASOURCE_URL="postgresql://neondb_owner:xxx@ep-xxx.eu-west-2.aws.neon.tech/invoices?sslmode=require" -a invoices-monolith
fly secrets set DB_USERNAME="neondb_owner" -a invoices-monolith
fly secrets set DB_PASSWORD="your_neon_password" -a invoices-monolith

# Redis
fly secrets set REDIS_HOST="enhanced-lemur-12345.upstash.io" -a invoices-monolith
fly secrets set REDIS_PORT="6379" -a invoices-monolith
fly secrets set REDIS_PASSWORD="your_redis_password" -a invoices-monolith
fly secrets set REDIS_SSL="true" -a invoices-monolith

# Cloudflare R2
fly secrets set S3_ENDPOINT="https://your-account-id.r2.cloudflarestorage.com" -a invoices-monolith
fly secrets set S3_ACCESS_KEY="your_r2_access_key" -a invoices-monolith
fly secrets set S3_SECRET_KEY="your_r2_secret_key" -a invoices-monolith
fly secrets set S3_BUCKET_NAME="invoices-documents" -a invoices-monolith
fly secrets set S3_REGION="auto" -a invoices-monolith

# CORS (ajustar según tu frontend)
fly secrets set CORS_ALLOWED_ORIGINS="https://invoices-frontend.vercel.app,https://www.myapp.com" -a invoices-monolith
```

### Paso 5: Desplegar el monolito

```bash
cd /home/user/invoices-back

# Compilar y desplegar
fly deploy -c invoices-monolith/fly.toml
```

**Tiempo estimado:** 3-5 minutos

### Paso 6: Verificar deployment

```bash
# Ver logs
fly logs -a invoices-monolith

# Ver status
fly status -a invoices-monolith

# Abrir en navegador
fly open -a invoices-monolith
```

---

## 🎨 Parte 5: Vercel (Frontend - Opcional)

### Paso 1: Desplegar frontend

Si tienes un frontend React/Next.js:

1. Conecta tu repositorio a Vercel
2. Vercel detectará automáticamente el framework
3. Deploy automático en cada push

### Paso 2: Variables de entorno en Vercel

1. Ir a tu proyecto en Vercel
2. Settings → Environment Variables
3. Agregar:

```bash
VITE_API_BASE_URL=https://invoices-monolith.fly.dev
```

**⚠️ IMPORTANTE:** Ajustar según el nombre de tu app en Fly.io

---

## ✅ Verificación Post-Deployment

### 1. Verificar health check

```bash
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

### 2. Test de login

```bash
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

### 3. Verificar conexión a PostgreSQL

```bash
fly logs -a invoices-monolith | grep -i "flyway\|database"
```

Buscar: `Flyway migration completed successfully` o `HikariPool-1 - Start completed`

### 4. Verificar Redis Streams

```bash
fly logs -a invoices-monolith | grep -i redis
```

Buscar: `Connected to Redis at enhanced-lemur-12345.upstash.io:6379`

### 5. Verificar Cloudflare R2

```bash
fly logs -a invoices-monolith | grep -i minio
```

Buscar: `MinIO client initialized successfully` o similar

---

## 📊 Resumen de Costos (Gratis)

```
✅ Fly.io:
   - Invoices Monolith: 1 VM (256MB RAM)
   Total: $0/mes (dentro del free tier)

✅ Neon PostgreSQL:
   - 1 database (500 MB)
   Total: $0/mes

✅ Cloudflare R2:
   - 10 GB storage
   - Sin cargo por bandwidth
   Total: $0/mes

✅ Upstash Redis:
   - 10,000 comandos/día
   - Redis Streams incluidos
   Total: $0/mes

✅ Vercel (opcional):
   - Frontend deployment
   Total: $0/mes

════════════════════════════
TOTAL: $0/mes ✨
════════════════════════════
```

**💡 Comparación con microservicios:**
- **Antes:** 5 servicios + 4 databases + Kafka = complejidad
- **Ahora:** 1 monolito + 1 database + Redis = simple y gratis

---

## 🐛 Troubleshooting

### Error: "Failed to connect to Neon PostgreSQL"

**Verificar:**

```bash
# 1. Verificar que el secret esté configurado
fly secrets list -a invoices-monolith | grep DATASOURCE

# 2. Verificar que la connection string incluya ?sslmode=require
```

**Solución:** Asegurarse de incluir `?sslmode=require` al final de la URL

---

### Error: "Redis connection timeout"

**Verificar:**

```bash
# 1. Verificar que Upstash Redis esté activo
# Ir a https://console.upstash.com/redis

# 2. Verificar credenciales
fly secrets list -a invoices-monolith | grep REDIS
```

**Solución:** Verificar que `REDIS_SSL=true` esté configurado

---

### Error: "Failed to connect to R2"

**Verificar:**

```bash
# 1. Verificar que las credenciales estén configuradas
fly secrets list -a invoices-monolith | grep S3

# 2. Verificar que el bucket existe en Cloudflare
```

**Solución:** Regenerar API token en Cloudflare R2 si es necesario

---

### Error: "Application crashed - out of memory"

**Problema:** VM de 256MB puede ser insuficiente para Spring Boot

**Solución:** Escalar a VM más grande (sigue siendo gratis hasta 3 VMs)

```bash
# Escalar a 512MB
fly scale memory 512 -a invoices-monolith
```

---

### Error: "Database migration failed"

**Verificar:**

```bash
# Ver logs de Flyway
fly logs -a invoices-monolith | grep Flyway
```

**Soluciones comunes:**
- Verificar que Neon database esté accessible
- Verificar permisos del usuario de base de datos
- Verificar que las migraciones en `src/main/resources/db/migration/` sean válidas

---

## 🔒 Seguridad

### Mejores prácticas

1. **Nunca commitear secrets al repositorio**
   ```bash
   # Asegurarse de que .env está en .gitignore
   echo ".env" >> .gitignore
   echo ".env.production" >> .gitignore
   ```

2. **Rotar JWT_SECRET en producción**
   ```bash
   # Generar nuevo secret
   openssl rand -base64 32

   # Actualizar en Fly.io
   fly secrets set JWT_SECRET="new-secret-here" -a invoices-monolith
   ```

3. **Usar HTTPS siempre**
   - Fly.io proporciona HTTPS automáticamente
   - Cloudflare R2 usa HTTPS por defecto
   - Upstash Redis TLS habilitado

4. **Configurar CORS correctamente**
   ```bash
   # Solo permitir orígenes conocidos
   fly secrets set CORS_ALLOWED_ORIGINS="https://mi-frontend.vercel.app" -a invoices-monolith
   ```

---

## 📚 Recursos Adicionales

### Documentación oficial

- **Fly.io:** https://fly.io/docs/
- **Neon:** https://neon.tech/docs/
- **Cloudflare R2:** https://developers.cloudflare.com/r2/
- **Upstash Redis:** https://upstash.com/docs/redis
- **Vercel:** https://vercel.com/docs

### Dashboards

- **Fly.io:** https://fly.io/dashboard
- **Neon:** https://console.neon.tech/
- **Cloudflare R2:** https://dash.cloudflare.com/
- **Upstash:** https://console.upstash.com/
- **Vercel:** https://vercel.com/dashboard

### Guías relacionadas

- [Variables de Entorno](./ENVIRONMENT_VARIABLES.md)
- [Configuración de Neon Database](./NEON_DATABASE_SETUP.md)
- [Guía de Testing](./TESTING_GUIDE.md)
- [README Principal](./README.md)

---

## 🎯 Checklist Final

Antes de considerar el deployment completo, verifica:

- [ ] Neon PostgreSQL database creada (`invoices`)
- [ ] Upstash Redis database creado
- [ ] Cloudflare R2 bucket creado (`invoices-documents`)
- [ ] Fly.io CLI instalado y autenticado
- [ ] Fly.io app creada (`invoices-monolith`)
- [ ] Todos los secrets configurados en Fly.io
- [ ] Monolito desplegado con `fly deploy`
- [ ] Health check respondiendo OK
- [ ] Login test funcionando
- [ ] Logs sin errores críticos
- [ ] Variables de entorno en Vercel configuradas (si aplica)

---

## 🚀 ¡Todo Listo!

Con esta configuración tienes un monolito completo de gestión de facturas ejecutándose **100% gratis** en la nube.

**URLs finales:**

```
Backend:   https://invoices-monolith.fly.dev
Frontend:  https://invoices-frontend.vercel.app (si aplica)
```

**Credenciales admin (configurar al primer arranque):**
- Email: `admin@invoices.com`
- Password: `admin123`

**Endpoints principales:**
- Login: `POST /api/auth/login`
- Users: `GET /api/users`
- Invoices: `GET /api/invoices`
- Documents: `GET /api/documents`
- Audit Logs: `GET /api/audit-logs`

¡Disfruta tu sistema de facturas! 🎉

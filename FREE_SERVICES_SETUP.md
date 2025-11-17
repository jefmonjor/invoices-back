# 🆓 Servicios Gratuitos para Sistema Mínimo (2 Usuarios)

Esta guía te muestra cómo configurar **TODOS** los servicios necesarios para ejecutar el sistema de facturas de forma **completamente gratuita**.

---

## 📋 Resumen de Servicios Gratuitos

| Servicio | Proveedor | Free Tier | Uso |
|----------|-----------|-----------|-----|
| **Backend** | Fly.io | 3 VMs (consumo) | Gateway, User, Invoice |
| **Base de Datos** | Neon PostgreSQL | 4 DB (500MB c/u) | userdb, invoicedb, documentdb, tracedb |
| **Object Storage** | Cloudflare R2 | 10 GB | Almacenamiento de PDFs |
| **Message Queue** | Upstash Kafka | 10K msg/día | Sistema de eventos/auditoría |
| **Frontend** | Vercel | Unlimited | UI React |

**✅ Todo 100% gratis para 2 usuarios básicos**

---

## 🎯 Arquitectura Final

```
Frontend (Vercel)
        ↓
API Gateway (Fly.io)
        ↓
    ┌───┴────┬──────────┬──────────┐
    ↓        ↓          ↓          ↓
  User    Invoice   Document    Trace
Service  Service    Service    Service
(Fly.io) (Fly.io)   (Fly.io)   (Fly.io)
    ↓        ↓          ↓          ↓
         PostgreSQL (Neon)        Kafka       R2
    (4 databases separadas)    (Upstash) (Cloudflare)
```

---

## 🚀 Parte 1: Configurar Cloudflare R2 (Almacenamiento de PDFs)

### ¿Qué es Cloudflare R2?

Alternativa a Amazon S3/MinIO con **10 GB gratis permanentemente** y **sin cargos por ancho de banda**.

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

Agregar a tu `.env.production`:

```bash
# Cloudflare R2 Storage
R2_ACCESS_KEY_ID=your-access-key-id
R2_SECRET_ACCESS_KEY=your-secret-access-key
R2_ENDPOINT=https://your-account-id.r2.cloudflarestorage.com
R2_BUCKET_NAME=invoices-documents
R2_REGION=auto
```

---

## 📨 Parte 2: Configurar Upstash Kafka (Sistema de Eventos)

### ¿Qué es Upstash Kafka?

Kafka serverless con **10,000 mensajes gratis al día** - perfecto para auditoría y eventos de 2 usuarios.

### Paso 1: Crear cuenta en Upstash

1. Visita: https://console.upstash.com/
2. Sign up con GitHub o Email
3. No necesitas tarjeta de crédito

### Paso 2: Crear cluster de Kafka

```bash
# 1. En Upstash Console, ir a "Kafka"

# 2. Click "Create Cluster"

# 3. Configurar:
#    - Name: invoices-events
#    - Type: Single Replica (free tier)
#    - Region: eu-west-1 (Irlanda - más cerca de Amsterdam)

# 4. Click "Create"
```

### Paso 3: Crear topics

```bash
# En el cluster creado, ir a "Topics"

# Crear 3 topics:

1. invoice-events
   - Partitions: 1
   - Retention: 7 days

2. user-events
   - Partitions: 1
   - Retention: 7 days

3. audit-trail
   - Partitions: 1
   - Retention: 30 days
```

### Paso 4: Obtener credenciales

```bash
# 1. En el cluster, ir a "Details"

# 2. Copiar:
#    - Endpoint: https://ruling-lemur-12345-eu1-kafka.upstash.io:9092
#    - Username: cnVsaW5nLWxlbXVyL...
#    - Password: YourPasswordHere...

# También en "REST API" tab:
#    - REST Endpoint: https://ruling-lemur-12345-eu1-rest-kafka.upstash.io
#    - REST Username: same as above
#    - REST Password: same as above
```

### Paso 5: Variables de entorno para Kafka

Agregar a tu `.env.production`:

```bash
# Upstash Kafka
KAFKA_BOOTSTRAP_SERVERS=ruling-lemur-12345-eu1-kafka.upstash.io:9092
KAFKA_USERNAME=your-upstash-username
KAFKA_PASSWORD=your-upstash-password
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=SCRAM-SHA-256

# Topics
KAFKA_TOPIC_INVOICE_EVENTS=invoice-events
KAFKA_TOPIC_USER_EVENTS=user-events
KAFKA_TOPIC_AUDIT_TRAIL=audit-trail
```

---

## 💾 Parte 3: Neon PostgreSQL (Ya configurado)

Si aún no tienes las 4 bases de datos, sigue estos pasos:

### Paso 1: Crear las 4 bases de datos

1. Visita: https://console.neon.tech/
2. Crea 4 databases en tu proyecto:
   - `userdb`
   - `invoicedb`
   - `documentdb`
   - `tracedb`

### Paso 2: Copiar connection strings

Agregar a `.env.production`:

```bash
# Neon PostgreSQL Databases
USER_DB_URL=postgresql://neondb_owner:password@ep-xxx.eu-west-2.aws.neon.tech/userdb?sslmode=require
INVOICE_DB_URL=postgresql://neondb_owner:password@ep-xxx.eu-west-2.aws.neon.tech/invoicedb?sslmode=require
DOCUMENT_DB_URL=postgresql://neondb_owner:password@ep-xxx.eu-west-2.aws.neon.tech/documentdb?sslmode=require
TRACE_DB_URL=postgresql://neondb_owner:password@ep-xxx.eu-west-2.aws.neon.tech/tracedb?sslmode=require
```

---

## ☁️ Parte 4: Fly.io (Backend)

### Paso 1: Instalar Fly CLI

```bash
# Mac/Linux
curl -L https://fly.io/install.sh | sh

# Agregar al PATH
export PATH="/home/user/.fly/bin:$PATH"
```

### Paso 2: Autenticarse

```bash
fly auth login
```

### Paso 3: Ya está listo

El deployment se hace con el script `deploy-all-services.sh` (ver Parte 6)

---

## 🎨 Parte 5: Vercel (Frontend)

### Paso 1: Variables de entorno en Vercel

1. Ir a tu proyecto: https://vercel.com/jeffersons-projects-fe447893/invoices-frontend
2. Settings → Environment Variables
3. Agregar:

```bash
VITE_API_BASE_URL=https://invoices-backend.fly.dev
```

**⚠️ IMPORTANTE:** NO incluir `/api` al final - el gateway ya maneja este path

---

## 🚀 Parte 6: Desplegar Todo

### Paso 1: Crear archivo .env.production

```bash
cd /home/user/invoices-back
cp .env.production.example .env.production
```

### Paso 2: Editar .env.production con TODOS tus valores

```bash
# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
JWT_ISSUER=invoices-backend
JWT_EXPIRATION_MS=3600000

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://invoices-frontend-vert.vercel.app,https://*.vercel.app

# Neon PostgreSQL Databases
USER_DB_URL=postgresql://neondb_owner:xxx@ep-xxx.eu-west-2.aws.neon.tech/userdb?sslmode=require
INVOICE_DB_URL=postgresql://neondb_owner:xxx@ep-xxx.eu-west-2.aws.neon.tech/invoicedb?sslmode=require
DOCUMENT_DB_URL=postgresql://neondb_owner:xxx@ep-xxx.eu-west-2.aws.neon.tech/documentdb?sslmode=require
TRACE_DB_URL=postgresql://neondb_owner:xxx@ep-xxx.eu-west-2.aws.neon.tech/tracedb?sslmode=require

# Cloudflare R2 Storage
R2_ACCESS_KEY_ID=your-r2-access-key
R2_SECRET_ACCESS_KEY=your-r2-secret-key
R2_ENDPOINT=https://your-account-id.r2.cloudflarestorage.com
R2_BUCKET_NAME=invoices-documents
R2_REGION=auto

# Upstash Kafka
KAFKA_BOOTSTRAP_SERVERS=ruling-lemur-12345-eu1-kafka.upstash.io:9092
KAFKA_USERNAME=your-kafka-username
KAFKA_PASSWORD=your-kafka-password
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=SCRAM-SHA-256
KAFKA_TOPIC_INVOICE_EVENTS=invoice-events
KAFKA_TOPIC_USER_EVENTS=user-events
KAFKA_TOPIC_AUDIT_TRAIL=audit-trail
```

### Paso 3: Desplegar servicios esenciales

```bash
# Esto desplegará Gateway (ya hecho), User, Invoice
./deploy-all-services.sh
```

**Tiempo estimado:** 8-10 minutos

### Paso 4 (OPCIONAL): Desplegar Document y Trace

Si necesitas almacenamiento permanente de PDFs y auditoría completa:

```bash
# Editar deploy-all-services.sh y descomentar:
# deploy_service "document-service" "invoices-document-service" "8083" "DOCUMENT_DB_URL"
# deploy_service "trace-service" "invoices-trace-service" "8084" "TRACE_DB_URL"

# Luego ejecutar
./deploy-all-services.sh
```

---

## ✅ Verificación Post-Deployment

### 1. Verificar health checks

```bash
# Gateway
curl https://invoices-backend.fly.dev/actuator/health

# User Service
curl https://invoices-user-service.fly.dev/actuator/health

# Invoice Service
curl https://invoices-invoice-service.fly.dev/actuator/health
```

**Esperado:** `{"status":"UP"}`

### 2. Test de login

```bash
curl -X POST https://invoices-backend.fly.dev/api/auth/login \
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

### 3. Verificar R2 storage

```bash
# Ver logs del document-service
fly logs -a invoices-document-service | grep R2
```

Buscar: `"Connected to Cloudflare R2 bucket: invoices-documents"`

### 4. Verificar Kafka

```bash
# Ver logs del trace-service
fly logs -a invoices-trace-service | grep Kafka
```

Buscar: `"Connected to Upstash Kafka cluster"`

---

## 📊 Resumen de Costos (Gratis)

```
✅ Fly.io:
   - Gateway Service: 1 VM (consumo mínimo)
   - User Service: 1 VM (consumo mínimo)
   - Invoice Service: 1 VM (consumo mínimo)
   - Document Service: 1 VM (opcional)
   - Trace Service: 1 VM (opcional)
   Total: ~$0/mes para 2 usuarios básicos

✅ Neon PostgreSQL:
   - 4 databases × 500 MB = 2 GB total
   Total: $0/mes (dentro del free tier)

✅ Cloudflare R2:
   - 10 GB storage
   - Sin cargo por bandwidth
   Total: $0/mes

✅ Upstash Kafka:
   - 10,000 mensajes/día
   - 2 usuarios = ~100 mensajes/día
   Total: $0/mes

✅ Vercel:
   - Frontend deployment
   Total: $0/mes

════════════════════════════
TOTAL: $0/mes ✨
════════════════════════════
```

---

## 🐛 Troubleshooting

### Error: "Failed to connect to R2"

**Verificar:**

```bash
# 1. Verificar que las credenciales estén configuradas
fly secrets list -a invoices-document-service | grep R2

# 2. Probar conexión desde local
aws s3 ls --endpoint-url $R2_ENDPOINT
```

**Solución:** Regenerar API token en Cloudflare R2

---

### Error: "Kafka connection timeout"

**Verificar:**

```bash
# 1. Verificar que Upstash cluster esté activo
# Ir a https://console.upstash.com/kafka

# 2. Verificar credenciales
fly secrets list -a invoices-trace-service | grep KAFKA
```

**Solución:** Verificar que el endpoint incluya el puerto `:9092`

---

### Error: "Database migration failed"

**Verificar:**

```bash
# Ver logs del servicio
fly logs -a invoices-user-service | grep Flyway
```

**Solución común:** Verificar que la connection string incluya `?sslmode=require`

---

## 📚 Recursos Adicionales

### Documentación oficial

- Fly.io: https://fly.io/docs/
- Neon: https://neon.tech/docs/
- Cloudflare R2: https://developers.cloudflare.com/r2/
- Upstash Kafka: https://upstash.com/docs/kafka
- Vercel: https://vercel.com/docs

### Dashboards

- Fly.io: https://fly.io/dashboard
- Neon: https://console.neon.tech/
- Cloudflare R2: https://dash.cloudflare.com/
- Upstash: https://console.upstash.com/
- Vercel: https://vercel.com/dashboard

---

## 🎯 Checklist Final

Antes de hacer el commit y PR, verifica:

- [ ] Cloudflare R2 bucket creado y API token guardado
- [ ] Upstash Kafka cluster creado con 3 topics
- [ ] Neon PostgreSQL con 4 databases
- [ ] Archivo `.env.production` completo con TODAS las variables
- [ ] Gateway, User, Invoice desplegados en Fly.io
- [ ] Document Service desplegado (opcional)
- [ ] Trace Service desplegado (opcional)
- [ ] Vercel environment variable `VITE_API_BASE_URL` configurada
- [ ] Health checks respondiendo OK
- [ ] Login test funcionando
- [ ] Logs sin errores

---

## 🚀 ¡Todo Listo!

Con esta configuración tienes un sistema completo de microservicios ejecutándose **100% gratis** para 2 usuarios básicos.

**URLs finales:**

```
Frontend:  https://invoices-frontend-vert.vercel.app
Backend:   https://invoices-backend.fly.dev
User:      https://invoices-user-service.fly.dev
Invoice:   https://invoices-invoice-service.fly.dev
Document:  https://invoices-document-service.fly.dev (opcional)
Trace:     https://invoices-trace-service.fly.dev (opcional)
```

**Credenciales admin:**
- Email: `admin@invoices.com`
- Password: `admin123`

¡Disfruta tu sistema de facturas! 🎉

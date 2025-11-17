# 🆓 Arquitectura Free-Tier para Invoices Backend

Esta guía detalla cómo implementar todo el sistema usando **servicios completamente gratuitos**, ideal para proyectos personales, MVPs o desarrollo.

## 📋 Tabla de Contenidos

1. [Arquitectura General](#-arquitectura-general)
2. [Stack Tecnológico Gratuito](#-stack-tecnológico-gratuito)
3. [Diagrama de Arquitectura](#-diagrama-de-arquitectura)
4. [Configuración por Servicio](#-configuración-por-servicio)
5. [Deployment](#-deployment)
6. [Limitaciones y Consideraciones](#-limitaciones-y-consideraciones)

---

## 🏗️ Arquitectura General

La arquitectura ha sido adaptada para usar servicios 100% gratuitos manteniendo:
- ✅ Clean Architecture
- ✅ Microservicios independientes
- ✅ Event-driven con Redis Streams
- ✅ Almacenamiento S3-compatible
- ✅ Base de datos serverless

### Cambios Principales

| Componente Anterior | Componente Gratuito | Motivo |
|-------------------|-------------------|---------|
| PostgreSQL local | **Neon** | Serverless, gratis hasta 3GB |
| Kafka + Zookeeper | **Redis Streams** (Upstash) | 10k commands/día gratis |
| MinIO local | **Cloudflare R2** | 10GB storage gratis |
| Eureka/Config Server | **Eliminados** | No necesarios en cloud |
| Docker Compose local | **Render/Fly.io** | Deploy directo en cloud |

---

## 🛠️ Stack Tecnológico Gratuito

### Frontend
- **Vercel** (Gratis)
  - Deployments ilimitados
  - 100GB bandwidth/mes
  - SSL automático
  - CDN global

### Backend (Elegir una opción)

#### Opción 1: Render.com (Recomendado)
- **Free Tier:** 750 horas/mes por servicio
- **Servicios:** 5 microservicios (Gateway, User, Invoice, Document, Trace)
- **Pros:** Fácil setup, auto-deploy desde Git
- **Cons:** Se duerme tras 15 min inactividad

#### Opción 2: Fly.io
- **Free Tier:** 3 apps con 256MB RAM c/u
- **Servicios:** Combinar microservicios o elegir 3 principales
- **Pros:** Siempre activo, mejor rendimiento
- **Cons:** Límite de 3 apps

### Base de Datos
- **Neon PostgreSQL** (Serverless)
  - 3GB storage gratis
  - Múltiples databases (userdb, invoicedb, documentdb, tracedb)
  - Auto-scale a 0 cuando no está en uso
  - Backups automáticos

### Event Streaming
- **Upstash Redis** (Serverless)
  - 10,000 commands/día gratis
  - Redis Streams para eventos
  - SSL/TLS incluido
  - Alternativa: Redis Cloud (30MB, comandos ilimitados)

### Object Storage
- **Cloudflare R2** (S3-compatible)
  - 10GB storage gratis
  - 1M uploads/mes
  - Descargas ilimitadas
  - API compatible con AWS S3

---

## 📊 Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    FRONTEND (Vercel)                         │
│              React / Next.js / Angular                       │
│           https://tu-app.vercel.app                         │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTPS/REST
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              GATEWAY SERVICE (Render/Fly.io)                 │
│                  :8080 - API Gateway                         │
│              - JWT Validation                                │
│              - CORS Configuration                            │
│              - Route to services                             │
└──────┬──────────────┬──────────────┬──────────────┬─────────┘
       │              │              │              │
       ▼              ▼              ▼              ▼
┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
│   USER    │  │  INVOICE  │  │ DOCUMENT  │  │  TRACE    │
│  SERVICE  │  │  SERVICE  │  │  SERVICE  │  │  SERVICE  │
│   :8082   │  │   :8081   │  │   :8083   │  │   :8084   │
│           │  │           │  │           │  │           │
│ - Auth    │  │ - CRUD    │  │ - Upload  │  │ - Audit   │
│ - JWT     │  │ - PDF Gen │  │ - S3 Ops  │  │ - Events  │
│ - Roles   │  │ - Events  │  │           │  │ - DLQ     │
└─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
      │              │              │              │
      ▼              ▼              │              ▼
┌─────────────────────────┐        │      ┌─────────────┐
│   NEON POSTGRESQL       │        │      │   UPSTASH   │
│   (Serverless)          │        │      │   REDIS     │
│                         │        │      │             │
│ - userdb                │        │      │ Streams:    │
│ - invoicedb             │        │      │ - invoice-  │
│ - documentdb            │        │      │   events    │
│ - tracedb               │        │      │ - dlq       │
└─────────────────────────┘        │      └─────────────┘
                                   ▼
                           ┌───────────────┐
                           │ CLOUDFLARE R2 │
                           │ (S3-compat)   │
                           │               │
                           │ Bucket:       │
                           │ invoices-     │
                           │ documents     │
                           └───────────────┘
```

---

## ⚙️ Configuración por Servicio

### 1. Neon PostgreSQL

**Setup:**
1. Ir a https://console.neon.tech/
2. Crear proyecto: "invoices-backend"
3. Crear 4 databases:
   - `userdb`
   - `invoicedb`
   - `documentdb`
   - `tracedb`

**Variables de entorno:**
```bash
# User Service
SPRING_DATASOURCE_URL=postgresql://user:pass@ep-xxx.neon.tech/userdb?sslmode=require

# Invoice Service
SPRING_DATASOURCE_URL=postgresql://user:pass@ep-xxx.neon.tech/invoicedb?sslmode=require

# Document Service
SPRING_DATASOURCE_URL=postgresql://user:pass@ep-xxx.neon.tech/documentdb?sslmode=require

# Trace Service
SPRING_DATASOURCE_URL=postgresql://user:pass@ep-xxx.neon.tech/tracedb?sslmode=require
```

### 2. Upstash Redis

**Setup:**
1. Ir a https://console.upstash.com/redis
2. Crear database: "invoices-events"
3. Región: eu-west-1 (Ireland) o us-east-1
4. Copiar credenciales

**Variables de entorno:**
```bash
REDIS_HOST=eu-west-1-xxx.upstash.io
REDIS_PORT=6379
REDIS_PASSWORD=tu_password_upstash
REDIS_SSL=true
REDIS_STREAM_INVOICE_EVENTS=invoice-events
REDIS_STREAM_INVOICE_DLQ=invoice-events-dlq
REDIS_CONSUMER_GROUP=trace-group
```

**Alternativa: Redis Cloud**
- URL: https://redis.com/try-free/
- 30MB gratis
- Comandos ilimitados

### 3. Cloudflare R2

**Setup:**
1. Ir a https://dash.cloudflare.com/
2. R2 → Create bucket: "invoices-documents"
3. Manage R2 API Tokens → Create API Token
4. Permisos: Read & Write

**Variables de entorno:**
```bash
S3_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
S3_ACCESS_KEY=tu_r2_access_key
S3_SECRET_KEY=tu_r2_secret_key
S3_BUCKET_NAME=invoices-documents
S3_REGION=auto
S3_PATH_STYLE_ACCESS=false  # false para R2
```

### 4. Configuración Común

**JWT:**
```bash
# Generar con: openssl rand -base64 64 | tr -d '\n'
JWT_SECRET=tu_jwt_secret_min_64_chars
JWT_EXPIRATION_MS=3600000
JWT_ISSUER=invoices-backend
```

**Eureka (Deshabilitado):**
```bash
EUREKA_CLIENT_ENABLED=false
```

**CORS:**
```bash
CORS_ALLOWED_ORIGINS=https://tu-app.vercel.app
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS,PATCH
CORS_MAX_AGE=3600
```

---

## 🚀 Deployment

### Opción 1: Deploy en Render.com

1. **Crear cuenta** en https://render.com

2. **Conectar repositorio** GitHub

3. **Crear servicios** (uno por microservicio):

   **Gateway Service:**
   ```yaml
   Name: invoices-gateway
   Environment: Docker
   Build Context: ./gateway-service
   Dockerfile: ./gateway-service/Dockerfile
   Port: 8080
   Instance Type: Free
   Auto-Deploy: Yes
   ```

   **Variables de entorno (todas las del .env.production.example)**

4. **Repetir** para:
   - `user-service` (puerto 8082)
   - `invoice-service` (puerto 8081)
   - `document-service` (puerto 8083)
   - `trace-service` (puerto 8084)

5. **URLs generadas:**
   ```
   https://invoices-gateway.onrender.com
   https://invoices-user.onrender.com
   https://invoices-invoice.onrender.com
   https://invoices-document.onrender.com
   https://invoices-trace.onrender.com
   ```

6. **Configurar Gateway** para apuntar a los servicios:
   - En `gateway-service/application.yml` cambiar `lb://` por URLs directas

### Opción 2: Deploy en Fly.io

Ver archivo `deploy-flyio.sh` para automatización.

1. **Instalar Fly CLI:**
   ```bash
   curl -L https://fly.io/install.sh | sh
   ```

2. **Login:**
   ```bash
   fly auth login
   ```

3. **Deploy cada servicio:**
   ```bash
   # Gateway
   cd gateway-service
   fly launch --name invoices-gateway --region ams
   fly deploy

   # User Service
   cd ../user-service
   fly launch --name invoices-user --region ams
   fly deploy

   # ... etc
   ```

4. **Configurar secrets:**
   ```bash
   fly secrets set \
     SPRING_DATASOURCE_URL=postgresql://... \
     REDIS_HOST=... \
     REDIS_PASSWORD=... \
     --app invoices-gateway
   ```

---

## ⚠️ Limitaciones y Consideraciones

### Render.com Free Tier
- ✅ **Ventajas:**
  - 750 horas/mes por servicio (suficiente para 1 servicio 24/7)
  - 5 servicios = suficiente para todos los microservicios
  - Auto-deploy desde Git
  - SSL gratuito

- ⚠️ **Limitaciones:**
  - Se duerme tras 15 min de inactividad
  - Primera request tarda 30-60s en despertar
  - 512MB RAM por servicio

### Fly.io Free Tier
- ✅ **Ventajas:**
  - Siempre activo (no se duerme)
  - Mejor rendimiento
  - 3 apps con 256MB RAM c/u

- ⚠️ **Limitaciones:**
  - Solo 3 apps gratis
  - Necesitas combinar microservicios o elegir los principales

### Neon PostgreSQL
- ✅ **Ventajas:**
  - 3GB storage
  - Serverless (scale to zero)
  - Múltiples databases

- ⚠️ **Limitaciones:**
  - Límite de conexiones concurrentes (ajustar HikariCP)
  - Pausa tras inactividad (puede causar latencia en primera query)

### Upstash Redis
- ✅ **Ventajas:**
  - 10,000 commands/día
  - Redis Streams nativo
  - Serverless

- ⚠️ **Limitaciones:**
  - 10k commands/día (monitorear uso)
  - Para más, considerar Redis Cloud (30MB, ilimitado)

### Cloudflare R2
- ✅ **Ventajas:**
  - 10GB storage
  - 1M uploads/mes
  - Descargas ilimitadas
  - Compatible S3

- ⚠️ **Limitaciones:**
  - Límite de uploads (1M/mes)
  - Sin versionado en free tier

---

## 📈 Escalamiento Futuro

Cuando necesites escalar:

1. **Backend:**
   - Render: Upgrade a $7/mes por servicio (always-on)
   - Fly.io: $1.94/mes por 256MB adicional
   - Railway: $5/mes crédito

2. **Base de Datos:**
   - Neon: $19/mes (10GB, más conexiones)
   - Supabase: Similar a Neon
   - AWS RDS: Desde $15/mes

3. **Redis:**
   - Upstash: $0.20/100k commands
   - Redis Cloud: Desde $5/mes
   - Upstash Kafka: $10/mes (si necesitas volver a Kafka)

4. **Storage:**
   - Cloudflare R2: $0.015/GB después de 10GB
   - AWS S3: Desde $0.023/GB

---

## 🎯 Próximos Pasos

1. ✅ Configurar Neon PostgreSQL
2. ✅ Configurar Upstash Redis
3. ✅ Configurar Cloudflare R2
4. ✅ Deploy en Render/Fly.io
5. ✅ Configurar Frontend en Vercel
6. ✅ Testing end-to-end
7. ✅ Monitoreo (Render Dashboard, Upstash Metrics)

---

## 📚 Recursos

- [Neon Docs](https://neon.tech/docs)
- [Upstash Redis Docs](https://docs.upstash.com/redis)
- [Cloudflare R2 Docs](https://developers.cloudflare.com/r2/)
- [Render Docs](https://render.com/docs)
- [Fly.io Docs](https://fly.io/docs/)

---

## 🤝 Soporte

Si encuentras problemas:
1. Revisar logs en dashboard de cada servicio
2. Verificar variables de entorno
3. Comprobar límites de free tier
4. Abrir issue en GitHub

---

**¡Feliz deployment! 🚀**

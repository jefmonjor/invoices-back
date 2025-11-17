# 🚀 Guía de Deployment Híbrido: Fly.io + Render

Esta guía te ayudará a desplegar la arquitectura **completamente gratuita** combinando:
- **Fly.io** → 3 servicios principales (Gateway, User, Invoice)
- **Render** → 2 servicios secundarios (Document, Trace)

## 📋 Requisitos Previos

Asegúrate de tener configurados:
- ✅ Cuenta en [Neon](https://console.neon.tech/) con 4 databases creadas
- ✅ Cuenta en [Upstash Redis](https://console.upstash.com/redis)
- ✅ Cuenta en [Cloudflare R2](https://dash.cloudflare.com/)
- ✅ Cuenta en [Fly.io](https://fly.io/app/sign-up)
- ✅ Cuenta en [Render](https://dashboard.render.com/register)

## 🏗️ Arquitectura de Deployment

```
┌─────────────────────────────────────────────────────────┐
│                  FRONTEND (Vercel)                      │
│            https://tu-app.vercel.app                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │   GATEWAY (Fly.io)         │
        │   invoices-backend.fly.dev │
        └────────┬───────────────────┘
                 │
    ┌────────────┼────────────┬────────────┐
    │            │            │            │
    ▼            ▼            ▼            ▼
┌─────────┐ ┌─────────┐ ┌──────────┐ ┌──────────┐
│  USER   │ │ INVOICE │ │ DOCUMENT │ │  TRACE   │
│ (Fly.io)│ │(Fly.io) │ │ (Render) │ │ (Render) │
└────┬────┘ └────┬────┘ └────┬─────┘ └────┬─────┘
     │           │           │            │
     └───────────┴───────────┴────────────┘
                     │
         ┌───────────┼───────────┐
         ▼           ▼           ▼
    ┌────────┐  ┌────────┐  ┌────────┐
    │  NEON  │  │UPSTASH │  │   R2   │
    │  (DB)  │  │ REDIS  │  │(Storage)│
    └────────┘  └────────┘  └────────┘
```

---

## 📦 Parte 1: Deployment en Fly.io (Servicios Principales)

### Paso 1: Instalar Fly CLI

```bash
# Linux/Mac
curl -L https://fly.io/install.sh | sh

# Windows (PowerShell)
iwr https://fly.io/install.ps1 -useb | iex
```

### Paso 2: Autenticarse

```bash
fly auth login
```

### Paso 3: Desplegar Servicios

**Opción A: Usar el script automático (Recomendado)**

```bash
./deploy-flyio-free-tier.sh
```

**Opción B: Deployment manual**

#### 1. User Service

```bash
cd user-service
fly launch --name invoices-user-service --region ams --ha=false --now
```

#### 2. Invoice Service

```bash
cd ../invoice-service
fly launch --name invoices-invoice-service --region ams --ha=false --now
```

#### 3. Gateway Service

```bash
cd ../gateway-service
fly launch --name invoices-backend --region ams --ha=false --now
```

### Paso 4: Configurar Secrets en Fly.io

Para cada servicio, configura las variables de entorno sensibles:

#### Gateway Service

```bash
fly secrets set -a invoices-backend \
  JWT_SECRET='tu_jwt_secret_generado_con_openssl' \
  CORS_ALLOWED_ORIGINS='https://tu-app.vercel.app'
```

#### User Service

```bash
fly secrets set -a invoices-user-service \
  SPRING_DATASOURCE_URL='postgresql://user:pass@ep-xxx.neon.tech/userdb?sslmode=require' \
  JWT_SECRET='mismo_jwt_secret_que_gateway'
```

#### Invoice Service

```bash
fly secrets set -a invoices-invoice-service \
  SPRING_DATASOURCE_URL='postgresql://user:pass@ep-xxx.neon.tech/invoicedb?sslmode=require' \
  JWT_SECRET='mismo_jwt_secret_que_gateway' \
  REDIS_HOST='subtle-parrot-38179.upstash.io' \
  REDIS_PASSWORD='tu_upstash_redis_password' \
  REDIS_SSL='true'
```

### Paso 5: Verificar Deployment

```bash
# Ver status de las apps
fly apps list | grep invoices

# Ver logs en tiempo real
fly logs -a invoices-backend
fly logs -a invoices-user-service
fly logs -a invoices-invoice-service

# Verificar salud de los servicios
curl https://invoices-backend.fly.dev/actuator/health
curl https://invoices-user-service.fly.dev/actuator/health
curl https://invoices-invoice-service.fly.dev/actuator/health
```

---

## 📦 Parte 2: Deployment en Render (Servicios Secundarios)

### Paso 1: Conectar Repositorio

1. Ve a https://dashboard.render.com/
2. Click en "New +" → "Blueprint"
3. Conecta tu repositorio de GitHub
4. Selecciona el archivo `render.yaml`

### Paso 2: Configurar Variables de Entorno

Render detectará automáticamente las variables del `render.yaml`. Configura las marcadas como `sync: false`:

#### Document Service

| Variable | Valor |
|----------|-------|
| `SPRING_DATASOURCE_URL` | `postgresql://user:pass@ep-xxx.neon.tech/documentdb?sslmode=require` |
| `JWT_SECRET` | Mismo que en Fly.io |
| `S3_ENDPOINT` | `https://YOUR_ACCOUNT_ID.r2.cloudflarestorage.com` |
| `S3_ACCESS_KEY` | Tu R2 Access Key |
| `S3_SECRET_KEY` | Tu R2 Secret Key |

#### Trace Service

| Variable | Valor |
|----------|-------|
| `SPRING_DATASOURCE_URL` | `postgresql://user:pass@ep-xxx.neon.tech/tracedb?sslmode=require` |
| `JWT_SECRET` | Mismo que en Fly.io |
| `REDIS_HOST` | `subtle-parrot-38179.upstash.io` |
| `REDIS_PASSWORD` | Tu Upstash Redis password |

### Paso 3: Deploy

Click en "Apply" y Render comenzará a desplegar ambos servicios automáticamente.

### Paso 4: Verificar URLs

Render te dará URLs como:
- `https://invoices-document-service.onrender.com`
- `https://invoices-trace-service.onrender.com`

---

## 🔗 Parte 3: Configurar Gateway para Conectar Todos los Servicios

El Gateway debe conocer las URLs de todos los servicios. Actualiza las variables de entorno:

```bash
fly secrets set -a invoices-backend \
  USER_SERVICE_URL='https://invoices-user-service.fly.dev' \
  INVOICE_SERVICE_URL='https://invoices-invoice-service.fly.dev' \
  DOCUMENT_SERVICE_URL='https://invoices-document-service.onrender.com' \
  TRACE_SERVICE_URL='https://invoices-trace-service.onrender.com'
```

### Actualizar application.yml del Gateway

Si usas Eureka deshabilitado, actualiza las rutas directamente:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: https://invoices-user-service.fly.dev
          predicates:
            - Path=/api/users/**,/api/auth/**

        - id: invoice-service
          uri: https://invoices-invoice-service.fly.dev
          predicates:
            - Path=/api/invoices/**

        - id: document-service
          uri: https://invoices-document-service.onrender.com
          predicates:
            - Path=/api/documents/**

        - id: trace-service
          uri: https://invoices-trace-service.onrender.com
          predicates:
            - Path=/api/traces/**
```

---

## 🎯 Parte 4: Testing End-to-End

### 1. Health Checks

```bash
# Fly.io services
curl https://invoices-backend.fly.dev/actuator/health
curl https://invoices-user-service.fly.dev/actuator/health
curl https://invoices-invoice-service.fly.dev/actuator/health

# Render services (pueden tardar 30-60s si están dormidos)
curl https://invoices-document-service.onrender.com/actuator/health
curl https://invoices-trace-service.onrender.com/actuator/health
```

### 2. Test de Autenticación

```bash
# Register
curl -X POST https://invoices-backend.fly.dev/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!"
  }'

# Login
curl -X POST https://invoices-backend.fly.dev/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!"
  }'
```

### 3. Test de Invoice Creation

```bash
# Crear factura (usar JWT del login anterior)
curl -X POST https://invoices-backend.fly.dev/api/invoices \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "Test Client",
    "amount": 100.00
  }'
```

---

## 📊 Monitoreo y Logs

### Fly.io

```bash
# Ver logs en tiempo real
fly logs -a invoices-backend

# Ver métricas
fly dashboard -a invoices-backend
```

### Render

1. Ve a https://dashboard.render.com/
2. Selecciona tu servicio
3. Click en "Logs" o "Metrics"

### Upstash Redis

1. Ve a https://console.upstash.com/redis
2. Selecciona tu database
3. Click en "Metrics" para ver uso de comandos

### Neon PostgreSQL

1. Ve a https://console.neon.tech/
2. Selecciona tu proyecto
3. Click en "Monitoring" para ver queries y uso

---

## 💰 Costos y Límites

| Servicio | Plan | Límites | Costo |
|----------|------|---------|-------|
| Fly.io | Free | 3 apps × 256MB | $0 |
| Render | Free | 750h/mes × 2 servicios | $0 |
| Neon | Free | 3GB storage | $0 |
| Upstash Redis | Free | 10k commands/día | $0 |
| Cloudflare R2 | Free | 10GB storage | $0 |
| **TOTAL** | | | **$0/mes** |

### Límites Importantes

- **Fly.io**: 3 apps gratis con 256MB RAM c/u
- **Render**: Servicios se duermen tras 15 min de inactividad
- **Neon**: Límite de conexiones concurrentes (configurar HikariCP)
- **Upstash**: 10,000 comandos/día (monitorear uso)

---

## 🔧 Troubleshooting

### Servicio no responde en Render

**Problema**: Primera request tarda mucho
**Solución**: Es normal, el servicio estaba dormido. Tarda 30-60s en despertar.

### Error de conexión a Neon

**Problema**: `SSL connection required`
**Solución**: Asegúrate de incluir `?sslmode=require` en la URL de conexión

### Redis connection timeout

**Problema**: No se puede conectar a Upstash Redis
**Solución**: Verifica que `REDIS_SSL=true` y el puerto es `6379`

### Gateway no encuentra los servicios

**Problema**: 404 en rutas
**Solución**: Verifica las URLs de los servicios en las variables de entorno del Gateway

---

## 📚 Recursos

- [Fly.io Docs](https://fly.io/docs/)
- [Render Docs](https://render.com/docs)
- [Neon Docs](https://neon.tech/docs)
- [Upstash Redis Docs](https://docs.upstash.com/redis)
- [Cloudflare R2 Docs](https://developers.cloudflare.com/r2/)

---

## 🎉 ¡Listo!

Tu arquitectura completa está desplegada de forma **100% gratuita**:

- ✅ Frontend en Vercel
- ✅ Backend en Fly.io + Render
- ✅ Database en Neon
- ✅ Redis en Upstash
- ✅ Storage en Cloudflare R2

**URLs Finales:**
- Gateway: `https://invoices-backend.fly.dev`
- Frontend: `https://tu-app.vercel.app`

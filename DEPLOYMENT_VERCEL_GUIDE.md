# 🚀 Guía de Deployment: Backend para Frontend en Vercel

Esta guía te explica cómo desplegar el backend de tu sistema de facturas para que funcione con el frontend en Vercel usando **servicios 100% GRATUITOS**.

---

## 📋 Índice

- [Resumen de Arquitectura](#-resumen-de-arquitectura)
- [Opciones de Deployment Gratuito](#-opciones-de-deployment-gratuito)
- [Opción 1: Fly.io + Neon (RECOMENDADO)](#-opción-1-flyio--neon-recomendado)
- [Opción 2: Render + Neon](#-opción-2-render--neon)
- [Configuración de CORS](#-configuración-de-cors)
- [Variables de Entorno](#-variables-de-entorno)
- [Conectar con Frontend en Vercel](#-conectar-con-frontend-en-vercel)
- [Troubleshooting](#-troubleshooting)

---

## 🏗️ Resumen de Arquitectura

Tu backend es un **sistema de microservicios** con:

```
Frontend (Vercel)
        ↓ HTTPS
API Gateway (Fly.io/Render) ← ¡PUNTO DE ENTRADA ÚNICO!
        ↓
    ┌───┴───┬──────────┬──────────┐
    ↓       ↓          ↓          ↓
User    Invoice    Document   Trace
Service Service    Service    Service
    ↓       ↓          ↓          ↓
      PostgreSQL (Neon)
```

**Importante:** El frontend SOLO debe apuntar al **Gateway Service** (puerto 8080).

---

## 💰 Opciones de Deployment Gratuito

### Comparación Rápida

| Servicio | Backend | Base de Datos | Limitaciones | Recomendado |
|----------|---------|---------------|--------------|-------------|
| **Fly.io + Neon** | 3 VMs gratis | 500MB PostgreSQL | Ideal para microservicios | ✅ **SÍ** |
| **Render + Neon** | 750h/mes por servicio | 500MB PostgreSQL | Se duerme después de 15 min | ⚠️ OK |
| **Railway** | $5 crédito inicial | PostgreSQL incluido | Se agota rápido | ❌ No |
| **Heroku** | Ya no es gratis | - | - | ❌ No |

---

## ⭐ Opción 1: Fly.io + Neon (RECOMENDADO)

Esta es la **mejor opción gratuita** para microservicios Spring Boot.

### ¿Por qué Fly.io?

✅ **3 VMs compartidas gratis** (perfecto para nuestros servicios)
✅ **3GB de almacenamiento persistente**
✅ **Soporta Docker** y microservicios
✅ **No se duerme** tan agresivamente como Render
✅ **HTTPS automático**
✅ **CLI fácil de usar**

### Requisitos Previos

```bash
# 1. Instalar Fly.io CLI
curl -L https://fly.io/install.sh | sh

# 2. Crear cuenta gratuita
fly auth signup

# 3. Login
fly auth login
```

### Paso 1: Crear Base de Datos en Neon (GRATIS)

**Neon** es PostgreSQL serverless con **500MB gratis** (ideal para desarrollo).

1. **Ir a [Neon.tech](https://neon.tech)** y crear cuenta
2. **Crear nuevo proyecto**: "invoices-backend"
3. **Crear 4 bases de datos** (una por servicio):
   ```sql
   -- Desde el SQL Editor de Neon:
   CREATE DATABASE userdb;
   CREATE DATABASE invoicedb;
   CREATE DATABASE documentdb;
   CREATE DATABASE tracedb;
   ```

4. **Obtener connection string**:
   ```
   Settings → Connection String

   Ejemplo:
   postgres://user:password@ep-cool-name-123456.us-east-2.aws.neon.tech/userdb?sslmode=require
   ```

5. **Guardar las 4 URLs** (una por cada base de datos):
   ```bash
   USER_DB_URL=postgres://user:pass@host.neon.tech/userdb?sslmode=require
   INVOICE_DB_URL=postgres://user:pass@host.neon.tech/invoicedb?sslmode=require
   DOCUMENT_DB_URL=postgres://user:pass@host.neon.tech/documentdb?sslmode=require
   TRACE_DB_URL=postgres://user:pass@host.neon.tech/tracedb?sslmode=require
   ```

### Paso 2: Simplificar Arquitectura para Deployment

Para deployment inicial gratuito, **simplificaremos temporalmente**:

**Arquitectura Completa (Local):**
- ✅ Eureka Server
- ✅ Config Server
- ✅ 4 Business Services
- ✅ PostgreSQL local
- ✅ Kafka + Zookeeper
- ✅ MinIO

**Arquitectura Simplificada (Producción Gratis):**
- ❌ Sin Eureka (usamos URLs directas)
- ❌ Sin Config Server (usamos variables de entorno)
- ✅ Gateway + User Service + Invoice Service (esenciales)
- ✅ PostgreSQL en Neon
- ❌ Sin Kafka/MinIO (temporalmente)

### Paso 3: Desplegar Gateway Service en Fly.io

```bash
# 1. Ir al directorio del gateway
cd gateway-service

# 2. Inicializar app de Fly.io
fly launch --name invoices-backend --no-deploy

# 3. Configurar variables de entorno
fly secrets set \
  SPRING_PROFILES_ACTIVE=prod \
  JWT_SECRET=$(openssl rand -base64 32) \
  CORS_ALLOWED_ORIGINS=https://tu-app.vercel.app \
  DATABASE_URL=$USER_DB_URL \
  EUREKA_CLIENT_ENABLED=false

# 4. Desplegar
fly deploy

# 5. Verificar que funciona
fly open
# Deberías ver el health endpoint: /actuator/health
```

### Paso 4: Desplegar User Service

```bash
cd ../user-service

fly launch --name invoices-user-service --no-deploy

fly secrets set \
  SPRING_PROFILES_ACTIVE=prod \
  DATABASE_URL=$USER_DB_URL \
  JWT_SECRET=$(openssl rand -base64 32) \
  EUREKA_CLIENT_ENABLED=false

fly deploy
```

### Paso 5: Desplegar Invoice Service

```bash
cd ../invoice-service

fly launch --name invoices-invoice-service --no-deploy

fly secrets set \
  SPRING_PROFILES_ACTIVE=prod \
  DATABASE_URL=$INVOICE_DB_URL \
  JWT_SECRET=$(openssl rand -base64 32) \
  EUREKA_CLIENT_ENABLED=false \
  KAFKA_ENABLED=false

fly deploy
```

### Paso 6: Obtener URL del Gateway

```bash
# Ver tu app deployada
fly status

# URL será algo como:
# https://invoices-backend.fly.dev
```

**Guarda esta URL** - la necesitarás para configurar el frontend en Vercel.

---

## 🔄 Opción 2: Render + Neon

Si prefieres **Render** (interfaz web más simple), aquí está el proceso:

### Paso 1: Crear Base de Datos en Neon

*(Igual que en Opción 1 - Paso 1)*

### Paso 2: Desplegar en Render

1. **Ir a [Render.com](https://render.com)** y crear cuenta

2. **New → Web Service**

3. **Conectar tu repositorio** `jefmonjor/invoices-back`

4. **Configurar el servicio**:
   ```
   Name: invoices-gateway
   Region: Ohio (US East)
   Branch: main
   Root Directory: gateway-service
   Environment: Docker
   Dockerfile Path: ./Dockerfile
   Docker Command: (dejar vacío)
   Instance Type: Free
   ```

5. **Variables de entorno** (Add Environment Variable):
   ```bash
   SPRING_PROFILES_ACTIVE=prod
   PORT=8080

   # CORS - ¡IMPORTANTE!
   CORS_ALLOWED_ORIGINS=https://tu-app.vercel.app

   # JWT
   JWT_SECRET=<genera-uno-seguro-32-chars>
   JWT_ISSUER=invoices-backend
   JWT_EXPIRATION_MS=3600000

   # Database (Neon)
   DATABASE_URL=<tu-neon-connection-string>

   # Simplificado (sin Eureka)
   EUREKA_CLIENT_ENABLED=false
   ```

6. **Create Web Service**

7. **Esperar 5-10 minutos** para el primer deploy

8. **Obtener URL**:
   ```
   https://invoices-gateway.onrender.com
   ```

### ⚠️ Limitaciones de Render Free Tier

- **Se duerme después de 15 min** de inactividad
- **Primera request después de dormir tarda 30-60 segundos** en despertar
- 750 horas gratis por mes

**Solución:** Usar un servicio de "ping" gratuito como [UptimeRobot](https://uptimerobot.com) para hacer ping cada 5 minutos y evitar que se duerma.

---

## 🌐 Configuración de CORS

El CORS ya está configurado en el Gateway para aceptar dominios de Vercel.

**Archivo configurado**: `gateway-service/src/main/resources/application.yml`

```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173,https://*.vercel.app}
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
  allowed-headers: "*"
  allow-credentials: true
```

### Configurar CORS en producción

**Fly.io:**
```bash
fly secrets set CORS_ALLOWED_ORIGINS=https://tu-app.vercel.app,https://tu-dominio-custom.com
```

**Render:**
```
Environment Variables → CORS_ALLOWED_ORIGINS
Value: https://tu-app.vercel.app
```

**⚠️ Importante:** Usar tu URL exacta de Vercel. Por ejemplo:
```
https://invoices-frontend-abc123xyz.vercel.app
```

---

## 🔐 Variables de Entorno

### Variables Obligatorias

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring | `prod` |
| `CORS_ALLOWED_ORIGINS` | URL del frontend en Vercel | `https://tu-app.vercel.app` |
| `JWT_SECRET` | Secreto para firmar JWT (min 32 chars) | `tu-secreto-super-largo-y-seguro-aqui` |
| `DATABASE_URL` | Connection string de Neon | `postgres://user:pass@host.neon.tech/db` |

### Variables Opcionales

| Variable | Descripción | Default |
|----------|-------------|---------|
| `JWT_EXPIRATION_MS` | Tiempo de expiración del token (ms) | `3600000` (1 hora) |
| `JWT_ISSUER` | Emisor del token | `invoices-backend` |
| `LOG_LEVEL_ROOT` | Nivel de logging | `INFO` |
| `LOG_LEVEL_APP` | Nivel de logging de la app | `INFO` |

### Generar JWT_SECRET seguro

```bash
# Opción 1: OpenSSL
openssl rand -base64 32

# Opción 2: Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

# Opción 3: Online
# https://generate-secret.vercel.app/32
```

---

## 🔗 Conectar con Frontend en Vercel

### Paso 1: Obtener URL del Backend

Después de desplegar, obtendrás una URL como:
- Fly.io: `https://invoices-backend.fly.dev`
- Render: `https://invoices-gateway.onrender.com`

### Paso 2: Configurar Variable de Entorno en Vercel

1. **Ir a tu proyecto en Vercel**
2. **Settings → Environment Variables**
3. **Editar o agregar**:
   ```bash
   VITE_API_BASE_URL=https://invoices-backend.fly.dev/api

   # O si usas React/Next.js:
   NEXT_PUBLIC_API_URL=https://invoices-backend.fly.dev/api
   ```

4. **Redeploy el frontend**:
   ```bash
   # Desde Vercel Dashboard:
   Deployments → Latest → Redeploy
   ```

### Paso 3: Verificar Conexión

**Test desde DevTools del frontend:**

```javascript
// Abrir consola en tu app de Vercel
fetch('https://invoices-backend.fly.dev/actuator/health')
  .then(res => res.json())
  .then(data => console.log('✅ Backend conectado:', data))
  .catch(err => console.error('❌ Error:', err))

// Test de login
fetch('https://invoices-backend.fly.dev/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
})
  .then(res => res.json())
  .then(data => console.log('✅ Login exitoso:', data))
  .catch(err => console.error('❌ Error:', err))
```

---

## 🐛 Troubleshooting

### Error: "CORS policy: No 'Access-Control-Allow-Origin'"

**Causa:** Backend no permite el origen de Vercel

**Solución:**
```bash
# Fly.io
fly secrets set CORS_ALLOWED_ORIGINS=https://tu-app-exacta.vercel.app

# Render
# Ir a Environment Variables → CORS_ALLOWED_ORIGINS
# Valor: https://tu-app-exacta.vercel.app

# Redeploy
fly deploy  # Fly.io
# O usar el botón "Manual Deploy" en Render
```

### Error: "Failed to fetch" o "Network Error"

**Causa 1:** URL incorrecta

**Solución:**
```bash
# Verificar que el backend esté corriendo
curl https://invoices-backend.fly.dev/actuator/health

# Debería responder:
# {"status":"UP"}
```

**Causa 2:** Backend dormido (Render)

**Solución:**
- Primera request después de 15 min de inactividad tarda 30-60 segundos
- Usa UptimeRobot para hacer ping cada 5 minutos

### Error: "401 Unauthorized"

**Causa:** JWT inválido o no se envía

**Solución:**
```javascript
// Asegúrate de enviar el token en el header Authorization
const token = localStorage.getItem('token');

fetch('https://invoices-backend.fly.dev/api/invoices', {
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
})
```

### Error: "Connection to database failed"

**Causa:** URL de Neon incorrecta

**Solución:**
```bash
# Verificar connection string de Neon
# Debe incluir ?sslmode=require al final

# Ejemplo correcto:
postgres://user:pass@ep-name-123.us-east-2.aws.neon.tech/userdb?sslmode=require

# Actualizar en Fly.io:
fly secrets set DATABASE_URL=<nueva-url-correcta>

# Redeploy
fly deploy
```

### Build de Docker falla

**Causa:** Dependencias de Maven no se descargan

**Solución:**
```bash
# Verificar que el Dockerfile incluya:
FROM maven:3.9-eclipse-temurin-21 AS build
COPY pom.xml .
RUN mvn dependency:go-offline  # ← Importante

# Si sigue fallando, hacer build local:
cd gateway-service
docker build -t invoices-gateway .
```

---

## 📊 Verificación Final - Checklist

### Backend (Fly.io/Render)

- [ ] ✅ Backend desplegado y accesible
- [ ] ✅ HTTPS funcionando automáticamente
- [ ] ✅ `/actuator/health` responde `{"status":"UP"}`
- [ ] ✅ CORS configurado con URL de Vercel
- [ ] ✅ JWT_SECRET configurado (>32 chars)
- [ ] ✅ DATABASE_URL apunta a Neon
- [ ] ✅ Variables de entorno configuradas

### Base de Datos (Neon)

- [ ] ✅ Cuenta creada en Neon.tech
- [ ] ✅ 4 bases de datos creadas (userdb, invoicedb, documentdb, tracedb)
- [ ] ✅ Connection strings copiadas
- [ ] ✅ SSL mode habilitado (`?sslmode=require`)

### Frontend (Vercel)

- [ ] ✅ `VITE_API_BASE_URL` configurada con URL del backend
- [ ] ✅ Variables de entorno configuradas
- [ ] ✅ Frontend redeployado
- [ ] ✅ Login funciona desde el frontend
- [ ] ✅ No hay errores CORS en DevTools

---

## 🎉 Resultado Final

```
✅ Frontend: https://tu-app.vercel.app
✅ Backend:  https://invoices-backend.fly.dev
✅ Database: PostgreSQL en Neon.tech (500MB gratis)
✅ CORS:     Configurado
✅ HTTPS:    Automático
✅ Costo:    $0.00 / mes 🎊
```

---

## 📈 Próximos Pasos (Opcional)

### Agregar más servicios

```bash
# Desplegar Document Service
cd document-service
fly launch --name invoices-document-service
fly deploy

# Desplegar Trace Service
cd trace-service
fly launch --name invoices-trace-service
fly deploy
```

### Agregar Kafka (Upstash - Gratis)

1. Crear cuenta en [Upstash.com](https://upstash.com)
2. Crear cluster de Kafka (10k mensajes/día gratis)
3. Obtener connection string
4. Configurar en servicios:
   ```bash
   fly secrets set KAFKA_BOOTSTRAP_SERVERS=<upstash-kafka-url>
   ```

### Monitoreo Gratuito

- **Uptime:** [UptimeRobot](https://uptimerobot.com) - 50 monitores gratis
- **Logs:** Fly.io logs en tiempo real: `fly logs`
- **Métricas:** Fly.io dashboard incluye métricas básicas

---

## 📞 Soporte

- **Fly.io Docs:** https://fly.io/docs
- **Render Docs:** https://render.com/docs
- **Neon Docs:** https://neon.tech/docs
- **Issues:** https://github.com/jefmonjor/invoices-back/issues

---

**¡Tu backend está listo para producción en Vercel! 🚀**

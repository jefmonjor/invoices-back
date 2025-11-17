# ⚡ Guía Rápida de Deployment

Sigue estos pasos en orden para desplegar **toda la arquitectura** en menos de 30 minutos.

---

## 📋 Antes de Empezar

Ejecuta el checklist:

```bash
./check-requirements.sh
```

Si todo está ✅, continúa con el deployment.

---

## 🚀 Paso 1: Generar JWT Secret (1 min)

```bash
# Genera y guarda este valor - lo usarás en TODOS los servicios
openssl rand -base64 64 | tr -d '\n'
```

📝 **Copia este valor** - lo necesitarás varias veces.

---

## ✈️ Paso 2: Deploy en Fly.io (10 mins)

### 2.1 Ejecutar Script de Deploy

```bash
./deploy-flyio-free-tier.sh
```

Este script desplegará:
- ✅ Gateway Service
- ✅ User Service
- ✅ Invoice Service

### 2.2 Configurar Secrets del Gateway

```bash
JWT_SECRET="tu_jwt_secret_del_paso_1"

fly secrets set -a invoices-backend \
  JWT_SECRET="$JWT_SECRET" \
  CORS_ALLOWED_ORIGINS="https://tu-app.vercel.app"
```

### 2.3 Configurar Secrets del User Service

Necesitas el **connection string de Neon** para `userdb`:

```bash
# Ejemplo de URL de Neon:
# postgresql://user:password@ep-xxx-xxx.eu-central-1.aws.neon.tech/userdb?sslmode=require

NEON_USER_DB="postgresql://..."
JWT_SECRET="mismo_del_paso_1"

fly secrets set -a invoices-user-service \
  SPRING_DATASOURCE_URL="$NEON_USER_DB" \
  JWT_SECRET="$JWT_SECRET"
```

### 2.4 Configurar Secrets del Invoice Service

Necesitas:
- Connection string de Neon para `invoicedb`
- Password de Upstash Redis

```bash
NEON_INVOICE_DB="postgresql://..."
JWT_SECRET="mismo_del_paso_1"
REDIS_PASSWORD="tu_upstash_redis_password"

fly secrets set -a invoices-invoice-service \
  SPRING_DATASOURCE_URL="$NEON_INVOICE_DB" \
  JWT_SECRET="$JWT_SECRET" \
  REDIS_HOST="subtle-parrot-38179.upstash.io" \
  REDIS_PASSWORD="$REDIS_PASSWORD" \
  REDIS_SSL="true"
```

### 2.5 Verificar que todo está corriendo

```bash
# Ver apps desplegadas
fly apps list | grep invoices

# Ver logs (en 3 terminales diferentes)
fly logs -a invoices-backend
fly logs -a invoices-user-service
fly logs -a invoices-invoice-service

# Health checks
curl https://invoices-backend.fly.dev/actuator/health
curl https://invoices-user-service.fly.dev/actuator/health
curl https://invoices-invoice-service.fly.dev/actuator/health
```

✅ **Si ves `{"status":"UP"}` en los 3, ¡perfecto!**

---

## 🎨 Paso 3: Deploy en Render (15 mins)

### 3.1 Conectar Repositorio

1. Ve a: https://dashboard.render.com/
2. Click **"New +"** → **"Blueprint"**
3. Conecta tu repositorio de GitHub: `jefmonjor/invoices-back`
4. Selecciona el archivo: `render.yaml`

### 3.2 Configurar Variables de Entorno

Render detectará automáticamente las variables. Configura las que tienen `sync: false`:

#### **Document Service:**

| Variable | Valor |
|----------|-------|
| `SPRING_DATASOURCE_URL` | `postgresql://...documentdb?sslmode=require` |
| `JWT_SECRET` | Mismo JWT del Paso 1 |
| `S3_ENDPOINT` | `https://YOUR_ACCOUNT_ID.r2.cloudflarestorage.com` |
| `S3_ACCESS_KEY` | Tu Cloudflare R2 Access Key |
| `S3_SECRET_KEY` | Tu Cloudflare R2 Secret Key |

#### **Trace Service:**

| Variable | Valor |
|----------|-------|
| `SPRING_DATASOURCE_URL` | `postgresql://...tracedb?sslmode=require` |
| `JWT_SECRET` | Mismo JWT del Paso 1 |
| `REDIS_HOST` | `subtle-parrot-38179.upstash.io` |
| `REDIS_PASSWORD` | Tu password de Upstash Redis |

### 3.3 Deploy

Click **"Apply"** - Render comenzará a construir y desplegar ambos servicios.

⏱️ **Esto tarda ~10-15 minutos** (primera vez). Puedes ver los logs en tiempo real.

### 3.4 Verificar

Una vez desplegados, Render te dará las URLs:

```bash
# Health checks (pueden tardar 30-60s si acaban de despertar)
curl https://invoices-document-service.onrender.com/actuator/health
curl https://invoices-trace-service.onrender.com/actuator/health
```

---

## 🔗 Paso 4: Conectar el Gateway con Todos los Servicios (2 mins)

El Gateway necesita conocer las URLs de todos los servicios.

### Opción A: Usar Eureka (si está habilitado)

Ya configurado, no necesitas hacer nada.

### Opción B: URLs Directas (Recomendado para free tier)

Actualiza el Gateway con las URLs de los servicios de Render:

```bash
fly secrets set -a invoices-backend \
  USER_SERVICE_URL="https://invoices-user-service.fly.dev" \
  INVOICE_SERVICE_URL="https://invoices-invoice-service.fly.dev" \
  DOCUMENT_SERVICE_URL="https://invoices-document-service.onrender.com" \
  TRACE_SERVICE_URL="https://invoices-trace-service.onrender.com"
```

---

## ✅ Paso 5: Testing End-to-End (5 mins)

### 5.1 Test de Autenticación

```bash
# Register nuevo usuario
curl -X POST https://invoices-backend.fly.dev/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!",
    "fullName": "Test User"
  }'
```

Deberías recibir un token JWT.

### 5.2 Login

```bash
curl -X POST https://invoices-backend.fly.dev/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!"
  }'
```

Copia el `token` de la respuesta.

### 5.3 Test de Invoice Creation

```bash
# Reemplaza YOUR_JWT_TOKEN con el token del login
TOKEN="tu_jwt_token_aqui"

curl -X POST https://invoices-backend.fly.dev/api/invoices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "Cliente Test",
    "clientEmail": "cliente@test.com",
    "total": 1000.00,
    "status": "PENDING"
  }'
```

Deberías recibir la factura creada.

### 5.4 Verificar que se creó el evento en Redis

Ve a tu dashboard de Upstash Redis:
- https://console.upstash.com/redis
- Deberías ver comandos ejecutados en las métricas

---

## 🎉 ¡Listo!

Tu arquitectura completa está desplegada:

```
✅ Gateway:  https://invoices-backend.fly.dev
✅ User:     https://invoices-user-service.fly.dev
✅ Invoice:  https://invoices-invoice-service.fly.dev
✅ Document: https://invoices-document-service.onrender.com
✅ Trace:    https://invoices-trace-service.onrender.com
```

---

## 🔧 Troubleshooting Rápido

### Error: "Connection refused" en Neon

**Problema:** La URL de Neon no es correcta.

**Solución:** Asegúrate de incluir `?sslmode=require` al final:
```
postgresql://user:pass@host.neon.tech/database?sslmode=require
```

### Error: "Redis timeout"

**Problema:** Password de Redis incorrecto o host incorrecto.

**Solución:**
1. Verifica el host: `subtle-parrot-38179.upstash.io`
2. Verifica que `REDIS_SSL=true`
3. Obtén el password correcto de Upstash

### Servicio en Render no responde

**Problema:** El servicio está dormido (free tier).

**Solución:** Espera 30-60 segundos. La primera request despierta el servicio.

### Gateway no encuentra los servicios

**Problema:** URLs no configuradas.

**Solución:** Ejecuta el Paso 4 para configurar las URLs de los servicios.

---

## 📊 Monitoreo

### Fly.io

```bash
# Ver logs en tiempo real
fly logs -a invoices-backend

# Ver métricas
fly dashboard -a invoices-backend
```

### Render

Dashboard web: https://dashboard.render.com/

### Upstash Redis

Dashboard: https://console.upstash.com/redis

### Neon PostgreSQL

Dashboard: https://console.neon.tech/

---

## 💰 Costos

**Total: $0/mes**

- Fly.io: 3 apps × 256MB = $0
- Render: 2 servicios × 750h/mes = $0
- Neon: 3GB PostgreSQL = $0
- Upstash: 10k commands/día = $0
- Cloudflare R2: 10GB = $0

---

## 📚 Próximos Pasos

1. **Deploy Frontend en Vercel**
   - Configura `NEXT_PUBLIC_API_URL=https://invoices-backend.fly.dev`

2. **Configurar CORS en Gateway**
   - Actualiza `CORS_ALLOWED_ORIGINS` con tu URL de Vercel

3. **Monitoreo**
   - Configura alertas en cada plataforma
   - Monitorea uso de free tiers

4. **Testing**
   - Prueba todos los endpoints
   - Verifica que los eventos se registran en trace-service

---

¿Problemas? Revisa `HYBRID_DEPLOYMENT_GUIDE.md` para más detalles.

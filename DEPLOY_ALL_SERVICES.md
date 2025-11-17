# 🚀 Desplegar TODOS los Microservicios

Esta guía te muestra cómo desplegar todos los microservicios del sistema en Fly.io.

---

## 📋 Servicios a Desplegar

```
✅ Gateway Service    (ya desplegado) → https://invoices-backend.fly.dev
🔄 User Service       → https://invoices-user-service.fly.dev
🔄 Invoice Service    → https://invoices-invoice-service.fly.dev
🔄 Document Service   → https://invoices-document-service.fly.dev
🔄 Trace Service      → https://invoices-trace-service.fly.dev
```

---

## ⚡ Opción 1: Script Automático (RECOMENDADO)

### Comando:

```bash
cd /Users/Jefferson/Documents/proyecto/invoices-back
./deploy-all-services.sh
```

**Tiempo estimado:** 15-20 minutos (despliega los 4 servicios en paralelo)

### Lo que hace el script:

1. ✅ Verifica prerequisitos (Fly CLI, .env.production)
2. ✅ Crea apps en Fly.io para cada servicio
3. ✅ Genera fly.toml para cada servicio
4. ✅ Configura secrets (JWT, Database URLs)
5. ✅ Despliega cada servicio
6. ✅ Verifica health checks

### Después del deployment:

El script automáticamente:
- ✅ Ejecuta migraciones de Flyway
- ✅ Crea el usuario admin en `userdb`
- ✅ Crea todas las tablas necesarias

---

## 🔧 Opción 2: Desplegar Servicios Manualmente

Si prefieres control total, puedes desplegar cada servicio uno por uno:

### 1. User Service

```bash
cd user-service

# Crear fly.toml
cat > fly.toml << 'EOF'
app = "invoices-user-service"
primary_region = "ams"

[build]
  dockerfile = "Dockerfile"

[env]
  SPRING_PROFILES_ACTIVE = "prod"
  SERVER_PORT = "8082"

[http_service]
  internal_port = 8082
  force_https = true
  auto_stop_machines = false
  auto_start_machines = true
  min_machines_running = 1

[[services]]
  protocol = "tcp"
  internal_port = 8082

  [[services.ports]]
    port = 80
    handlers = ["http"]
    force_https = true

  [[services.ports]]
    port = 443
    handlers = ["tls", "http"]

  [[services.http_checks]]
    interval = "30s"
    timeout = "5s"
    grace_period = "20s"
    method = "GET"
    path = "/actuator/health"
    protocol = "http"

[[vm]]
  size = "shared-cpu-1x"
  memory = "512mb"
EOF

# Cargar variables
source ../.env.production

# Crear app
fly launch --name invoices-user-service --region ams --no-deploy --copy-config --yes

# Configurar secrets
fly secrets set \
  SPRING_PROFILES_ACTIVE=prod \
  JWT_SECRET="$JWT_SECRET" \
  EUREKA_CLIENT_ENABLED=false \
  SPRING_DATASOURCE_URL="$USER_DB_URL" \
  -a invoices-user-service

# Desplegar
fly deploy -a invoices-user-service

# Verificar
curl https://invoices-user-service.fly.dev/actuator/health
```

### 2. Invoice Service

```bash
cd ../invoice-service

# Crear fly.toml (similar al anterior pero puerto 8081)
cat > fly.toml << 'EOF'
app = "invoices-invoice-service"
primary_region = "ams"

[build]
  dockerfile = "Dockerfile"

[env]
  SPRING_PROFILES_ACTIVE = "prod"
  SERVER_PORT = "8081"

[http_service]
  internal_port = 8081
  force_https = true
  auto_stop_machines = false
  auto_start_machines = true
  min_machines_running = 1

[[services]]
  protocol = "tcp"
  internal_port = 8081

  [[services.ports]]
    port = 80
    handlers = ["http"]
    force_https = true

  [[services.ports]]
    port = 443
    handlers = ["tls", "http"]

  [[services.http_checks]]
    interval = "30s"
    timeout = "5s"
    grace_period = "20s"
    method = "GET"
    path = "/actuator/health"
    protocol = "http"

[[vm]]
  size = "shared-cpu-1x"
  memory = "512mb"
EOF

# Cargar variables
source ../.env.production

# Crear app
fly launch --name invoices-invoice-service --region ams --no-deploy --copy-config --yes

# Configurar secrets
fly secrets set \
  SPRING_PROFILES_ACTIVE=prod \
  JWT_SECRET="$JWT_SECRET" \
  EUREKA_CLIENT_ENABLED=false \
  SPRING_DATASOURCE_URL="$INVOICE_DB_URL" \
  -a invoices-invoice-service

# Desplegar
fly deploy -a invoices-invoice-service

# Verificar
curl https://invoices-invoice-service.fly.dev/actuator/health
```

### 3. Document Service

```bash
cd ../document-service

# Similar a los anteriores pero puerto 8083
# ... (mismo proceso)
```

### 4. Trace Service

```bash
cd ../trace-service

# Similar a los anteriores pero puerto 8084
# ... (mismo proceso)
```

---

## ⏱️ Tiempos de Deployment

| Servicio | Tiempo Estimado |
|----------|-----------------|
| User Service | 4-5 minutos |
| Invoice Service | 4-5 minutos |
| Document Service | 4-5 minutos |
| Trace Service | 4-5 minutos |
| **Total (secuencial)** | **16-20 minutos** |

---

## ✅ Verificación Post-Deployment

### 1. Verificar que todos los servicios estén UP

```bash
# User Service
curl https://invoices-user-service.fly.dev/actuator/health

# Invoice Service
curl https://invoices-invoice-service.fly.dev/actuator/health

# Document Service
curl https://invoices-document-service.fly.dev/actuator/health

# Trace Service
curl https://invoices-trace-service.fly.dev/actuator/health
```

**Todos deben responder:** `{"status":"UP"}`

### 2. Verificar que el usuario admin existe

```bash
# Test de login
curl -X POST https://invoices-user-service.fly.dev/api/auth/login \
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

### 3. Ver logs de cada servicio

```bash
# User Service
fly logs -a invoices-user-service

# Invoice Service
fly logs -a invoices-invoice-service

# Document Service
fly logs -a invoices-document-service

# Trace Service
fly logs -a invoices-trace-service
```

**Buscar en logs:**
- ✅ "Started [Service]Application in X seconds"
- ✅ "Flyway migration completed successfully"
- ✅ "Successfully validated the applied migrations"

---

## 🔄 Actualizar Gateway para Usar URLs Reales

Después de desplegar todos los servicios, actualiza el gateway para que use las URLs de Fly.io en lugar de Eureka:

```bash
cd gateway-service

# Actualizar secrets con URLs de servicios
fly secrets set \
  USER_SERVICE_URL=https://invoices-user-service.fly.dev \
  INVOICE_SERVICE_URL=https://invoices-invoice-service.fly.dev \
  DOCUMENT_SERVICE_URL=https://invoices-document-service.fly.dev \
  TRACE_SERVICE_URL=https://invoices-trace-service.fly.dev \
  -a invoices-backend

# Redeploy gateway
fly deploy -a invoices-backend
```

---

## 📊 Arquitectura Final

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
         PostgreSQL (Neon)
    (4 databases separadas)
```

---

## 🐛 Troubleshooting

### Error: "Out of memory"

**Solución:** Aumentar memoria a 1GB

```bash
fly scale memory 1024 -a invoices-user-service
fly scale memory 1024 -a invoices-invoice-service
```

⚠️ Nota: Esto puede salir del free tier

---

### Error: "Connection to database failed"

**Verificar:**

```bash
# Verificar que DATABASE_URL esté configurada
fly secrets list -a invoices-user-service

# Si falta, agregarla:
fly secrets set SPRING_DATASOURCE_URL="$USER_DB_URL" -a invoices-user-service
```

---

### Error: "Flyway migration failed"

**Ver logs:**

```bash
fly logs -a invoices-user-service | grep Flyway
```

**Solución común:** Borrar y recrear la base de datos en Neon

---

## 💰 Costo Estimado

Con free tier de Fly.io:

```
✅ Gateway Service:  1 VM (free)
✅ User Service:     1 VM (free)
✅ Invoice Service:  1 VM (free)
⚠️ Document Service: 1 VM (requiere upgrade)
⚠️ Trace Service:    1 VM (requiere upgrade)

Free tier: 3 VMs máximo
Total services: 5 VMs
```

**Opciones:**

1. **Solo desplegar 3 servicios esenciales** (free):
   - Gateway + User + Invoice

2. **Upgrade a plan pagado** ($5-10/mes):
   - Todos los servicios

---

## 🎯 Resultado Final Esperado

```
✅ Gateway:   https://invoices-backend.fly.dev
✅ User:      https://invoices-user-service.fly.dev
✅ Invoice:   https://invoices-invoice-service.fly.dev
✅ Document:  https://invoices-document-service.fly.dev
✅ Trace:     https://invoices-trace-service.fly.dev
✅ Database:  Neon PostgreSQL (4 databases)
✅ Region:    Amsterdam (ams)
✅ HTTPS:     Automático
```

---

## 🚀 Comando Final

```bash
cd /Users/Jefferson/Documents/proyecto/invoices-back
./deploy-all-services.sh
```

¡Y espera 15-20 minutos! ☕

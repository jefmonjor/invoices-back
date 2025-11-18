# Variables de Entorno - Invoices Monolith

Documentación completa de las variables de entorno para el monolito de gestión de facturas.

---

## 📋 Índice

- [Variables Principales](#-variables-principales)
- [Base de Datos](#-base-de-datos)
- [Redis (Event Streaming)](#-redis-event-streaming)
- [S3/MinIO (Storage)](#-s3minio-storage)
- [JWT Security](#-jwt-security)
- [CORS](#-cors)
- [Logging](#-logging)
- [Ejemplo .env](#-ejemplo-env)

---

## 🔧 Variables Principales

### Servidor

| Variable | Descripción | Requerida | Valor por Defecto |
|----------|-------------|-----------|-------------------|
| `SERVER_PORT` | Puerto de la aplicación | No | `8080` |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring (dev/prod) | No | `dev` |

---

## 💾 Base de Datos

El monolito usa una **única base de datos PostgreSQL** con todas las tablas consolidadas.

| Variable | Descripción | Requerida | Valor por Defecto | Ejemplo |
|----------|-------------|-----------|-------------------|---------|
| `SPRING_DATASOURCE_URL` | URL de conexión PostgreSQL | **Sí** | `jdbc:postgresql://localhost:5432/invoices` | `jdbc:postgresql://db.example.com:5432/invoices` |
| `DB_USERNAME` | Usuario de la base de datos | **Sí** | `postgres` | `invoices_user` |
| `DB_PASSWORD` | Contraseña de la base de datos | **Sí** | `postgres` | `my_secure_password` |

### Tablas en la Base de Datos

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

### Servicios PostgreSQL Gratuitos

- **[Neon](https://neon.tech)** - 0.5 GB gratis, serverless ⭐
- **[Supabase](https://supabase.com)** - 500 MB gratis
- **[Fly Postgres](https://fly.io/docs/postgres/)** - 3 GB gratis

---

## 📡 Redis (Event Streaming)

El monolito usa **Redis Streams** para eventos asíncronos.

| Variable | Descripción | Requerida | Valor por Defecto | Ejemplo |
|----------|-------------|-----------|-------------------|---------|
| `REDIS_HOST` | Host de Redis | **Sí** | `localhost` | `redis.upstash.io` |
| `REDIS_PORT` | Puerto de Redis | No | `6379` | `6379` |
| `REDIS_PASSWORD` | Contraseña de Redis | No | _(vacío)_ | `my_redis_password` |
| `REDIS_SSL` | Usar SSL/TLS | No | `false` | `true` |
| `REDIS_STREAM_INVOICE_EVENTS` | Nombre del stream de eventos | No | `invoice-events` | `invoice-events` |
| `REDIS_STREAM_INVOICE_DLQ` | Stream de Dead Letter Queue | No | `invoice-events-dlq` | `invoice-events-dlq` |
| `REDIS_CONSUMER_GROUP` | Grupo de consumidores | No | `trace-group` | `trace-group` |
| `REDIS_CONSUMER_NAME` | Nombre del consumidor | No | `trace-consumer` | `trace-consumer` |

### Eventos Redis

El módulo **Trace** consume eventos del módulo **Invoice**:
- `INVOICE_CREATED` - Factura creada
- `INVOICE_UPDATED` - Factura actualizada
- `INVOICE_DELETED` - Factura eliminada
- `INVOICE_PAID` - Factura pagada

### Servicios Redis Gratuitos

- **[Upstash Redis](https://upstash.com)** - 10,000 comandos/día gratis ⭐
- **[Redis Cloud](https://redis.com/try-free/)** - 30 MB gratis
- **[Fly Redis](https://fly.io/docs/reference/redis/)** - 256 MB gratis

---

## 📦 S3/MinIO (Storage)

Almacenamiento de documentos PDF en S3-compatible storage.

| Variable | Descripción | Requerida | Valor por Defecto | Ejemplo |
|----------|-------------|-----------|-------------------|---------|
| `S3_ENDPOINT` | Endpoint del servicio S3 | **Sí** | `http://localhost:9000` | `https://xxxxx.r2.cloudflarestorage.com` |
| `S3_ACCESS_KEY` | Access Key de S3 | **Sí** | `minioadmin` | `your_access_key` |
| `S3_SECRET_KEY` | Secret Key de S3 | **Sí** | `minioadmin123` | `your_secret_key` |
| `S3_BUCKET_NAME` | Nombre del bucket | **Sí** | `invoices-documents` | `my-invoices-bucket` |
| `S3_REGION` | Región de S3 | No | `auto` | `us-east-1` |
| `S3_PATH_STYLE_ACCESS` | Usar path-style access | No | `true` | `false` |

### Servicios S3 Gratuitos

- **[Cloudflare R2](https://www.cloudflare.com/products/r2/)** - 10 GB gratis ⭐
- **MinIO** - Self-hosted (desarrollo local)
- **[Backblaze B2](https://www.backblaze.com/b2/cloud-storage.html)** - 10 GB gratis

---

## 🔐 JWT Security

Configuración de tokens JWT para autenticación.

| Variable | Descripción | Requerida | Valor por Defecto | Ejemplo |
|----------|-------------|-----------|-------------------|---------|
| `JWT_SECRET` | Clave secreta para firmar tokens | **Sí** | ⚠️ _cambiar en producción_ | `your-super-secret-jwt-key-min-32-chars-base64` |
| `JWT_EXPIRATION_MS` | Tiempo de expiración del token (ms) | No | `3600000` (1 hora) | `7200000` (2 horas) |
| `JWT_ISSUER` | Emisor del token | No | `invoices-backend` | `my-company` |

### Generar JWT_SECRET Seguro

```bash
# Opción 1: OpenSSL
openssl rand -base64 32

# Opción 2: Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

# Opción 3: Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

⚠️ **IMPORTANTE:** El `JWT_SECRET` debe tener al menos 32 caracteres y ser único por entorno.

---

## 🌐 CORS

Configuración de CORS para conectar con frontend.

| Variable | Descripción | Requerida | Valor por Defecto | Ejemplo |
|----------|-------------|-----------|-------------------|---------|
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos (separados por coma) | No | `http://localhost:3000,http://localhost:5173` | `https://myapp.vercel.app` |
| `CORS_ALLOWED_METHODS` | Métodos HTTP permitidos | No | `GET,POST,PUT,DELETE,OPTIONS` | `GET,POST,PUT,DELETE,PATCH,OPTIONS` |
| `CORS_ALLOWED_HEADERS` | Headers permitidos | No | `*` | `Content-Type,Authorization` |
| `CORS_EXPOSED_HEADERS` | Headers expuestos | No | `Authorization` | `Authorization,X-Total-Count` |
| `CORS_ALLOW_CREDENTIALS` | Permitir credenciales | No | `true` | `true` |
| `CORS_MAX_AGE` | Tiempo de caché de preflight (segundos) | No | `3600` | `7200` |

---

## 📝 Logging

Configuración de niveles de logging.

| Variable | Descripción | Valor por Defecto | Opciones |
|----------|-------------|-------------------|----------|
| `LOG_LEVEL_ROOT` | Nivel de log raíz | `INFO` | `TRACE, DEBUG, INFO, WARN, ERROR` |
| `LOG_LEVEL_APP` | Nivel de log de la app | `DEBUG` | `TRACE, DEBUG, INFO, WARN, ERROR` |

---

## 📄 Ejemplo .env

### Desarrollo Local

```env
# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/invoices
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# S3/MinIO (Local)
S3_ENDPOINT=http://localhost:9000
S3_ACCESS_KEY=minioadmin
S3_SECRET_KEY=minioadmin123
S3_BUCKET_NAME=invoices-documents
S3_REGION=us-east-1

# JWT
JWT_SECRET=your-super-secret-jwt-key-min-32-chars-base64-encoded-change-in-production
JWT_EXPIRATION_MS=3600000
JWT_ISSUER=invoices-backend

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Logging
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
```

### Producción (Fly.io)

```env
# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# Database (Neon)
SPRING_DATASOURCE_URL=jdbc:postgresql://ep-xxxx.us-east-1.aws.neon.tech/invoices?sslmode=require
DB_USERNAME=your_neon_user
DB_PASSWORD=your_neon_password

# Redis (Upstash)
REDIS_HOST=usw1-xxxx.upstash.io
REDIS_PORT=6379
REDIS_PASSWORD=your_upstash_password
REDIS_SSL=true

# S3 (Cloudflare R2)
S3_ENDPOINT=https://xxxxx.r2.cloudflarestorage.com
S3_ACCESS_KEY=your_r2_access_key
S3_SECRET_KEY=your_r2_secret_key
S3_BUCKET_NAME=invoices-prod
S3_REGION=auto

# JWT (GENERAR NUEVO!)
JWT_SECRET=production-secret-key-min-32-chars-unique-per-environment
JWT_EXPIRATION_MS=3600000
JWT_ISSUER=invoices-production

# CORS
CORS_ALLOWED_ORIGINS=https://myapp.vercel.app,https://www.myapp.com

# Logging
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=INFO
```

---

## 🔒 Seguridad

### Variables Secretas

Estas variables **NUNCA** deben commitearse a Git:

- ✅ `DB_PASSWORD`
- ✅ `REDIS_PASSWORD`
- ✅ `S3_SECRET_KEY`
- ✅ `JWT_SECRET`

### Configurar Secrets en Fly.io

```bash
fly secrets set JWT_SECRET="your-secret-key"
fly secrets set DB_PASSWORD="your-db-password"
fly secrets set REDIS_PASSWORD="your-redis-password"
fly secrets set S3_SECRET_KEY="your-s3-secret"
```

---

## ✅ Validación

Para validar que todas las variables están configuradas correctamente:

```bash
# Ejecutar health check
curl http://localhost:8080/actuator/health

# Verificar logs
docker logs invoices-monolith

# Probar conexión a DB
docker exec -it invoices-monolith \
  psql -h postgres -U postgres -d invoices -c "SELECT 1;"
```

---

## 📚 Referencias

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [Neon Database Setup](./NEON_DATABASE_SETUP.md)
- [Free Services Setup](./FREE_SERVICES_SETUP.md)

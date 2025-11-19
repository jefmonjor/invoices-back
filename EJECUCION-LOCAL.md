# 🏃 Ejecución Local - Invoices Backend

## ✅ Pre-requisitos

1. **Java 21** instalado (verificar con `java -version`)
2. **Maven 3.9+** instalado (verificar con `mvn -version`)

## 🚀 Opción 1: Ejecutar con Script (Recomendado)

### Primera vez o después de cambios:
```bash
./run-local.sh
```

Este script:
- ✅ Configura todas las variables de entorno
- ✅ Compila el proyecto con Maven
- ✅ Ejecuta la aplicación
- ✅ Usa servicios remotos (Neon, Upstash, R2)

### Ejecuciones subsecuentes (más rápido):
```bash
./run-local-fast.sh
```

Este script:
- ⚡ NO compila (usa el JAR existente)
- ⚡ Inicia más rápido
- ⚠️ Requiere haber ejecutado `run-local.sh` al menos una vez

## 🛠️ Opción 2: Ejecutar con Maven directamente

```bash
cd invoices-monolith

# Configurar variables de entorno (ver archivo .env-example)
export SPRING_DATASOURCE_URL="jdbc:postgresql://ep-delicate-snow-abyzqltv-pooler.eu-west-2.aws.neon.tech:5432/neondb?sslmode=require"
export DB_USERNAME="neondb_owner"
export DB_PASSWORD="npg_02GsdHFqhfoU"
# ... (copiar todas las variables del script)

# Ejecutar con Maven
mvn spring-boot:run
```

## 🐳 Opción 3: Usar servicios locales (Docker)

Si prefieres NO usar los servicios remotos, puedes levantar PostgreSQL y Redis locales:

```bash
# PostgreSQL local
docker run -d \
  --name postgres-local \
  -e POSTGRES_DB=invoices \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:17-alpine

# Redis local
docker run -d \
  --name redis-local \
  -p 6379:6379 \
  redis:7-alpine

# MinIO local (S3-compatible)
docker run -d \
  --name minio-local \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

Luego ejecutar sin exportar las variables de servicios remotos (usa los defaults de `application.yml`).

## 📍 URLs Importantes

| Servicio | URL |
|----------|-----|
| API Base | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health Check | http://localhost:8080/actuator/health |
| Metrics | http://localhost:8080/actuator/metrics |
| API Docs (JSON) | http://localhost:8080/api-docs |

## 🔍 Verificar que funciona

### 1. Health Check
```bash
curl http://localhost:8080/actuator/health
```

Debería retornar:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

### 2. Crear un usuario
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@example.com",
    "password": "Admin123!",
    "role": "ADMIN"
  }'
```

### 3. Autenticarse
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin123!"
  }'
```

## 🐛 Debugging

### Ver logs en tiempo real
Los logs se muestran en la consola. Para más detalle, cambia el nivel de log:

```bash
export LOG_LEVEL_APP=DEBUG
```

### Problemas comunes

#### ❌ "Connection refused" a PostgreSQL
- Verifica que Neon esté accesible: `ping ep-delicate-snow-abyzqltv-pooler.eu-west-2.aws.neon.tech`
- O usa PostgreSQL local (ver Opción 3)

#### ❌ "Failed to initialize MinIO"
- Verifica que Cloudflare R2 esté accesible
- O usa MinIO local (ver Opción 3)

#### ❌ "JWT_SECRET must not be null"
- Asegúrate de exportar `JWT_SECRET` (el script lo hace automáticamente)

#### ❌ Puerto 8080 en uso
Cambia el puerto:
```bash
export SERVER_PORT=8081
```

## 🔄 Hot Reload (Desarrollo activo)

Para desarrollo con hot reload, usa Spring Boot DevTools:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"
```

O con IntelliJ IDEA:
1. Abre el proyecto
2. Run → Edit Configurations
3. Agrega las variables de entorno
4. Run en modo Debug (permite hot swap de código)

## 📦 Variables de Entorno Completas

Ver archivo: `configure-secrets.sh` (contiene TODAS las credenciales)

O copiar desde `run-local.sh` líneas 13-40.

## 🚫 IMPORTANTE - Seguridad

⚠️ **Los scripts usan credenciales REALES de producción**
- Cloudflare R2 → Producción
- Neon PostgreSQL → Producción
- Upstash Redis → Producción

**NO** hagas operaciones destructivas en local a menos que sepas lo que haces.

Para desarrollo seguro, usa la **Opción 3** con servicios locales en Docker.

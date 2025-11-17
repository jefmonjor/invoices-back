# Invoices Monolith

**Aplicación monolítica consolidada** - Todos los servicios de facturación en una sola aplicación para simplificar el despliegue y la operación.

## 📦 Servicios Consolidados

Este monolito integra todos los microservicios anteriores:

- **User Service**: Gestión de usuarios y autenticación (JWT)
- **Invoice Service**: Gestión de facturas, ítems, clientes y empresas
- **Document Service**: Generación de PDFs y almacenamiento de documentos (S3/R2)
- **Trace Service**: Auditoría y trazabilidad de eventos

## 🚀 Despliegue Rápido en Fly.io

### Prerequisitos

```bash
# Instalar Fly.io CLI
curl -L https://fly.io/install.sh | sh

# Login
flyctl auth login
```

### Configuración de Servicios Externos

Necesitas configurar estos servicios gratuitos:

1. **Base de datos**: [Neon PostgreSQL](https://neon.tech) (Free tier)
2. **Redis**: [Upstash Redis](https://upstash.com) (Free tier)
3. **Almacenamiento**: [Cloudflare R2](https://cloudflare.com/products/r2/) (Free tier hasta 10GB)

### Despliegue

```bash
cd invoices-monolith

# 1. Crear la app en Fly.io
flyctl apps create invoices-monolith

# 2. Configurar secretos
flyctl secrets set \
  SPRING_DATASOURCE_URL="postgresql://user:pass@your-neon-host.neon.tech/invoices?sslmode=require" \
  DB_USERNAME="your_db_user" \
  DB_PASSWORD="your_db_password" \
  JWT_SECRET="your-super-secret-jwt-key-min-32-chars-base64-encoded" \
  REDIS_HOST="your-redis-host.upstash.io" \
  REDIS_PORT="6379" \
  REDIS_PASSWORD="your-redis-password" \
  S3_ENDPOINT="https://your-account-id.r2.cloudflarestorage.com" \
  S3_ACCESS_KEY="your-r2-access-key" \
  S3_SECRET_KEY="your-r2-secret-key" \
  S3_BUCKET_NAME="invoices-documents"

# 3. Desplegar
flyctl deploy
```

## 🏗️ Desarrollo Local

### Con Docker Compose

La forma más fácil es usar el docker-compose.yml del proyecto raíz:

```bash
cd /home/user/invoices-back
docker-compose up -d postgres redis minio
```

### Ejecutar la aplicación

```bash
cd invoices-monolith

# Compilar
mvn clean package

# Ejecutar
java -jar target/invoices-monolith-1.0.0.jar
```

La aplicación estará disponible en `http://localhost:8080`

### Swagger UI

Accede a la documentación de la API en: `http://localhost:8080/swagger-ui.html`

## 🔧 Variables de Entorno

### Obligatorias

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | URL de conexión PostgreSQL | `postgresql://user:pass@host:5432/invoices?sslmode=require` |
| `DB_USERNAME` | Usuario de base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de base de datos | `secretpass` |
| `JWT_SECRET` | Secreto para JWT (mín 32 chars) | `your-secret-key-32-chars-minimum` |
| `REDIS_HOST` | Host de Redis | `localhost` o `redis-host.upstash.io` |
| `REDIS_PASSWORD` | Contraseña de Redis | `redis-password` |
| `S3_ENDPOINT` | Endpoint S3/R2 | `https://account-id.r2.cloudflarestorage.com` |
| `S3_ACCESS_KEY` | Access key S3/R2 | `your-access-key` |
| `S3_SECRET_KEY` | Secret key S3/R2 | `your-secret-key` |
| `S3_BUCKET_NAME` | Nombre del bucket | `invoices-documents` |

### Opcionales

| Variable | Descripción | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Puerto del servidor | `8080` |
| `LOG_LEVEL_ROOT` | Nivel de log raíz | `INFO` |
| `LOG_LEVEL_APP` | Nivel de log de la app | `DEBUG` |
| `REDIS_PORT` | Puerto de Redis | `6379` |
| `REDIS_SSL` | Usar SSL para Redis | `false` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos para CORS | `http://localhost:3000` |

## 📊 Endpoints Principales

### Autenticación
- `POST /api/auth/login` - Iniciar sesión
- `POST /api/auth/register` - Registrar usuario

### Usuarios
- `GET /api/users` - Listar usuarios
- `GET /api/users/{id}` - Obtener usuario
- `PUT /api/users/{id}` - Actualizar usuario
- `DELETE /api/users/{id}` - Eliminar usuario

### Facturas
- `GET /api/invoices` - Listar facturas
- `POST /api/invoices` - Crear factura
- `GET /api/invoices/{id}` - Obtener factura
- `PUT /api/invoices/{id}` - Actualizar factura
- `DELETE /api/invoices/{id}` - Eliminar factura

### Documentos
- `POST /api/documents/upload` - Subir documento
- `GET /api/documents/{id}` - Obtener documento
- `GET /api/documents/{id}/download` - Descargar documento
- `DELETE /api/documents/{id}` - Eliminar documento

### Trazabilidad
- `GET /api/audit-logs` - Listar logs de auditoría
- `GET /api/audit-logs/{id}` - Obtener log de auditoría

## 🏥 Health Checks

- **Health**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Info**: `GET /actuator/info`

## 🧪 Tests

```bash
# Ejecutar tests
mvn test

# Con coverage
mvn test jacoco:report
```

## 📁 Estructura del Proyecto

```
invoices-monolith/
├── src/main/java/com/invoices/
│   ├── InvoicesApplication.java          # Clase principal
│   ├── config/                            # Configuración global
│   ├── security/                          # Seguridad y JWT
│   ├── user/                              # Módulo de usuarios
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   ├── invoice/                           # Módulo de facturas
│   │   ├── presentation/controllers/
│   │   ├── domain/usecases/
│   │   ├── infrastructure/persistence/
│   │   └── ...
│   ├── document/                          # Módulo de documentos
│   │   ├── controller/
│   │   ├── service/
│   │   └── ...
│   └── trace/                             # Módulo de auditoría
│       ├── controller/
│       ├── service/
│       └── ...
├── src/main/resources/
│   ├── application.yml                    # Configuración principal
│   ├── db/migration/                      # Migraciones Flyway
│   └── jasper-templates/                  # Templates JasperReports
├── Dockerfile                             # Multi-stage build
├── fly.toml                               # Configuración Fly.io
└── pom.xml                                # Dependencias Maven
```

## 🔄 Migración desde Microservicios

Este monolito consolida los siguientes microservicios anteriores:

- `user-service` → `com.invoices.user.*`
- `invoice-service` → `com.invoices.invoice.*`
- `document-service` → `com.invoices.document.*`
- `trace-service` → `com.invoices.trace.*`
- `gateway-service` → Integrado en la configuración de seguridad

**Ventajas del monolito:**
- ✅ Un solo despliegue
- ✅ Configuración simplificada
- ✅ Menor complejidad operacional
- ✅ Mejor para equipos pequeños
- ✅ Más fácil de debugear

## 📝 Notas de Producción

### Base de Datos

La aplicación usa una sola base de datos PostgreSQL con todas las tablas. Flyway gestiona las migraciones automáticamente.

### Escalamiento

En Fly.io puedes escalar vertical u horizontalmente:

```bash
# Escalar memoria
flyctl scale memory 1024  # 1GB

# Escalar instancias
flyctl scale count 2
```

### Monitoreo

Fly.io proporciona métricas automáticas. Puedes verlas en:
```bash
flyctl dashboard
```

## 🛠️ Troubleshooting

### La aplicación no inicia

1. Verifica las variables de entorno: `flyctl secrets list`
2. Revisa los logs: `flyctl logs`
3. Verifica la conexión a la base de datos

### Errores de conexión a Redis

1. Verifica que Redis SSL esté configurado correctamente
2. Para Upstash Redis, asegúrate de usar `REDIS_SSL=true`

### Problemas con S3/R2

1. Verifica las credenciales de R2
2. Asegúrate de que el bucket existe
3. Verifica los permisos del access key

## 📞 Soporte

Para problemas o preguntas, abre un issue en el repositorio.

## 📄 Licencia

Este proyecto es privado y confidencial.

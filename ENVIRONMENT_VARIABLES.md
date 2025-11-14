# 🔧 Guía de Variables de Entorno

Documentación completa de todas las variables de entorno configurables en cada microservicio.

---

## 🚀 Config Server (Puerto 8888)

### Variables Principales

| Variable | Descripción | Valores | Default |
|----------|-------------|---------|---------|
| `CONFIG_PROFILE` | Profile activo | `native`, `git` | `native` |
| `CONFIG_GIT_URI` | URI del repositorio Git | URL | `https://github.com/jefmonjor/invoices-back-config.git` |
| `CONFIG_GIT_BRANCH` | Branch de Git | String | `main` |
| `CONFIG_NATIVE_PATH` | Path para configuraciones nativas | Path | `classpath:/config` |
| `CONFIG_SERVER_USERNAME` | Usuario para basic auth (opcional) | String | `config-admin` |
| `CONFIG_SERVER_PASSWORD` | Password para basic auth (opcional) | String | `config-password` |
| `LOG_LEVEL_ROOT` | Log level root | `DEBUG`, `INFO`, `WARN`, `ERROR` | `INFO` |
| `LOG_LEVEL_CONFIG` | Log level config server | `DEBUG`, `INFO`, `WARN`, `ERROR` | `DEBUG` |

### Configurar Profile

```bash
# Development (logging DEBUG)
export CONFIG_PROFILE=dev

# Test (logging INFO)
export CONFIG_PROFILE=test

# Production (logging WARN)
export CONFIG_PROFILE=prod
```

### Ejemplo: Usar Git Backend

```bash
export CONFIG_PROFILE=git
export CONFIG_GIT_URI=https://github.com/tu-org/config-repo.git
export CONFIG_GIT_BRANCH=main
export CONFIG_SERVER_USERNAME=admin
export CONFIG_SERVER_PASSWORD=secure-password
```

---

## 📨 Trace Service (Puerto 8084)

### Variables de Kafka

| Variable | Descripción | Default |
|----------|-------------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | Servidores Kafka | `localhost:9092` |
| `KAFKA_TRACE_GROUP_ID` | Consumer group ID | `trace-group` |
| `KAFKA_INVOICE_TOPIC` | Topic principal | `invoice-events` |
| `KAFKA_INVOICE_DLQ_TOPIC` | **Dead Letter Queue topic** | `invoice-events-dlq` |

### Variables de Base de Datos

| Variable | Descripción | Default |
|----------|-------------|---------|
| `TRACE_DB_HOST` | Host PostgreSQL | `localhost` |
| `TRACE_DB_PORT` | Puerto PostgreSQL | `5432` |
| `TRACE_DB_NAME` | Nombre de BD | `tracedb` |
| `TRACE_DB_USERNAME` | Usuario BD | `trace_service_user` |
| `TRACE_DB_PASSWORD` | Password BD | `password` |

### Variables de JWT

| Variable | Descripción | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Secret key JWT (min 32 chars) | `default-secret-key...` |
| `JWT_ISSUER` | Emisor del JWT | `invoices-backend` |

### Configurar DLQ (CRÍTICO)

```bash
# Configurar Dead Letter Queue
export KAFKA_INVOICE_DLQ_TOPIC=invoice-events-dlq

# Configurar Kafka servers (producción)
export KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092,kafka3:9092

# Configurar consumer group
export KAFKA_TRACE_GROUP_ID=trace-group-prod
```

### Logging

| Variable | Descripción | Default |
|----------|-------------|---------|
| `LOG_LEVEL_ROOT` | Log level root | `INFO` |
| `LOG_LEVEL_APP` | Log level aplicación | `DEBUG` |

---

## 🔒 Gateway Service (Puerto 8080)

### Variables de JWT

| Variable | Descripción | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Secret key JWT (min 32 chars) | `default-secret-key...` |
| `JWT_ISSUER` | Emisor del JWT | `invoices-backend` |

### Variables de Eureka

| Variable | Descripción | Default |
|----------|-------------|---------|
| `EUREKA_SERVER_HOST` | Host Eureka | `localhost` |
| `EUREKA_SERVER_PORT` | Puerto Eureka | `8761` |
| `EUREKA_USERNAME` | Usuario Eureka | `eureka-admin` |
| `EUREKA_PASSWORD` | Password Eureka | `password` |

### Rate Limiting (Hardcoded)

El Rate Limiting está configurado en código:
- **Límite**: 100 requests/minuto por IP
- **Algoritmo**: Token Bucket
- **Excludes**: `/actuator/*`, `/health`

Para modificar, editar: `gateway-service/src/main/java/com/invoices/gateway_service/filter/RateLimitingFilter.java`

### Circuit Breaker (Hardcoded)

Configuración en código:
- **Failure threshold**: 50%
- **Wait duration**: 60 segundos
- **Sliding window**: 10 requests
- **Min calls**: 5

Para modificar, editar: `gateway-service/src/main/java/com/invoices/gateway_service/config/ResilienceConfig.java`

---

## 📄 Document Service (Puerto 8083)

### Variables de MinIO

| Variable | Descripción | Default |
|----------|-------------|---------|
| `MINIO_ENDPOINT` | Endpoint MinIO | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | Access key | `minioadmin` |
| `MINIO_SECRET_KEY` | Secret key | `minioadmin` |
| `MINIO_BUCKET_NAME` | Nombre del bucket | `invoices` |
| `MINIO_REGION` | Región | `us-east-1` |

### Variables de Base de Datos

| Variable | Descripción | Default |
|----------|-------------|---------|
| `DOCUMENT_DB_HOST` | Host PostgreSQL | `localhost` |
| `DOCUMENT_DB_PORT` | Puerto PostgreSQL | `5432` |
| `DOCUMENT_DB_NAME` | Nombre de BD | `documentdb` |
| `DOCUMENT_DB_USERNAME` | Usuario BD | `document_service_user` |
| `DOCUMENT_DB_PASSWORD` | Password BD | `password` |

### Configurar MinIO

```bash
# Desarrollo (local)
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET_NAME=invoices-dev

# Producción (S3-compatible)
export MINIO_ENDPOINT=https://s3.amazonaws.com
export MINIO_ACCESS_KEY=AKIA...
export MINIO_SECRET_KEY=...
export MINIO_BUCKET_NAME=invoices-prod
export MINIO_REGION=us-east-1
```

---

## 🗂️ Archivos de Configuración

### .env para Docker Compose

Crear archivo `.env` en la raíz:

```bash
# Config Server
CONFIG_PROFILE=native
CONFIG_SERVER_PORT=8888

# Trace Service
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_INVOICE_DLQ_TOPIC=invoice-events-dlq
TRACE_DB_HOST=postgres-trace
TRACE_DB_PASSWORD=secure-password

# Document Service
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
DOCUMENT_DB_HOST=postgres-document
DOCUMENT_DB_PASSWORD=secure-password

# Gateway Service
JWT_SECRET=your-super-secret-jwt-key-min-32-characters-long
EUREKA_SERVER_HOST=eureka-server
EUREKA_PASSWORD=secure-password

# Logging
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
```

### .env.production para Producción

```bash
# Config Server
CONFIG_PROFILE=git
CONFIG_GIT_URI=https://github.com/tu-org/config-repo.git
CONFIG_SERVER_USERNAME=admin
CONFIG_SERVER_PASSWORD=<SECURE_PASSWORD>

# Trace Service
KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092,kafka3:9092
KAFKA_INVOICE_DLQ_TOPIC=invoice-events-dlq
TRACE_DB_HOST=prod-postgres-trace.rds.amazonaws.com
TRACE_DB_PASSWORD=<SECURE_PASSWORD>

# Document Service
MINIO_ENDPOINT=https://s3.amazonaws.com
MINIO_ACCESS_KEY=<AWS_ACCESS_KEY>
MINIO_SECRET_KEY=<AWS_SECRET_KEY>
MINIO_BUCKET_NAME=invoices-prod
DOCUMENT_DB_HOST=prod-postgres-document.rds.amazonaws.com
DOCUMENT_DB_PASSWORD=<SECURE_PASSWORD>

# Gateway Service
JWT_SECRET=<SECURE_JWT_SECRET_MIN_32_CHARS>
EUREKA_SERVER_HOST=eureka.prod.example.com
EUREKA_PASSWORD=<SECURE_PASSWORD>

# Logging
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=INFO
```

---

## 🐳 Docker Compose

Usar variables de entorno en `docker-compose.yml`:

```yaml
version: '3.8'

services:
  config-server:
    image: config-server:latest
    environment:
      - CONFIG_PROFILE=${CONFIG_PROFILE:-native}
      - LOG_LEVEL_ROOT=${LOG_LEVEL_ROOT:-INFO}
    ports:
      - "8888:8888"

  trace-service:
    image: trace-service:latest
    environment:
      - KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS}
      - KAFKA_INVOICE_DLQ_TOPIC=${KAFKA_INVOICE_DLQ_TOPIC}
      - TRACE_DB_HOST=${TRACE_DB_HOST}
      - TRACE_DB_PASSWORD=${TRACE_DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - kafka
      - postgres-trace
    ports:
      - "8084:8084"

  # ... otros servicios
```

---

## ✅ Validación de Variables

### Script de Validación

Crear `validate-env.sh`:

```bash
#!/bin/bash

echo "🔍 Validando variables de entorno..."

# Validar JWT Secret (min 32 caracteres)
if [ ${#JWT_SECRET} -lt 32 ]; then
    echo "❌ ERROR: JWT_SECRET debe tener al menos 32 caracteres"
    exit 1
fi

# Validar que DLQ topic esté configurado
if [ -z "$KAFKA_INVOICE_DLQ_TOPIC" ]; then
    echo "⚠️  WARNING: KAFKA_INVOICE_DLQ_TOPIC no está configurado"
fi

# Validar Config Profile
if [[ ! "$CONFIG_PROFILE" =~ ^(dev|test|prod|native|git)$ ]]; then
    echo "⚠️  WARNING: CONFIG_PROFILE tiene valor inválido: $CONFIG_PROFILE"
fi

echo "✅ Validación completada"
```

```bash
chmod +x validate-env.sh
./validate-env.sh
```

---

## 🔐 Mejores Prácticas

1. **NUNCA** commitear `.env` al repositorio
2. **Usar** `.env.example` como template
3. **Rotar** secrets regularmente en producción
4. **Usar** gestores de secrets (AWS Secrets Manager, HashiCorp Vault)
5. **Validar** variables antes de deployment
6. **Documentar** cambios en este archivo

---

## 📚 Referencias

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Docker Environment Variables](https://docs.docker.com/compose/environment-variables/)
- [12 Factor App: Config](https://12factor.net/config)

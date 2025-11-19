# Invoices Backend - Monolith

Sistema monolítico de gestión de facturas con Spring Boot 3.4 + Java 21

---

## 🚀 Deploy Rápido (MacBook M1 Pro)

### Paso 1: Build Local (2-3 min)

```bash
./build-local-fast.sh
```

### Paso 2: Deploy a Fly.io (1-2 min)

```bash
cd invoices-monolith
fly deploy --dockerfile Dockerfile.prebuilt
```

**Total: 3-5 minutos** ⚡

---

## ⚙️ Configuración Inicial

### 1. Instalar Fly CLI

```bash
brew install flyctl
fly auth login
```

### 2. Configurar Secrets

```bash
# Usar script automático
./configure-secrets.sh

# O manualmente:
fly secrets set SPRING_DATASOURCE_URL="jdbc:postgresql://..."
fly secrets set DB_USERNAME="usuario"
fly secrets set DB_PASSWORD="password"
fly secrets set JWT_SECRET="$(openssl rand -base64 32)"
fly secrets set REDIS_HOST="tu-redis.upstash.io"
fly secrets set REDIS_PORT="6379"
fly secrets set REDIS_PASSWORD="tu-password"
fly secrets set REDIS_SSL="true"
fly secrets set S3_ENDPOINT="https://..."
fly secrets set S3_ACCESS_KEY="key"
fly secrets set S3_SECRET_KEY="secret"
fly secrets set S3_BUCKET_NAME="invoices-documents"
```

---

## 🆓 Servicios Externos (Gratis)

| Servicio | Proveedor | Free Tier |
|----------|-----------|-----------|
| **PostgreSQL** | [Neon](https://neon.tech) | 512MB |
| **Redis** | [Upstash](https://upstash.com) | 10K cmd/día |
| **Storage** | [Cloudflare R2](https://cloudflare.com/r2) | 10GB |
| **Hosting** | [Fly.io](https://fly.io) | 3 VMs |

---

## 📊 Monitoreo

```bash
# Logs en tiempo real
fly logs -a invoices-monolith

# Status
fly status -a invoices-monolith

# Abrir app
fly open -a invoices-monolith

# SSH
fly ssh console -a invoices-monolith
```

---

## 🛠️ Desarrollo Local

### Build

```bash
./build-local-fast.sh
```

### Tests

```bash
cd invoices-monolith
mvn test
```

### Ejecutar

```bash
cd invoices-monolith
java -jar target/invoices-monolith-1.0.0.jar
```

---

## 📁 Estructura

```
invoices-monolith/
├── src/main/java/com/invoices/
│   ├── user/          # Usuarios y auth
│   ├── invoice/       # Facturas
│   ├── document/      # PDFs
│   ├── trace/         # Auditoría
│   └── security/      # JWT
└── fly.toml          # Config Fly.io
```

---

## 🔧 Stack

- Java 21 + Spring Boot 3.4
- PostgreSQL (Neon)
- Redis (Upstash)
- Cloudflare R2 (S3)
- JasperReports
- JWT + Spring Security

---

## 🍎 Optimizado M1 Pro

- Build paralelo (16 threads)
- 2GB RAM para JVM
- Quick compilation
- Docker build en cloud (ARM64 → AMD64)

---

## 📝 Scripts

```bash
./build-local-fast.sh       # Build optimizado
./configure-secrets.sh       # Config secrets
./deploy-macos.sh           # Deploy completo
./quick-deploy.sh           # Deploy rápido
```

---

## 🌐 URLs

- **App**: https://invoices-monolith.fly.dev
- **Swagger**: https://invoices-monolith.fly.dev/swagger-ui.html
- **Health**: https://invoices-monolith.fly.dev/actuator/health

---

## ❓ Troubleshooting

### Build falla

```bash
java -version  # Verificar Java 21
mvn clean      # Limpiar caché
./build-local-fast.sh
```

### Deploy falla

```bash
fly auth whoami              # Verificar auth
fly logs -a invoices-monolith # Ver logs
fly secrets list             # Verificar secrets
```

---

## 📚 Docs

- [Fly.io](https://fly.io/docs/)
- [Spring Boot](https://docs.spring.io/spring-boot/)

---

**README antiguo completo**: `README.old.md`

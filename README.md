# Invoices Backend - Monolith

Sistema monolítico de gestión de facturas con Spring Boot 3.4 + Java 21

Optimizado para **MacBook M1 Pro** y **Railway**

---

## 🚂 Deploy a Railway (2 Opciones)

### Opción 1: Desde GitHub (Recomendado - Más Fácil)

1. **Push tu código a GitHub**
   ```bash
   git push origin main
   ```

2. **Crear proyecto en Railway**
   - Ve a [railway.app](https://railway.app)
   - Click **+ New Project** → **Deploy from GitHub repo**
   - Selecciona `invoices-back`
   - Railway detecta el `Dockerfile` automáticamente
   - Deploy completo en 3-5 minutos ⚡

3. **Configurar Variables** (ver sección abajo)

---

### Opción 2: Desde CLI (Para Desarrollo)

```bash
# 1. Instalar Railway CLI
bash <(curl -fsSL https://railway.app/install.sh)

# 2. Login
railway login

# 3. Build local (opcional pero más rápido)
./build-local-fast.sh

# 4. Deploy
./deploy-railway.sh
```

---

## ⚙️ Configurar Variables en Railway

### Variables Esenciales

En Railway → Tu Proyecto → Variables, añade:

```bash
# Spring Boot
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://<HOST>:<PORT>/<DB>
SPRING_DATASOURCE_USERNAME=usuario
SPRING_DATASOURCE_PASSWORD=password

# JWT (generar con: openssl rand -base64 32)
JWT_SECRET=tu-secreto-de-32-caracteres-minimo

# Redis (Upstash o Railway addon)
REDIS_HOST=tu-redis.upstash.io
REDIS_PORT=6379
REDIS_PASSWORD=tu-password
REDIS_SSL=true

# S3/R2 (Cloudflare R2)
S3_ENDPOINT=https://tu-cuenta.r2.cloudflarestorage.com
S3_ACCESS_KEY=tu-access-key
S3_SECRET_KEY=tu-secret-key
S3_BUCKET_NAME=invoices-documents

# JVM (Railway optimizado)
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError
```

### Desde CLI

```bash
railway variables set SPRING_DATASOURCE_URL="jdbc:postgresql://..."
railway variables set JWT_SECRET="$(openssl rand -base64 32)"
railway variables set SPRING_PROFILES_ACTIVE="prod"
# ... etc
```

---

## 🗄️ Base de Datos en Railway

### Opción A: PostgreSQL de Railway (Más Fácil)

1. En Railway → **+ New** → **Database** → **PostgreSQL**
2. Railway auto-configura las variables `DATABASE_URL`
3. Copia y adapta a formato JDBC:
   ```bash
   # Railway te da:
   DATABASE_URL=postgresql://user:pass@host:5432/railway

   # Convierte a:
   SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/railway
   SPRING_DATASOURCE_USERNAME=user
   SPRING_DATASOURCE_PASSWORD=pass
   ```

### Opción B: Base de Datos Externa (Neon, etc.)

Usa las mismas variables que antes.

---

## 📊 Monitoreo

```bash
# Logs en tiempo real
railway logs

# Status
railway status

# Abrir app en navegador
railway open

# Shell SSH
railway run bash

# Ver variables
railway variables
```

---

## 🆓 Servicios Recomendados

| Servicio | Proveedor | Free Tier | Nota |
|----------|-----------|-----------|------|
| **Hosting** | [Railway](https://railway.app) | $5/mes gratis | FÁCIL ✅ |
| **PostgreSQL** | Railway addon | Incluido | O [Neon](https://neon.tech) 512MB |
| **Redis** | [Upstash](https://upstash.com) | 10K cmd/día | |
| **Storage** | [Cloudflare R2](https://cloudflare.com/r2) | 10GB | |

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
invoices-back/
├── invoices-monolith/
│   ├── src/main/java/com/invoices/
│   │   ├── user/          # Usuarios y auth
│   │   ├── invoice/       # Facturas
│   │   ├── document/      # PDFs
│   │   ├── trace/         # Auditoría
│   │   └── security/      # JWT
│   └── Dockerfile         # Railway build
├── railway.json           # Config Railway
└── deploy-railway.sh      # Script deploy
```

---

## 🔧 Stack

- Java 21 + Spring Boot 3.4
- PostgreSQL (Railway o Neon)
- Redis (Upstash)
- Cloudflare R2 (S3)
- JasperReports
- JWT + Spring Security

---

## 🍎 Optimizado M1 Pro

- Build paralelo (16 threads)
- 2GB RAM para JVM local
- Docker multi-stage optimizado
- Railway lee PORT dinámico
- MaxRAMPercentage 70% en producción

---

## 📝 Scripts

```bash
./build-local-fast.sh       # Build local optimizado (2-3 min)
./deploy-railway.sh         # Deploy completo a Railway
./configure-secrets.sh      # Config secrets (legacy Fly.io)
```

---

## 🌐 URLs

Después del deploy, Railway te da:

- **App**: `https://tu-proyecto.up.railway.app`
- **Swagger**: `https://tu-proyecto.up.railway.app/swagger-ui.html`
- **Health**: `https://tu-proyecto.up.railway.app/actuator/health`

---

## ❓ Troubleshooting

### Build falla en Railway

```bash
# Ver logs en Railway dashboard o:
railway logs

# Verificar que Dockerfile esté en la ruta correcta
# railway.json → "dockerfilePath": "invoices-monolith/Dockerfile"
```

### App no arranca

```bash
# Verificar variables
railway variables

# Verificar que PORT se lee correctamente
# El Dockerfile usa: --server.port=${PORT:-8080}

# Ver logs
railway logs
```

### Build local falla

```bash
java -version  # Debe ser Java 21
mvn clean      # Limpiar caché
./build-local-fast.sh
```

### Conexión a DB falla

```bash
# Verificar formato JDBC correcto:
SPRING_DATASOURCE_URL=jdbc:postgresql://host:port/dbname

# NO usar el formato postgresql:// directo
# Railway da: postgresql://...
# Tú necesitas: jdbc:postgresql://...
```

---

## 🚀 Flujo Completo de Deploy

```bash
# 1. Build local
./build-local-fast.sh

# 2. Commit y push
git add -A
git commit -m "Ready for Railway deployment"
git push origin main

# 3. Deploy desde Railway UI
# O desde CLI:
railway login
railway link  # Primera vez
railway up    # Deploy
```

---

## 📚 Docs

- [Railway](https://docs.railway.app/)
- [Spring Boot](https://docs.spring.io/spring-boot/)
- [Railway CLI](https://docs.railway.app/develop/cli)

---

## 📌 Notas Importantes

- ✅ Railway lee `PORT` dinámico (configurado en Dockerfile)
- ✅ `railway.json` define ruta del Dockerfile
- ✅ Health check en `/actuator/health`
- ✅ Build optimizado para M1 Pro (16 threads)
- ⚠️ Después de $5 gratis, Railway cobra ~$5-10/mes
- 📝 README antiguo completo: `README.old.md`

---

**¿Problemas?** Revisa los logs: `railway logs`

# Invoices Backend - Monolith

Sistema monolítico de gestión de facturas con Spring Boot 3.4 + Java 21

Optimizado para **MacBook M1 Pro** y **Railway**

---

## 🚂 Deploy a Railway - Desde Web (Más Fácil)

### Paso 1: Push a GitHub

```bash
# Asegúrate de estar en main o merge tu branch
git checkout main
git merge claude/deploy-macos-backend-016XiNmf71TfQd2xwFzLMCds  # Si trabajas en branch
git push origin main
```

### Paso 2: Crear Proyecto en Railway

1. Ve a **[railway.app](https://railway.app)**
2. Click **+ New Project**
3. Selecciona **Deploy from GitHub repo**
4. Autoriza Railway a acceder a GitHub (primera vez)
5. Selecciona el repositorio **`jefmonjor/invoices-back`**
6. Railway detecta automáticamente:
   - ✅ `railway.json` (configuración)
   - ✅ `Dockerfile` en raíz del repo
   - ✅ Puerto dinámico (Railway inyecta `$PORT`)

### Paso 3: Configurar Variables de Entorno

En Railway → Tu Proyecto → **Variables** tab:

**Copia y pega este bloque completo:**

```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://ep-delicate-snow-abyzqltv-pooler.eu-west-2.aws.neon.tech:5432/neondb?sslmode=require
SPRING_DATASOURCE_USERNAME=neondb_owner
SPRING_DATASOURCE_PASSWORD=npg_02GsdHFqhfoU
JWT_EXPIRATION_MS=3600000
JWT_ISSUER=invoices-backend-prod
REDIS_HOST=subtle-parrot-38179.upstash.io
REDIS_PORT=6379
REDIS_PASSWORD=ApUjAAIgcDI37a9MyM6T1LPJbUI4964n8CwccbGkioWuVe2WQwrM6A
REDIS_SSL=true
S3_ENDPOINT=https://ac29c1ccf8f12dc453bdec1c87ddcffb.r2.cloudflarestorage.com
S3_ACCESS_KEY=6534534b1dfc4ae849e1d01f952cd06c
S3_SECRET_KEY=5bc3d93666a9fec20955fefa01b51c1d85f2b4e044233426b52dbaf7f514f246
S3_BUCKET_NAME=invoices-documents
S3_REGION=auto
S3_PATH_STYLE_ACCESS=true
CORS_ALLOWED_ORIGINS=https://invoices-frontend-vert.vercel.app,http://localhost:3000,http://localhost:5173
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError
```

**Generar JWT_SECRET único:**
```bash
# En tu Mac, ejecuta:
openssl rand -base64 32

# Luego añade en Railway:
JWT_SECRET=<el-valor-generado>
```

### Paso 4: Deploy Automático

Railway inicia el build automáticamente después de configurar variables:
- 🔨 Build con Docker (5-8 minutos)
- 🚀 Deploy automático
- ✅ Health check en `/actuator/health`
- 🌐 URL pública generada: `https://tu-proyecto.up.railway.app`

### Paso 5: Verificar Deployment

```bash
# Ver logs en Railway UI o:
# 1. Click en tu proyecto
# 2. Tab "Deployments"
# 3. Ver logs en tiempo real
```

---

### Alternativa: Deploy desde CLI (Desarrollo)

<details>
<summary>Click para ver instrucciones CLI (opcional)</summary>

```bash
# 1. Configurar variables automáticamente
./configure-railway-auto.sh

# 2. Deploy
railway up
```

</details>

---

## ✅ Servicios Pre-Configurados

Tu proyecto ya tiene credenciales para:
- **PostgreSQL**: Neon (`neondb` - EU West 2)
- **Redis**: Upstash (`subtle-parrot-38179`)
- **Storage**: Cloudflare R2 (`invoices-documents`)

Solo necesitas copiar las variables en Railway UI (Paso 3 arriba).

---

## 🗄️ Base de Datos - Neon PostgreSQL

### ✅ Ya Configurada

Tu proyecto usa **Neon PostgreSQL** (serverless) con:
- **Host**: `ep-delicate-snow-abyzqltv-pooler.eu-west-2.aws.neon.tech`
- **Database**: `neondb`
- **Region**: EU West 2 (London)
- **Connection**: Pooled (mejor rendimiento)
- **SSL**: Habilitado

El script `configure-railway-auto.sh` configura automáticamente la conexión JDBC:
```
jdbc:postgresql://ep-delicate-snow-abyzqltv-pooler.eu-west-2.aws.neon.tech:5432/neondb?sslmode=require
```

### Acceso a la Base de Datos

Ve a [console.neon.tech](https://console.neon.tech) para:
- Ver tablas y datos
- Ejecutar queries SQL
- Monitorear uso
- Gestionar branches

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

## 🆓 Servicios Configurados

| Servicio | Proveedor | Free Tier | Estado |
|----------|-----------|-----------|--------|
| **Hosting** | [Railway](https://railway.app) | $5/mes gratis | ⚙️ Configurar |
| **PostgreSQL** | [Neon](https://neon.tech) | 512MB | ✅ Ya configurado |
| **Redis** | [Upstash](https://upstash.com) | 10K cmd/día | ✅ Ya configurado |
| **Storage** | [Cloudflare R2](https://cloudflare.com/r2) | 10GB | ✅ Ya configurado |

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
- PostgreSQL (Neon - Serverless)
- Redis (Upstash)
- Cloudflare R2 (S3)
- JasperReports
- JWT + Spring Security
- Railway (Hosting)

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
./build-local-fast.sh          # Build local optimizado (2-3 min)
./deploy-railway.sh            # Deploy completo a Railway
./configure-railway-neon.sh    # Configurar variables de Neon en Railway
./configure-secrets.sh         # Config secrets (legacy Fly.io)
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
# railway.json → "dockerfilePath": "Dockerfile"
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

### Conexión a Neon falla

```bash
# Verificar formato JDBC correcto:
SPRING_DATASOURCE_URL=jdbc:postgresql://ep-xxx.us-east-2.aws.neon.tech:5432/neondb?sslmode=require

# IMPORTANTE:
# 1. Añade el prefijo "jdbc:" antes de "postgresql://"
# 2. Incluye "?sslmode=require" al final
# 3. Usa el puerto 5432
# 4. Para mejor rendimiento usa pooled connection:
#    ep-xxx-pooler.us-east-2.aws.neon.tech

# Verificar en Railway:
railway variables | grep DATASOURCE
```

---

## 🚀 Flujo Completo de Deploy (Web UI)

### Deployment Inicial

1. **Push código** (si tienes cambios pendientes)
   ```bash
   git add -A
   git commit -m "Ready for Railway"
   git push origin main
   ```

2. **Railway UI**
   - Ve a [railway.app](https://railway.app)
   - + New Project → Deploy from GitHub
   - Selecciona `jefmonjor/invoices-back`

3. **Configurar Variables**
   - Tab "Variables"
   - Copia el bloque del **Paso 3** (arriba)
   - Pega las 17 variables
   - Añade `JWT_SECRET` generado con `openssl rand -base64 32`

4. **Ver Deploy**
   - Tab "Deployments" → Ver logs en tiempo real
   - Espera 5-8 minutos
   - URL generada: `https://tu-proyecto.up.railway.app`

### Deployments Posteriores (Automáticos)

Cada `git push origin main` → Railway redeploy automáticamente ✅

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

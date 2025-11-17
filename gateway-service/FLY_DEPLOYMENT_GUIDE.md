# 🚀 Guía de Deployment del Gateway a Fly.io

## 📋 Prerequisitos

Antes de empezar, asegúrate de tener:

- ✅ Fly CLI instalado y en el PATH
- ✅ Autenticado en Fly.io (`fly auth login`)
- ✅ Archivo `.env.production` en el directorio raíz con todas las configuraciones

---

## 🎯 Deployment Rápido

### Opción 1: Script Automático (Recomendado)

```bash
cd gateway-service
./deploy-to-flyio.sh
```

Este script:
1. Verifica prerequisitos
2. Crea la app en Fly.io (si no existe)
3. Configura todas las variables de entorno
4. Despliega la aplicación
5. Verifica que esté funcionando

---

### Opción 2: Manual (Paso a Paso)

#### 1. Verificar que Fly CLI funciona

```bash
# Agregar al PATH si es necesario (macOS)
export PATH="/Users/Jefferson/.fly/bin:$PATH"

# Verificar
fly version

# Autenticar
fly auth login
```

#### 2. Crear la app en Fly.io

```bash
cd gateway-service

fly launch \
    --name invoices-backend \
    --region mad \
    --no-deploy \
    --yes
```

**Regiones disponibles:**
- `mad` - Madrid, Spain (Europa)
- `mia` - Miami, USA (América)
- `gru` - Sao Paulo, Brazil (América Latina)

#### 3. Configurar variables de entorno

```bash
# Cargar desde .env.production
source ../.env.production

# JWT y CORS
fly secrets set \
    SPRING_PROFILES_ACTIVE=prod \
    JWT_SECRET="$JWT_SECRET" \
    JWT_ISSUER=invoices-backend \
    JWT_EXPIRATION_MS=3600000 \
    CORS_ALLOWED_ORIGINS="$CORS_ALLOWED_ORIGINS" \
    -a invoices-backend

# Database (Neon)
fly secrets set \
    SPRING_DATASOURCE_URL="$USER_DB_URL" \
    -a invoices-backend

# Deshabilitar Eureka (arquitectura simplificada)
fly secrets set \
    EUREKA_CLIENT_ENABLED=false \
    -a invoices-backend
```

#### 4. Desplegar

```bash
fly deploy -a invoices-backend
```

Esto tomará **3-5 minutos**. Verás:

```
==> Building image
--> Building Dockerfile
--> Pushing image to registry
==> Deploying
--> v1 deployed successfully
✓ 1/1 machines successfully started
```

#### 5. Verificar

```bash
# Ver estado
fly status -a invoices-backend

# Ver logs
fly logs -a invoices-backend

# Test health check
curl https://invoices-backend.fly.dev/actuator/health

# Respuesta esperada:
# {"status":"UP"}
```

---

## 🐛 Troubleshooting

### Error: "command not found: fly"

**Causa:** Fly CLI no está en el PATH

**Solución:**

```bash
# macOS/Linux - Temporal
export PATH="/Users/Jefferson/.fly/bin:$PATH"

# macOS - Permanente
echo 'export PATH="/Users/Jefferson/.fly/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

---

### Error: "failed to fetch an image or build from source"

**Causa:** Problema con el build de Docker

**Solución:**

```bash
# Ver logs detallados
fly deploy --verbose

# Verificar que el Dockerfile existe
ls -la Dockerfile

# Build local para debugging
docker build -t gateway-test .
```

**Posibles causas:**
- Maven dependency download fallando
- Timeout en build (el build tarda >10 minutos)

**Fix:** Aumentar timeout en fly.toml:

```toml
[build]
  dockerfile = "Dockerfile"

[build.args]
  MAVEN_OPTS = "-Dmaven.repo.local=.m2/repository"
```

---

### Error: "Error: no organization specified"

**Causa:** No has creado una organización personal en Fly.io

**Solución:**

```bash
# Login nuevamente
fly auth login

# Crear organización personal
fly orgs create personal

# Retry deployment
fly deploy
```

---

### Error: "health checks failing"

**Causa:** La aplicación tarda en iniciar o hay un error en runtime

**Solución:**

```bash
# Ver logs en tiempo real
fly logs -a invoices-backend

# Ver logs de health checks específicamente
fly logs -a invoices-backend | grep health

# Conectar via SSH para debugging
fly ssh console -a invoices-backend
```

**Errores comunes en logs:**

#### 1. Error de conexión a base de datos

```
Caused by: org.postgresql.util.PSQLException: Connection refused
```

**Fix:** Verificar que DATABASE_URL esté correctamente configurado:

```bash
fly secrets list -a invoices-backend

# Si está mal, actualizar:
fly secrets set SPRING_DATASOURCE_URL="postgresql://..." -a invoices-backend
```

#### 2. Error de Eureka (Service Discovery)

```
com.netflix.discovery.shared.transport.TransportException: Cannot execute request on any known server
```

**Fix:** Deshabilitar Eureka para deployment simplificado:

```bash
fly secrets set EUREKA_CLIENT_ENABLED=false -a invoices-backend
```

O agregar en `application.yml`:

```yaml
eureka:
  client:
    enabled: ${EUREKA_CLIENT_ENABLED:false}
```

#### 3. Out of Memory

```
java.lang.OutOfMemoryError: Java heap space
```

**Fix:** Aumentar memoria en fly.toml:

```toml
[[vm]]
  size = "shared-cpu-1x"
  memory = "512mb"  # Cambiar a 512mb o 1024mb
```

---

### La app se despliega pero no responde

**Causa:** Puerto incorrecto o aplicación no se inicia

**Debugging:**

```bash
# Ver logs completos
fly logs -a invoices-backend

# Conectar via SSH
fly ssh console -a invoices-backend

# Dentro de SSH:
# Ver procesos
ps aux

# Ver puerto en uso
netstat -tulpn | grep 8080

# Ver variables de entorno
env | grep SPRING
```

**Fix común:** Asegurar que `SERVER_PORT=8080` en fly.toml:

```toml
[env]
  SPRING_PROFILES_ACTIVE = "prod"
  SERVER_PORT = "8080"
```

---

### Build muy lento (>10 minutos)

**Causa:** Maven descargando dependencies en cada build

**Fix:** Optimizar Dockerfile con cache:

```dockerfile
# Agregar después de COPY pom.xml:
RUN mvn dependency:go-offline -B --no-transfer-progress
```

Ya está en el Dockerfile actual ✅

---

## 📊 Verificación Post-Deployment

### Checklist

- [ ] App desplegada: `fly status` muestra "started"
- [ ] Health check OK: `curl https://invoices-backend.fly.dev/actuator/health`
- [ ] Logs sin errores: `fly logs`
- [ ] CORS configurado: `fly secrets list | grep CORS`
- [ ] JWT configurado: `fly secrets list | grep JWT`
- [ ] Database conectada: verificar en logs que Flyway corre

### Tests desde terminal

```bash
# 1. Health check
curl https://invoices-backend.fly.dev/actuator/health

# Esperado: {"status":"UP"}

# 2. Test de CORS (desde otro dominio)
curl -H "Origin: https://invoices-frontend-vert.vercel.app" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS \
     https://invoices-backend.fly.dev/api/auth/login \
     -v

# Esperado: Headers con Access-Control-Allow-Origin

# 3. Test de login (si tienes usuarios)
curl -X POST https://invoices-backend.fly.dev/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'

# Esperado: {"token":"..."}
```

---

## 🔧 Comandos Útiles

```bash
# Ver todas las apps
fly apps list

# Ver info de la app
fly info -a invoices-backend

# Ver secrets configurados
fly secrets list -a invoices-backend

# Actualizar un secret
fly secrets set KEY=value -a invoices-backend

# Ver uso de recursos
fly status -a invoices-backend

# Escalar (cambiar tamaño VM)
fly scale vm shared-cpu-1x --memory 512 -a invoices-backend

# Abrir dashboard web
fly dashboard -a invoices-backend

# Destruir app (¡CUIDADO!)
fly apps destroy invoices-backend
```

---

## 📈 Monitoreo

### Ver métricas

```bash
# Dashboard web
fly dashboard -a invoices-backend

# En el dashboard verás:
- CPU usage
- Memory usage
- Request rate
- Response times
```

### Logs en tiempo real

```bash
fly logs -a invoices-backend

# Filtrar por nivel
fly logs -a invoices-backend | grep ERROR
fly logs -a invoices-backend | grep WARN

# Desde una hora específica
fly logs -a invoices-backend --since 1h
```

---

## 🎯 Resultado Esperado

Una vez completado el deployment:

```
✅ URL Backend: https://invoices-backend.fly.dev
✅ Health: https://invoices-backend.fly.dev/actuator/health → {"status":"UP"}
✅ Region: Madrid (mad) o Miami (mia)
✅ HTTPS: Automático
✅ Costo: $0.00/mes (free tier)
```

---

## 📝 Siguiente Paso

Configurar Vercel para que apunte al backend:

```bash
Variable: VITE_API_BASE_URL
Valor: https://invoices-backend.fly.dev/api
```

Ver: [DEPLOYMENT_VERCEL_GUIDE.md](../DEPLOYMENT_VERCEL_GUIDE.md) para instrucciones completas.

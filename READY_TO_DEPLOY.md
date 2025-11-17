# ✅ Todo Listo para Deployment

Tu backend está **100% preparado** para ser desplegado a Fly.io y conectado con Vercel.

---

## 📦 Lo que se ha configurado

### ✅ Archivos Creados

```
invoices-back/
├── .env.production                    ← Variables de entorno (ya creado en tu máquina local)
├── gateway-service/
│   ├── fly.toml                      ← Configuración de Fly.io
│   ├── deploy-to-flyio.sh            ← Script automático de deployment
│   ├── FLY_DEPLOYMENT_GUIDE.md       ← Guía completa de deployment
│   └── src/main/resources/
│       └── application.yml           ← CORS y Eureka configurados
├── DEPLOYMENT_VERCEL_GUIDE.md        ← Guía general
├── QUICKSTART_PRODUCTION.md          ← Quick start
└── NEON_DATABASE_SETUP.md            ← Guía de Neon DB
```

### ✅ Configuraciones Aplicadas

- **CORS**: Configurado para `https://invoices-frontend-vert.vercel.app`
- **JWT Secret**: Generado y configurado
- **4 Bases de datos Neon**: Configuradas y listas
- **Eureka**: Configurable via `EUREKA_CLIENT_ENABLED=false`
- **Dockerfile**: Optimizado para Spring Boot 21
- **Health Checks**: Configurados en `/actuator/health`

---

## 🚀 DEPLOYMENT EN 3 PASOS

### Paso 1: Configurar Fly CLI (ya lo tienes)

Ya ejecutaste esto en tu máquina:

```bash
export PATH="/Users/Jefferson/.fly/bin:$PATH"
fly auth login  # Ya logueado ✅
```

---

### Paso 2: Desplegar el Gateway

**Opción A: Script Automático (RECOMENDADO)**

Desde tu máquina local, en el directorio `proyecto`:

```bash
cd invoices-back/gateway-service
./deploy-to-flyio.sh
```

Este script:
1. Verifica prerequisitos
2. Crea la app `invoices-backend` en Fly.io
3. Configura todas las variables de entorno
4. Despliega la aplicación (build + deploy)
5. Verifica que funcione

**Tiempo estimado**: 3-5 minutos

---

**Opción B: Manual (si prefieres control total)**

```bash
cd invoices-back/gateway-service

# 1. Crear app
fly launch --name invoices-backend --region mad --no-deploy --yes

# 2. Configurar secrets
source ../.env.production

fly secrets set \
    SPRING_PROFILES_ACTIVE=prod \
    JWT_SECRET="$JWT_SECRET" \
    CORS_ALLOWED_ORIGINS="$CORS_ALLOWED_ORIGINS" \
    EUREKA_CLIENT_ENABLED=false \
    -a invoices-backend

# 3. Desplegar
fly deploy -a invoices-backend

# 4. Verificar
fly status -a invoices-backend
curl https://invoices-backend.fly.dev/actuator/health
```

---

### Paso 3: Configurar Vercel

1. **Ir a Vercel**: https://vercel.com/dashboard
2. **Seleccionar proyecto**: `invoices-frontend`
3. **Settings → Environment Variables**
4. **Add new variable**:
   ```
   Name:  VITE_API_BASE_URL
   Value: https://invoices-backend.fly.dev/api

   ✅ Production
   ✅ Preview
   ✅ Development
   ```
5. **Save**
6. **Deployments → Latest → Redeploy**

---

## ✅ Verificación Final

### 1. Backend funcionando

```bash
# Health check
curl https://invoices-backend.fly.dev/actuator/health

# Esperado: {"status":"UP"}
```

### 2. CORS funcionando

Desde tu frontend en Vercel (DevTools → Console):

```javascript
fetch('https://invoices-backend.fly.dev/actuator/health')
  .then(res => res.json())
  .then(data => console.log('✅ Backend conectado:', data))
```

### 3. Login funcionando

Intenta hacer login desde tu app:
- Abre: https://invoices-frontend-vert.vercel.app
- Login con credenciales
- ✅ Si funciona: **¡LISTO!**

---

## 🎯 Resultado Final

Una vez completado:

```
✅ Frontend:   https://invoices-frontend-vert.vercel.app
✅ Backend:    https://invoices-backend.fly.dev
✅ Database:   Neon PostgreSQL (4 databases)
✅ Region:     Madrid, Spain (mad)
✅ HTTPS:      Automático
✅ CORS:       Configurado
✅ JWT:        Configurado
✅ Costo:      $0.00/mes 🎊
```

---

## 🐛 Si algo falla

### Ver logs en tiempo real

```bash
fly logs -a invoices-backend
```

### Errores comunes y soluciones

**1. Error: "health checks failing"**

```bash
# Ver logs
fly logs -a invoices-backend | grep ERROR

# Posibles causas:
# - Database URL incorrecta
# - Eureka no deshabilitado
# - Out of memory
```

**Solución**: Ver `gateway-service/FLY_DEPLOYMENT_GUIDE.md` sección Troubleshooting

---

**2. Error: "no organization specified"**

```bash
fly orgs create personal
fly deploy
```

---

**3. Error CORS en frontend**

Verificar que la URL en Fly secrets sea exacta:

```bash
fly secrets list -a invoices-backend | grep CORS

# Si está mal:
fly secrets set CORS_ALLOWED_ORIGINS=https://invoices-frontend-vert.vercel.app
```

---

## 📚 Documentación Adicional

- **Guía completa de Fly.io**: `gateway-service/FLY_DEPLOYMENT_GUIDE.md`
- **Troubleshooting detallado**: Ver sección Troubleshooting en la guía
- **Comandos útiles**: `fly logs`, `fly status`, `fly dashboard`

---

## 🔧 Comandos Post-Deployment

```bash
# Ver estado
fly status -a invoices-backend

# Ver logs en tiempo real
fly logs -a invoices-backend

# Abrir dashboard
fly dashboard -a invoices-backend

# Actualizar secrets
fly secrets set KEY=value -a invoices-backend

# Redeploy (si haces cambios)
cd gateway-service
fly deploy -a invoices-backend
```

---

## 🎯 Siguiente Paso

**EJECUTA ESTO AHORA EN TU TERMINAL:**

```bash
cd /Users/Jefferson/Documents/proyecto/invoices-back/gateway-service
./deploy-to-flyio.sh
```

Y luego avísame qué sucede. Si hay algún error, copia el output completo para ayudarte a resolverlo.

---

## ⚠️ Nota Importante: Arquitectura Simplificada

Este deployment inicial es **solo el Gateway Service** sin los otros microservicios (user-service, invoice-service, etc.).

**¿Por qué?**
- Free tier de Fly.io: 3 VMs (usamos 1 para gateway)
- Deployment simplificado para verificar que todo funcione
- Puedes agregar más servicios después

**Para agregar más servicios después:**

```bash
# Desplegar user-service
cd ../user-service
fly launch --name invoices-user-service
fly deploy

# Desplegar invoice-service
cd ../invoice-service
fly launch --name invoices-invoice-service
fly deploy
```

Pero primero asegúrate de que el gateway funcione correctamente.

---

**¿Listo para desplegar? Ejecuta el script y avísame qué pasa! 🚀**

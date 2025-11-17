# ⚡ Quick Start - Deployment a Producción

Guía rápida para desplegar el backend y conectarlo con Vercel en **menos de 15 minutos**.

---

## 🎯 Lo que vamos a hacer

1. ✅ Crear base de datos PostgreSQL gratis en Neon
2. ✅ Desplegar backend en Fly.io (gratis)
3. ✅ Configurar CORS para Vercel
4. ✅ Conectar frontend con backend
5. ✅ ¡Listo! 🎉

---

## 📋 Prerequisitos

```bash
# Solo necesitas tener instalado:
- Git
- Fly.io CLI (se instala en el proceso)
```

---

## 🚀 Paso a Paso (15 minutos)

### 1. Crear Base de Datos (3 minutos)

**a) Crear cuenta en Neon (PostgreSQL gratis)**

👉 **https://neon.tech** (login con GitHub)

**b) Crear proyecto y base de datos**

```sql
-- En Neon SQL Editor:
CREATE DATABASE userdb;
CREATE DATABASE invoicedb;
```

**c) Copiar Connection String**

```
Settings → Connection String → Copy

Ejemplo:
postgres://user:pass@ep-cool-name.us-east-2.aws.neon.tech/userdb?sslmode=require
```

📝 **Guardar estas URLs** - las necesitarás en el siguiente paso.

---

### 2. Desplegar Backend en Fly.io (8 minutos)

**a) Instalar Fly.io CLI**

```bash
# macOS/Linux
curl -L https://fly.io/install.sh | sh

# Windows (PowerShell)
powershell -Command "iwr https://fly.io/install.ps1 -useb | iex"
```

**b) Login en Fly.io**

```bash
fly auth signup  # Crear cuenta (gratis, con GitHub)
fly auth login   # O login si ya tienes cuenta
```

**c) Configurar variables de entorno**

```bash
# Copiar el ejemplo
cp .env.production.example .env.production

# Editar con tus valores
nano .env.production  # o usar tu editor favorito
```

**Valores mínimos requeridos:**

```bash
# .env.production
CORS_ALLOWED_ORIGINS=https://tu-app.vercel.app
JWT_SECRET=<genera-con-openssl-rand-base64-32>
USER_DB_URL=<tu-connection-string-de-neon>
INVOICE_DB_URL=<tu-connection-string-de-neon>
```

**d) Desplegar con script automático**

```bash
# Desplegar solo el gateway (recomendado para empezar)
./deploy-flyio.sh gateway

# O desplegar todos los servicios
./deploy-flyio.sh all
```

**e) Obtener URL del backend**

```bash
fly status

# URL será algo como:
# https://invoices-backend.fly.dev
```

---

### 3. Configurar Frontend en Vercel (2 minutos)

**a) Ir a tu proyecto en Vercel**

👉 **https://vercel.com/dashboard**

**b) Configurar variable de entorno**

```
Settings → Environment Variables → Add

Name:  VITE_API_BASE_URL
Value: https://invoices-backend.fly.dev/api

☑️ Production
☑️ Preview
☑️ Development
```

**c) Redeploy frontend**

```
Deployments → Latest → Redeploy
```

---

### 4. Verificar que funciona (2 minutos)

**a) Abrir tu app en Vercel**

```
https://tu-app.vercel.app
```

**b) Test en DevTools (F12)**

```javascript
// Copiar y pegar en la consola:
fetch('https://invoices-backend.fly.dev/actuator/health')
  .then(res => res.json())
  .then(data => console.log('✅ Backend conectado:', data))
```

**c) Test de login**

- Ir a la página de login
- Ingresar credenciales
- ✅ Si funciona: ¡Todo listo!
- ❌ Si falla: Ver [Troubleshooting](#troubleshooting)

---

## 🎉 ¡Listo!

```
✅ Backend desplegado en Fly.io (gratis)
✅ Base de datos en Neon (gratis)
✅ Frontend en Vercel conectado
✅ HTTPS automático
✅ Costo total: $0.00/mes
```

---

## 🔧 Comandos Útiles

### Ver logs en tiempo real

```bash
fly logs -a invoices-backend
```

### Abrir dashboard de Fly.io

```bash
fly dashboard -a invoices-backend
```

### Ver estado de los servicios

```bash
fly status -a invoices-backend
```

### Actualizar secretos (variables de entorno)

```bash
fly secrets set CORS_ALLOWED_ORIGINS=https://nueva-url.vercel.app -a invoices-backend
```

### Redeploy manual

```bash
cd gateway-service
fly deploy
```

---

## 🐛 Troubleshooting

### Error CORS

```bash
# Verificar que la URL de Vercel sea exacta (con https://)
fly secrets set CORS_ALLOWED_ORIGINS=https://tu-app-exacta.vercel.app

# Redeploy
cd gateway-service && fly deploy
```

### Backend no responde

```bash
# Ver logs
fly logs -a invoices-backend

# Verificar que esté running
fly status -a invoices-backend

# Restart si es necesario
fly apps restart invoices-backend
```

### Error de base de datos

```bash
# Verificar connection string (debe incluir ?sslmode=require)
fly secrets list -a invoices-backend

# Actualizar si es necesario
fly secrets set USER_DB_URL=<nueva-url> -a invoices-backend
```

---

## 📚 Documentación Completa

Para más detalles, ver:
- 📖 **[DEPLOYMENT_VERCEL_GUIDE.md](./DEPLOYMENT_VERCEL_GUIDE.md)** - Guía completa paso a paso
- 📖 **[README.md](./README.md)** - Documentación general del proyecto

---

## 💡 Próximos Pasos

Una vez que el sistema esté funcionando, puedes:

1. **Agregar más servicios**:
   ```bash
   ./deploy-flyio.sh all
   ```

2. **Agregar dominio custom**:
   ```bash
   fly certs add tu-dominio.com -a invoices-backend
   ```

3. **Configurar Kafka** (para auditoría completa):
   - Crear cuenta en Upstash.com (10k mensajes/día gratis)
   - Configurar en `fly secrets`

4. **Monitoreo**:
   - [UptimeRobot](https://uptimerobot.com) para uptime monitoring
   - Fly.io dashboard para métricas

---

## ❓ ¿Necesitas ayuda?

- **Issues:** https://github.com/jefmonjor/invoices-back/issues
- **Fly.io Docs:** https://fly.io/docs
- **Neon Docs:** https://neon.tech/docs

---

**¡Tu aplicación está en producción! 🚀**

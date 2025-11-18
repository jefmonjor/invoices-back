# 🍎 Guía de Deployment para macOS

Esta guía te ayudará a hacer deployment de la aplicación Invoices Backend a Fly.io desde tu MacBook.

## 📋 Pre-requisitos

- macOS (cualquier versión reciente)
- Cuenta en [Fly.io](https://fly.io) (gratis)
- Terminal (iTerm2, Terminal nativa, etc.)

## 🚀 Opción 1: Script Automatizado (Recomendado)

El script `deploy-macos.sh` hace TODO por ti:

```bash
# 1. Ve al directorio del proyecto
cd ~/Documents/proyecto/invoices-back

# 2. Da permisos de ejecución
chmod +x deploy-macos.sh configure-secrets.sh deploy.sh

# 3. Ejecuta el script automatizado
./deploy-macos.sh
```

El script automáticamente:
- ✅ Instala Homebrew (si no lo tienes)
- ✅ Instala Fly CLI
- ✅ Te autentica en Fly.io
- ✅ Crea la app si no existe
- ✅ Configura los secrets (te preguntará)
- ✅ Hace el deployment
- ✅ Verifica que todo funcione

## 🔧 Opción 2: Paso a Paso Manual

### 1️⃣ Instalar Fly CLI

Opción A - Usando Homebrew (recomendado):
```bash
brew install flyctl
```

Opción B - Usando curl:
```bash
curl -L https://fly.io/install.sh | sh

# Añadir al PATH (si usas zsh - default en macOS Catalina+)
echo 'export FLYCTL_INSTALL="$HOME/.fly"' >> ~/.zshrc
echo 'export PATH="$FLYCTL_INSTALL/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# O si usas bash
echo 'export FLYCTL_INSTALL="$HOME/.fly"' >> ~/.bash_profile
echo 'export PATH="$FLYCTL_INSTALL/bin:$PATH"' >> ~/.bash_profile
source ~/.bash_profile
```

### 2️⃣ Autenticarse en Fly.io

```bash
fly auth login
```

Se abrirá tu navegador para hacer login. Si no tienes cuenta, créala (es gratis).

### 3️⃣ Configurar Secrets (solo la primera vez)

```bash
cd ~/Documents/proyecto/invoices-back
chmod +x configure-secrets.sh
./configure-secrets.sh
```

Este script configurará:
- JWT Secret (auto-generado)
- Conexión a PostgreSQL (NeonDB)
- Conexión a Redis (Upstash)
- Credenciales de S3/R2 (Cloudflare)
- CORS para el frontend

### 4️⃣ Hacer Deployment

```bash
chmod +x deploy.sh
./deploy.sh
```

El script:
1. Verifica que todo esté configurado
2. Te muestra un resumen de la configuración
3. Te pide confirmación
4. Ejecuta el deployment
5. Verifica que la app esté funcionando
6. Te muestra las URLs y comandos útiles

## 📊 Verificar el Deployment

### Health Check
```bash
curl https://invoices-monolith.fly.dev/actuator/health
```

Deberías ver:
```json
{"status":"UP"}
```

### Ver Logs
```bash
fly logs -a invoices-monolith
```

### Ver Status
```bash
fly status -a invoices-monolith
```

### Abrir Dashboard
```bash
fly open -a invoices-monolith
```

## 🔐 Credenciales por Defecto

Después del primer deployment, puedes hacer login con:

- **Email:** `admin@invoices.com`
- **Password:** `admin123`

⚠️ **IMPORTANTE:** Cambia estas credenciales después del primer login.

## 🌐 URLs de la Aplicación

- **Backend API:** https://invoices-monolith.fly.dev
- **Health Check:** https://invoices-monolith.fly.dev/actuator/health
- **Swagger UI:** https://invoices-monolith.fly.dev/swagger-ui.html
- **API Docs:** https://invoices-monolith.fly.dev/api-docs
- **Frontend:** https://invoices-frontend-vert.vercel.app

## 🐛 Troubleshooting

### Error: "fly: command not found"

Asegúrate de haber añadido fly al PATH:

```bash
# Para zsh (default macOS Catalina+)
echo 'export PATH="$HOME/.fly/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Para bash
echo 'export PATH="$HOME/.fly/bin:$PATH"' >> ~/.bash_profile
source ~/.bash_profile
```

### Error: "Not logged in"

```bash
fly auth login
```

### Error: "App not found"

El script debería crear la app automáticamente, pero puedes hacerlo manualmente:

```bash
fly apps create invoices-monolith
```

### Error durante el build

Ver los logs completos:

```bash
fly logs -a invoices-monolith
```

### La app no responde

Dale tiempo (30-60 segundos) para que inicie. Luego verifica:

```bash
# Ver si está corriendo
fly status -a invoices-monolith

# Ver logs
fly logs -a invoices-monolith

# Reiniciar si es necesario
fly apps restart invoices-monolith
```

## 🔄 Re-deployar Cambios

Después de hacer cambios en el código:

```bash
cd ~/Documents/proyecto/invoices-back
./deploy.sh
```

O manualmente:

```bash
fly deploy -c invoices-monolith/fly.toml -a invoices-monolith
```

## 📱 Comandos Útiles

```bash
# Ver apps
fly apps list

# Ver secrets
fly secrets list -a invoices-monolith

# Añadir/cambiar un secret
fly secrets set JWT_SECRET="nuevo-secret" -a invoices-monolith

# SSH a la instancia
fly ssh console -a invoices-monolith

# Escalar (cambiar recursos)
fly scale memory 1024 -a invoices-monolith

# Ver métricas
fly status -a invoices-monolith

# Detener la app
fly apps stop invoices-monolith

# Reiniciar la app
fly apps restart invoices-monolith
```

## 🆘 Soporte

- **Fly.io Docs:** https://fly.io/docs
- **Fly.io Community:** https://community.fly.io
- **Issues del proyecto:** https://github.com/jefmonjor/invoices-back/issues

## ✅ Checklist de Deployment

- [ ] Fly CLI instalado
- [ ] Autenticado en Fly.io
- [ ] Secrets configurados (`./configure-secrets.sh`)
- [ ] Deployment ejecutado (`./deploy.sh`)
- [ ] Health check OK (200)
- [ ] Frontend conectado
- [ ] Credenciales admin cambiadas

---

**¡Listo!** Tu aplicación debería estar corriendo en producción 🎉

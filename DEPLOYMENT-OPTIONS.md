# Opciones de Deployment para Invoices Backend

Este documento describe las diferentes estrategias de deployment disponibles para la aplicación Invoices Backend en Fly.io.

## Problema Actual

El deployment actual falla con el error:
```
Error: failed to fetch an image or build from source: error building: deadline_exceeded: context deadline exceeded
```

Esto ocurre porque el build de Maven durante el deployment en Fly.io está tardando más de 20 minutos (el timeout configurado).

## Soluciones Disponibles

### ✅ Opción 1: Build Local + Deploy Rápido (RECOMENDADO)

Esta es la opción más rápida y confiable para deployments en macOS.

#### Pasos:

1. **Compilar localmente** (2-3 minutos):
   ```bash
   ./build-local-fast.sh
   ```

2. **Deploy con JAR pre-compilado** (1-2 minutos):
   ```bash
   cd invoices-monolith
   fly deploy --dockerfile Dockerfile.prebuilt
   ```

#### Ventajas:
- ✅ Deployment extremadamente rápido (1-2 minutos vs 20+ minutos)
- ✅ Evita timeouts de Fly.io
- ✅ Control total sobre el build
- ✅ Aprovecha el poder de tu máquina local

#### Desventajas:
- ⚠️ Requiere Maven instalado localmente
- ⚠️ Necesitas compilar antes de cada deploy

---

### 🔄 Opción 2: Dockerfile Optimizado

El Dockerfile principal ha sido optimizado para reducir el tiempo de build.

#### Mejoras implementadas:
- Compilación paralela con 2 threads (`-T 2C`)
- Mayor memoria para Maven (`MAVEN_OPTS="-Xmx1024m"`)
- Compilación rápida sin debug info
- Mejor caché de dependencias

#### Uso:
```bash
cd invoices-monolith
fly deploy
```

#### Ventajas:
- ✅ Build automático en Fly.io
- ✅ No requiere herramientas locales
- ✅ Proceso simple de un solo comando

#### Desventajas:
- ⚠️ Todavía puede tardar 15-20 minutos
- ⚠️ Riesgo de timeout en proyectos grandes
- ⚠️ Requiere aumentar el timeout en fly.toml si falla

---

### 🌐 Opción 3: Deploy vía Web (Fly.io Dashboard)

Puedes desplegar usando GitHub Actions o el dashboard de Fly.io.

#### Pasos:

1. **Push a GitHub**:
   ```bash
   git add .
   git commit -m "Ready for deployment"
   git push origin main
   ```

2. **Configurar GitHub Actions** (crear `.github/workflows/deploy.yml`):
   ```yaml
   name: Deploy to Fly.io

   on:
     push:
       branches: [main]

   jobs:
     deploy:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v3
         - uses: superfly/flyctl-actions/setup-flyctl@master
         - run: flyctl deploy --remote-only
           env:
             FLY_API_TOKEN: ${{ secrets.FLY_API_TOKEN }}
   ```

3. **Configurar secrets** en GitHub:
   - Ve a Settings > Secrets > Actions
   - Añade `FLY_API_TOKEN` con tu token de Fly.io

#### Ventajas:
- ✅ Deployment automático con cada push
- ✅ Build en infraestructura de GitHub (más recursos)
- ✅ CI/CD completo

#### Desventajas:
- ⚠️ Requiere configuración inicial
- ⚠️ Depende de GitHub Actions

---

### 🔧 Opción 4: Aumentar Timeout de Build

Si prefieres seguir usando el build remoto, puedes aumentar el timeout.

#### Modificar `fly.toml`:
```toml
[build]
  dockerfile = "Dockerfile"
  build-timeout = "30m"  # Aumentar de 20m a 30m
```

#### Ventajas:
- ✅ Solución simple
- ✅ No cambia el workflow

#### Desventajas:
- ⚠️ Builds muy lentos
- ⚠️ Puede seguir fallando si el proyecto crece

---

## Recomendación Final

Para **macOS** y **desarrollo activo**:

1. **Usa la Opción 1** (Build Local + Deploy Rápido):
   ```bash
   ./build-local-fast.sh
   cd invoices-monolith && fly deploy --dockerfile Dockerfile.prebuilt
   ```

Para **CI/CD** y **producción**:

2. **Usa la Opción 3** (GitHub Actions):
   - Configura una vez y olvídate
   - Deployment automático con cada push

---

## Scripts Disponibles

- `./build-local-fast.sh` - Compilación local optimizada
- `./deploy-macos.sh` - Deployment completo (verifica dependencias)
- `./quick-deploy.sh` - Deploy rápido con verificación de auth
- `./run-tests.sh` - Ejecuta tests antes de deployment

---

## Troubleshooting

### Build local falla
```bash
# Instalar Maven
brew install maven

# Verificar Java 21
java -version
# Si no es Java 21, instalar:
brew install openjdk@21
```

### Deploy falla con timeout
```bash
# Opción 1: Usar build local
./build-local-fast.sh
cd invoices-monolith && fly deploy --dockerfile Dockerfile.prebuilt

# Opción 2: Aumentar timeout en fly.toml
# Cambiar build-timeout a "30m"
```

### Tests fallando
Los tests han sido arreglados para:
- Excluir auto-configuraciones innecesarias (Redis, Flyway)
- Mockear servicios externos (MinIO, Security)
- Usar H2 en memoria en lugar de PostgreSQL

Para ejecutar tests:
```bash
./run-tests.sh
```

---

## Próximos Pasos

1. ✅ Arreglar tests (COMPLETADO)
2. ✅ Optimizar Dockerfile (COMPLETADO)
3. 🔄 Probar build local
4. 🔄 Deploy con Dockerfile.prebuilt
5. ⏳ Configurar GitHub Actions (opcional)

---

## Contacto y Soporte

Si tienes problemas:
1. Revisa los logs: `fly logs -a invoices-monolith`
2. Verifica status: `fly status -a invoices-monolith`
3. Consulta la documentación: https://fly.io/docs/

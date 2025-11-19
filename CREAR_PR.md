# 🚀 Instrucciones para Crear el Pull Request

## Método Rápido (Recomendado)

### Opción 1: URL Directa

Haz clic aquí para crear el PR automáticamente:

**👉 https://github.com/jefmonjor/invoices-back/compare/main...claude/setup-spring-boot-invoices-01Xzi9FpmYqnjMKXXiyutfY7**

Luego:
1. Haz clic en el botón verde **"Create pull request"**
2. El título ya está sugerido: `fix: Railway deployment and local development setup`
3. Copia y pega la descripción del archivo `PR_DESCRIPTION.md` (o usa una descripción breve)
4. Haz clic en **"Create pull request"** de nuevo
5. Haz clic en **"Merge pull request"**
6. Haz clic en **"Confirm merge"**

### Opción 2: Desde la Página del Repositorio

1. Ve a: https://github.com/jefmonjor/invoices-back
2. Busca el banner amarillo que dice **"Compare & pull request"**
3. Haz clic en él
4. Sigue los pasos 2-6 de la Opción 1

## ✅ Descripción Breve del PR (si no quieres copiar todo el archivo)

```
## Summary

Fixes critical Railway deployment issue and local development setup.

### Critical Fixes:
- ✅ Fix PORT environment variable (changed from SERVER_PORT to PORT for Railway)
- ✅ Configure health checks for Railway (/actuator/health/readiness)
- ✅ Remove Fly.io files with hardcoded credentials (security fix)
- ✅ Fix Swagger UI access (403 errors)
- ✅ Upgrade springdoc-openapi to 2.7.0 for Spring Boot 3.4.4

### Why This Fixes Railway:
Railway injects `PORT` variable, not `SERVER_PORT`. App was listening on port 8080 instead of Railway's dynamic port.

### Files Modified:
- application.yml - PORT env + health checks
- OpenApiConfig.java - Fixed scope + nested placeholder
- SecurityConfig.java - Swagger endpoints
- pom.xml - springdoc-openapi 2.7.0
- railway.json - Health check path

### Deleted (Security):
9 Fly.io scripts with hardcoded production credentials

After merge, Railway will automatically rebuild and deploy successfully.
```

## 📋 Commits Incluidos

1. `aaca5d6` - Fix PORT env variable (Railway requirement) ⭐ CRITICAL
2. `f9f40a8` - Configure Railway health check readiness probe
3. `1111b33` - Upgrade springdoc-openapi to 2.7.0
4. `d54ee7a` - Remove nested placeholder in OpenApiConfig
5. `3b0d9db` - Allow public access to Swagger endpoints
6. `15d2020` - Fix OpenApiConfig productionServer scope
7. `d25a20a` - Remove Fly.io files with hardcoded credentials

## 🎯 Qué Pasará Después del Merge

1. **Railway detectará el merge** a main
2. **Reconstruirá la aplicación** con el fix del PORT
3. **La app escuchará en el puerto correcto** (el que Railway asigne dinámicamente)
4. **Los health checks pasarán** en `/actuator/health/readiness`
5. **¡Despliegue exitoso!** ✅

Tiempo estimado: 2-3 minutos después del merge.

## ⚡ Verificar el Despliegue

Después del merge, verifica en Railway:

1. Ve a tu proyecto en Railway
2. Revisa los logs - deberías ver:
   ```
   Tomcat initialized with port XXXXX (http)  # XXXXX será el puerto dinámico de Railway
   ```
   En lugar de:
   ```
   Tomcat initialized with port 8080 (http)  # Este es el problema actual
   ```

3. El health check debería pasar:
   ```
   GET /actuator/health/readiness → 200 OK
   ```

## 🔗 Enlaces Útiles

- **Crear PR**: https://github.com/jefmonjor/invoices-back/compare/main...claude/setup-spring-boot-invoices-01Xzi9FpmYqnjMKXXiyutfY7
- **Repositorio**: https://github.com/jefmonjor/invoices-back
- **Descripción completa**: Ver archivo `PR_DESCRIPTION.md`

---

**¿Problemas?** Si el botón de merge está deshabilitado, verifica:
- No hay conflictos con main (no debería haber)
- Tienes permisos para hacer merge en el repositorio
- La branch está actualizada

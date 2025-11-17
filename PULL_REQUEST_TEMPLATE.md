## 🚀 Arquitectura Free-Tier Completa + Deployment Híbrido

Esta PR migra completamente el backend a una **arquitectura 100% gratuita** usando servicios cloud de primer nivel, con una estrategia de deployment híbrido optimizado.

---

## 📊 Resumen de Cambios

**19 archivos modificados** | **+1,814 líneas** | **-214 líneas**

### 🔄 Migraciones Principales

| Anterior | Nuevo | Beneficio |
|----------|-------|-----------|
| Kafka + Zookeeper | **Redis Streams** (Upstash) | 10k commands/día gratis vs 10k messages/día |
| MinIO local | **Cloudflare R2** | 10GB storage gratis + S3-compatible |
| PostgreSQL local | **Neon** | Serverless, auto-scale to zero |
| Eureka Server | **Deshabilitado** | No necesario en cloud |
| Deployment único | **Híbrido Fly.io + Render** | Mejor rendimiento + más servicios gratis |

---

## 🏗️ Nueva Arquitectura

### Stack Tecnológico

```
Frontend:  Vercel (gratis)
Backend:   Fly.io (3 servicios) + Render (2 servicios)
Database:  Neon PostgreSQL (3GB gratis)
Events:    Upstash Redis Streams (10k commands/día)
Storage:   Cloudflare R2 (10GB gratis)
```

### Distribución de Servicios

**Fly.io** (Servicios Críticos - Siempre Activos):
- ✅ Gateway Service → `invoices-backend.fly.dev`
- ✅ User Service → `invoices-user-service.fly.dev`
- ✅ Invoice Service → `invoices-invoice-service.fly.dev`

**Render** (Servicios Secundarios - Auto-scale):
- ✅ Document Service → `invoices-document-service.onrender.com`
- ✅ Trace Service → `invoices-trace-service.onrender.com`

---

## 🔧 Cambios Técnicos

### Event Streaming: Kafka → Redis Streams

**Archivos nuevos:**
- `invoice-service/config/RedisConfig.java`
- `invoice-service/events/InvoiceEventProducer.java`
- `trace-service/config/RedisConfig.java`
- `trace-service/config/RedisStreamConfig.java`
- `trace-service/events/InvoiceEventConsumer.java`

**Características:**
- Redis Streams para eventos de facturas
- Dead Letter Queue (DLQ) para eventos fallidos
- Retry logic con exponential backoff
- Compatible con Upstash Redis (ya configurado)

### Object Storage: MinIO → Cloudflare R2

**Archivos nuevos:**
- `document-service/config/S3Config.java`

**Características:**
- S3-compatible API
- Soporta R2, MinIO, AWS S3
- Bucket auto-creation con graceful fallback

### Database: PostgreSQL → Neon

**Cambios en `application.yml`:**
- Soporte para `SPRING_DATASOURCE_URL` completo
- SSL required por defecto
- HikariCP optimizado para serverless
- 4 databases separadas: userdb, invoicedb, documentdb, tracedb

### Infraestructura

**Docker Compose actualizado:**
- Redis en lugar de Kafka/Zookeeper
- Variables de entorno actualizadas
- Eureka deshabilitado por defecto

**Deployment híbrido:**
- `deploy-flyio-free-tier.sh` - Script automatizado para Fly.io
- `render.yaml` - Blueprint para Render (solo Document + Trace)
- `HYBRID_DEPLOYMENT_GUIDE.md` - Guía completa paso a paso

---

## 📚 Documentación Incluida

### 🆕 Nuevos Documentos

1. **`FREE_TIER_ARCHITECTURE.md`** (424 líneas)
   - Arquitectura completa free-tier
   - Diagrama de componentes
   - Setup de cada servicio gratuito
   - Limitaciones y consideraciones
   - Plan de escalamiento

2. **`HYBRID_DEPLOYMENT_GUIDE.md`** (386 líneas)
   - Deployment paso a paso en Fly.io
   - Deployment paso a paso en Render
   - Configuración de secrets
   - Testing end-to-end
   - Troubleshooting completo

3. **`deploy-flyio-free-tier.sh`** (116 líneas)
   - Script automatizado de deployment
   - Deploy de 3 servicios en orden correcto
   - Validaciones y health checks
   - Instrucciones post-deployment

### 📝 Documentos Actualizados

- `.env.production.example` - Upstash Redis configurado
- `render.yaml` - Solo servicios secundarios
- Todos los `application.yml` - Neon DB + Redis

---

## 💰 Costos

| Servicio | Límite Free Tier | Costo Mensual |
|----------|------------------|---------------|
| Fly.io (3 apps) | 256MB RAM c/u | **$0** |
| Render (2 apps) | 750h/mes c/u | **$0** |
| Neon PostgreSQL | 3GB storage | **$0** |
| Upstash Redis | 10k commands/día | **$0** |
| Cloudflare R2 | 10GB + 1M uploads | **$0** |
| **TOTAL** | | **$0/mes** |

---

## ✅ Testing

### Servicios Configurados

- ✅ Neon PostgreSQL - 4 databases creadas
- ✅ Upstash Redis - `subtle-parrot-38179.upstash.io`
- ✅ Cloudflare R2 - Pendiente configuración de usuario
- ✅ Fly.io - Cuentas listas
- ✅ Render - Cuentas listas

### Próximos Pasos

1. Ejecutar `./deploy-flyio-free-tier.sh` para desplegar en Fly.io
2. Configurar variables de entorno (ver guía)
3. Deploy en Render usando `render.yaml`
4. Testing end-to-end

---

## 🎯 Ventajas de Esta Arquitectura

### vs Todo en Render
- ✅ Servicios críticos siempre activos (Fly.io)
- ✅ Mejor tiempo de respuesta inicial
- ✅ No hay cold starts en autenticación

### vs Todo en Fly.io
- ✅ 5 servicios en lugar de 3 (límite free tier)
- ✅ Servicios secundarios con auto-scale a 0
- ✅ Optimización de recursos

### vs Arquitectura Anterior
- ✅ $0/mes vs costos de hosting
- ✅ Auto-scaling incluido
- ✅ SSL automático
- ✅ Deploy global con CDN

---

## 📖 Referencias

- [Fly.io Docs](https://fly.io/docs/)
- [Render Docs](https://render.com/docs)
- [Neon Docs](https://neon.tech/docs)
- [Upstash Redis Docs](https://docs.upstash.com/redis)
- [Cloudflare R2 Docs](https://developers.cloudflare.com/r2/)

---

## 🔍 Checklist de Review

- [x] Kafka → Redis Streams migración completa
- [x] MinIO → Cloudflare R2 ready
- [x] PostgreSQL → Neon compatible
- [x] Eureka deshabilitado
- [x] Docker compose actualizado
- [x] Scripts de deployment creados
- [x] Documentación completa
- [x] Variables de entorno actualizadas
- [x] Fly.toml configurados
- [x] render.yaml actualizado

---

**Ready to deploy! 🚀**

Ver `HYBRID_DEPLOYMENT_GUIDE.md` para instrucciones detalladas de deployment.

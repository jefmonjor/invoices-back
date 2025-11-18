# Migración a Arquitectura Monolítica

## 📋 Resumen

Este documento describe la migración de la arquitectura de microservicios a una arquitectura monolítica para simplificar el despliegue y la operación.

## 🎯 Motivación

La arquitectura de microservicios original presentaba los siguientes desafíos:

1. **Complejidad Operacional**: Múltiples servicios requieren múltiples despliegues y configuraciones
2. **Costos**: Cada servicio consume recursos independientes
3. **Desarrollo**: Más difícil de desarrollar y debugear localmente
4. **Coordinación**: Cambios que afectan múltiples servicios requieren coordinación compleja

## 🔄 Cambios Realizados

### Arquitectura Anterior (Microservicios)

```
┌─────────────────┐
│  Gateway (8080) │
└────────┬────────┘
         │
    ┌────┴────┬────────┬──────────┐
    │         │        │          │
┌───▼──┐ ┌───▼──┐ ┌──▼────┐ ┌───▼───┐
│ User │ │Invoice│ │Document│ │ Trace │
│ 8082 │ │ 8081 │ │  8083  │ │ 8084  │
└──────┘ └───────┘ └────────┘ └───────┘

+ Eureka Server (8761)
+ Config Server (8888)

= 7 servicios independientes
```

### Arquitectura Nueva (Monolito)

```
┌─────────────────────────────────┐
│   Invoices Monolith (8080)      │
│                                 │
│  ┌─────────────────────────┐   │
│  │  Security & Gateway     │   │
│  └─────────────────────────┘   │
│                                 │
│  ┌──────┐ ┌────────┐ ┌───────┐│
│  │ User │ │Invoice │ │Document││
│  └──────┘ └────────┘ └───────┘│
│  ┌──────┐                      │
│  │Trace │                      │
│  └──────┘                      │
└─────────────────────────────────┘

= 1 servicio unificado
```

## 📦 Estructura del Monolito

### Paquetes

```
com.invoices/
├── InvoicesApplication.java      # Main class
├── config/                        # Configuración global
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   ├── S3Config.java
│   └── OpenApiConfig.java
├── security/                      # Seguridad unificada
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
├── user/                          # Módulo User
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── invoice/                       # Módulo Invoice
│   ├── presentation/
│   ├── domain/
│   ├── infrastructure/
│   └── client/
├── document/                      # Módulo Document
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── config/
└── trace/                         # Módulo Trace
    ├── controller/
    ├── service/
    ├── repository/
    ├── entity/
    └── events/
```

## 🗄️ Base de Datos

### Antes: 4 bases de datos separadas
- `userdb`
- `invoicedb`
- `documentdb`
- `tracedb`

### Ahora: 1 base de datos con todas las tablas
- `invoices` (todas las tablas en una sola DB)

Las migraciones Flyway de todos los servicios se consolidaron en `/src/main/resources/db/migration`.

## 🚀 Despliegue

### Antes

```bash
# Desplegar 5 servicios en Fly.io
flyctl deploy -c gateway-service/fly.toml
flyctl deploy -c user-service/fly.toml
flyctl deploy -c invoice-service/fly.toml

# Desplegar 2 servicios en Render
render deploy document-service
render deploy trace-service
```

### Ahora

```bash
# Un solo despliegue
cd invoices-monolith
flyctl deploy
```

## 🔧 Configuración

### Variables de Entorno Simplificadas

Antes teníamos que configurar variables por servicio. Ahora todo está consolidado:

```bash
flyctl secrets set \
  SPRING_DATASOURCE_URL="..." \
  DB_USERNAME="..." \
  DB_PASSWORD="..." \
  JWT_SECRET="..." \
  REDIS_HOST="..." \
  REDIS_PASSWORD="..." \
  S3_ENDPOINT="..." \
  S3_ACCESS_KEY="..." \
  S3_SECRET_KEY="..."
```

## 📊 Comparación

| Aspecto | Microservicios | Monolito |
|---------|----------------|----------|
| **Servicios** | 7 | 1 |
| **Puertos** | 8080, 8081, 8082, 8083, 8084, 8761, 8888 | 8080 |
| **Bases de datos** | 4 separadas | 1 unificada |
| **Despliegues** | 7 independientes | 1 único |
| **Complejidad** | Alta | Baja |
| **Tiempo de despliegue** | ~15-20 min | ~5 min |
| **Memoria requerida** | ~2GB total | ~512MB |
| **Costo mensual** | ~$10-15 | ~$0-5 |
| **Debugging** | Complejo | Simple |
| **Latencia entre servicios** | 50-100ms | 0ms (local) |

## ✅ Ventajas del Monolito

1. **Simplicidad**: Un solo servicio, una sola configuración
2. **Desarrollo más rápido**: No necesitas levantar múltiples servicios
3. **Debugging más fácil**: Todo el código en un solo proceso
4. **Mejor rendimiento**: Sin latencia de red entre componentes
5. **Costos reducidos**: Menos recursos necesarios
6. **Deploy más simple**: Un solo comando
7. **Testing más fácil**: Tests de integración más simples

## ⚠️ Consideraciones

### Cuándo usar Microservicios

Considera volver a microservicios si:
- El equipo crece a más de 10 desarrolladores
- Necesitas escalar servicios de forma independiente
- Diferentes servicios tienen ciclos de despliegue muy diferentes
- Necesitas tecnologías diferentes por servicio

### Cuándo usar Monolito

El monolito es ideal si:
- Equipo pequeño (1-10 desarrolladores) ✅
- Startup o proyecto nuevo ✅
- Presupuesto limitado ✅
- Necesitas desarrollo rápido ✅
- Todos los componentes están fuertemente acoplados ✅

## 🔄 Proceso de Migración

1. ✅ Crear estructura del monolito
2. ✅ Copiar código de todos los servicios
3. ✅ Consolidar configuración
4. ✅ Unificar migraciones de base de datos
5. ✅ Integrar seguridad del gateway
6. ✅ Crear Dockerfile único
7. ✅ Configurar Fly.io
8. ⏳ Testing de integración
9. ⏳ Despliegue a producción
10. ⏳ Migración de datos (si necesario)

## 📝 Próximos Pasos

1. **Probar localmente**:
   ```bash
   cd invoices-monolith
   mvn clean package
   java -jar target/invoices-monolith-1.0.0.jar
   ```

2. **Configurar servicios externos**:
   - Neon PostgreSQL
   - Upstash Redis
   - Cloudflare R2

3. **Desplegar a Fly.io**:
   ```bash
   flyctl deploy
   ```

4. **Verificar endpoints**:
   - Health: `https://invoices-monolith.fly.dev/actuator/health`
   - Swagger: `https://invoices-monolith.fly.dev/swagger-ui.html`

## 🆘 Soporte

Si encuentras problemas durante la migración:

1. Revisa los logs: `flyctl logs`
2. Verifica las variables de entorno: `flyctl secrets list`
3. Consulta el README del monolito: `invoices-monolith/README.md`

## 📚 Referencias

- [Documentación del Monolito](./invoices-monolith/README.md)
- [Fly.io Docs](https://fly.io/docs)
- [Spring Boot Docs](https://spring.io/projects/spring-boot)

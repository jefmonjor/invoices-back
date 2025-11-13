# 🚀 PLAN DE ACCIÓN EJECUTIVO - invoices-back

**Fecha:** 13 Noviembre 2025
**Estado:** Requiere Implementación Completa
**Prioridad:** CRÍTICA
**Tiempo Estimado:** 4-5 semanas (1 developer full-time)

---

## 📌 SITUACIÓN ACTUAL

```
┌─────────────────────────────────────┐
│ PROYECTO: invoices-back             │
│ Estado: SCAFFOLDING (0% funcional)  │
│ Riesgo: CRÍTICO                     │
│ Bloqueadores: 5 CRÍTICOS            │
│ Problemas Altos: 9 PROBLEMAS        │
└─────────────────────────────────────┘
```

### Resumen del Estado
- ✅ Arquitectura microservicios bien diseñada
- ✅ OpenAPI specs completas y documentadas
- ✅ Infraestructura configurada (Eureka, Config, Kafka)
- ❌ **0% de código de negocio implementado**
- ❌ **No hay seguridad (JWT, CORS, auth)**
- ❌ **No hay validación ni manejo de errores**
- ❌ **Bases de datos compartidas (anti-patrón)**
- ❌ **Credenciales hardcodeadas**

---

## 🎯 OBJETIVOS DEL PROYECTO

### Objetivo Principal
**Convertir scaffolding en backend FUNCIONAL y LISTO PARA FRONTEND en 4-5 semanas**

### Criterios de Éxito
1. ✅ Autenticación JWT funcionando
2. ✅ Todos los CRUD endpoints implementados
3. ✅ 70%+ test coverage
4. ✅ Validación completa de datos
5. ✅ Manejo centralizado de errores
6. ✅ CORS configurado para frontend
7. ✅ Documentación completa
8. ✅ Deployable con docker-compose

---

## 🔴 BLOQUEADORES CRÍTICOS - ACCIONES INMEDIATAS

### BLOQUEADOR #1: No Hay Código Implementado
**Impacto:** Proyecto no funciona
**Acción:**
```bash
# Crear estructura de clases (90-110 archivos .java)
# Estimar: 2-3 semanas paralelas
# Priority: CRÍTICA - Parallelizar en sprints
```

### BLOQUEADOR #2: Seguridad Ausente
**Impacto:** Endpoints desprotegidos
**Acción:**
```bash
# SEMANA 1 - COMPLETAR ANTES DE CUALQUIER OTRA COSA
# 1. Agregar Spring Security + JWT
# 2. Implementar JwtTokenProvider y JwtAuthenticationFilter
# 3. Crear AuthController (/auth/login)
# 4. Configurar CORS en gateway
# 5. Proteger todos los endpoints con @PreAuthorize
```

### BLOQUEADOR #3: Bases de Datos Compartidas
**Impacto:** Imposible escalar microservicios
**Acción:**
```bash
# Antes de persistencia:
# 1. Crear 4 BDs separadas: user_db, invoice_db, document_db, trace_db
# 2. Configurar connection strings por servicio
# 3. Crear scripts de inicialización
# 4. Verificar conexiones antes de proceder

# Conexiones:
# user-service    → postgresql://localhost:5433/user_db
# invoice-service → postgresql://localhost:5434/invoice_db
# document-service → postgresql://localhost:5435/document_db
# trace-service    → postgresql://localhost:5436/trace_db
```

### BLOQUEADOR #4: Credenciales Hardcodeadas
**Impacto:** Riesgo de seguridad CRÍTICO
**Acción:**
```bash
# INMEDIATO:
# 1. Crear .gitignore (prevenir commits accidentales)
# 2. Externalizar credenciales → variables de entorno
# 3. Crear .env.example para documentación
# 4. Actualizar application.yml para usar ${DB_USER}, ${DB_PASS}

# Ejemplo application.yml:
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

### BLOQUEADOR #5: Validación y Manejo de Errores Ausente
**Impacto:** Datos inconsistentes en BD
**Acción:**
```bash
# FASE 3 (Semana 3):
# 1. Crear GlobalExceptionHandler en gateway
# 2. Agregar spring-boot-starter-validation
# 3. Validar todos los DTOs con @Valid, @NotNull, @Size, etc.
# 4. Implementar custom validators si necesario
```

---

## 📋 PLAN DE ACCIÓN POR SEMANA

### ⏱️ SEMANA 1: SEGURIDAD (CRÍTICA)

**Goal:** Backend tiene autenticación JWT funcionando

#### Día 1-2: Foundation
```bash
# gateway-service
1. Agregar dependencias:
   - spring-boot-starter-security
   - jjwt 0.12.3 (JWT)
   - spring-cloud-starter-gateway-mvc (ya presente)

2. Crear estructura:
   - gateway-service/src/main/java/com/invoices/gateway_service/
     ├── config/
     │   ├── SecurityConfig.java
     │   └── CorsConfig.java
     ├── security/
     │   ├── JwtTokenProvider.java
     │   ├── JwtAuthenticationFilter.java
     │   └── UserDetailsServiceImpl.java
     └── exception/
         └── JwtAuthenticationException.java
```

#### Día 3: JWT Implementation
```java
✅ JwtTokenProvider
  - generateToken(UserDetails) → token
  - validateToken(String token) → boolean
  - getUsernameFromToken(String token) → String

✅ JwtAuthenticationFilter
  - extract JWT from Authorization header
  - validate token
  - set SecurityContext

✅ SecurityConfig
  - sessionCreationPolicy: STATELESS
  - authorize all /auth/* requests
  - require auth for /api/**
  - add JWT filter
```

#### Día 4-5: Authentication Endpoints
```java
✅ AuthController (/auth)
  - POST /login → LoginRequest → LoginResponse (token)
  - POST /register → RegisterRequest → User
  - POST /refresh → refresh JWT token

✅ LoginRequest
  - username (String)
  - password (String)

✅ LoginResponse
  - accessToken (String)
  - tokenType (String) = "Bearer"
  - expiresIn (Long)
```

#### Entregables Semana 1
- [ ] Spring Security configurado
- [ ] JWT generación/validación funcionando
- [ ] AuthController con /login endpoint
- [ ] CORS configurado
- [ ] Tests de seguridad (sin token = 401)
- [ ] Docker: eureka-server + config-server corriendo
- [ ] Postman: login endpoint testeable

---

### ⏱️ SEMANA 2-3: IMPLEMENTACIÓN PARALELA

**Goal:** Backend tiene todas las entidades, servicios y controllers

#### Equipo: 2 Developers en Paralelo

##### Developer A: User & Client Services
```bash
user-service/

1. Entidades (Día 1)
   - User.java (@Entity)
   - Role.java (@Entity)
   - Client.java (@Entity)
   - Repositories

2. Services (Día 2-3)
   - UserService: create, update, delete, findById, findAll
   - ClientService: CRUD
   - UserDetailsServiceImpl: para Spring Security

3. Controllers (Día 4-5)
   - UserController: GET, POST, PUT, DELETE
   - ClientController: CRUD
   - Validación con @Valid

4. Mappers (Día 5)
   - UserMapper (MapStruct)
   - ClientMapper (MapStruct)

5. Tests (Día 5)
   - UserServiceTest (unitarios)
   - UserControllerTest (integration)
```

##### Developer B: Invoice & Document Services
```bash
invoice-service/

1. Entidades (Día 1)
   - Invoice.java (@Entity)
   - InvoiceItem.java (@Entity)
   - Repositories

2. Services (Día 2-3)
   - InvoiceService: CRUD + businesslogic
   - PdfGeneratorService: JasperReports integration
   - Kafka producer: publish InvoiceCreatedEvent

3. Controllers (Día 4)
   - InvoiceController: GET, POST, PUT, DELETE
   - Validación con @Valid

4. Mappers (Día 4)
   - InvoiceMapper (MapStruct)
   - InvoiceItemMapper (MapStruct)

5. Tests (Día 5)
   - InvoiceServiceTest (unitarios)
   - InvoiceControllerTest (integration)

document-service/

1. Entidades (Día 1)
   - Document.java (@Entity)

2. MinIO Configuration (Día 2)
   - MinioConfig.java (bean)
   - DocumentService: upload/download/delete

3. Tests (Día 3)
   - Upload/download tests
```

#### Configuración Crítica (Semana 2-3)

**application.yml Updates:**
```yaml
# user-service/application.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5433/user_db
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # NO update en producción
    show-sql: false       # NO en producción
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}

# invoice-service/application.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5434/invoice_db
  jpa:
    hibernate:
      ddl-auto: validate
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

#### Entregables Semana 2-3
- [ ] Todas las entidades JPA implementadas
- [ ] Todos los Repositories funcionales
- [ ] Todos los Services con lógica de negocio
- [ ] Todos los Controllers (CRUD completo)
- [ ] Mappers con MapStruct
- [ ] Validación con DTOs
- [ ] Tests unitarios básicos
- [ ] docker-compose con 4 BDs separadas

---

### ⏱️ SEMANA 3-4: FINALIZACIÓN

**Goal:** Backend completamente funcional, testeado y documentado

#### Enfoque: Calidad y Polish

```bash
trace-service/
1. TraceEntity + Repository
2. Kafka Consumer (@KafkaListener)
3. TraceService
4. TraceController (GET con paginación)
5. TraceMapper

GlobalExceptionHandler
1. @ControllerAdvice
2. Manejo de excepciones: validation, not found, generic
3. ErrorResponse JSON

Logging
1. logback.xml con rolling files
2. @Slf4j en servicios
3. Logging estructurado con MDC
4. Enmascarar datos sensibles

Testing - Full Coverage
1. Unit tests: ServiceTests (70%+)
2. Integration tests: @SpringBootTest
3. Controller tests: @WebMvcTest
4. Repository tests: @DataJpaTest
5. End-to-End: Postman collection

Documentation
1. README.md (setup, deployment, troubleshooting)
2. JavaDoc en clases públicas
3. OpenAPI specs actualizadas y verificadas
4. Postman collection con ejemplos
5. Architecture Decision Records (ADR)
```

#### Entregables Semana 4
- [ ] 70%+ test coverage
- [ ] GlobalExceptionHandler implementado
- [ ] Logging estructurado
- [ ] Documentación completa
- [ ] Postman collection funcional
- [ ] docker-compose listo para producción
- [ ] All endpoints tested y documentados

---

## 📊 ROADMAP VISUAL

```
SEMANA 1:
┌──────────────────┐
│ Security (JWT)   │  ← CRÍTICO
│ AuthController   │  ← BLOCKER
│ CORS Config      │
└──────────────────┘
        ↓
SEMANA 2-3:
┌─────────────────────────────────────┐
│ Entities (Usuarios, Facturas, Docs) │
│ Services (Lógica de negocio)        │
│ Controllers (APIs REST)              │
│ Mappers (MapStruct)                  │
│ Validación (DTOs)                    │
└─────────────────────────────────────┘
        ↓
SEMANA 4:
┌────────────────────────────────────┐
│ Exception Handling (Global)        │
│ Logging (Structured)               │
│ Testing (70%+ coverage)            │
│ Documentation (Complete)           │
│ Docker (Production-ready)          │
└────────────────────────────────────┘
        ↓
✅ BACKEND READY FOR FRONTEND
```

---

## 🛠️ STACK TÉCNICO RECOMENDADO

```
Framework:          Spring Boot 3.4.4
Language:           Java 21 LTS
Build:              Maven
Database:           PostgreSQL 15+ (4 instancias separadas)
Message Queue:      Apache Kafka
Service Discovery:  Eureka
API Documentation:  OpenAPI 3.0.3 + Springdoc
Mapping:            MapStruct 1.6.0
Testing:            JUnit 5 + Mockito + AssertJ
Logging:            SLF4J + Logback
Security:           Spring Security + JWT (jjwt)
Container:          Docker + docker-compose
```

---

## 📦 ESTRUCTURA DE CARPETAS FINAL

```
invoices-back/
├── eureka-server/              ✅ Listo
├── config-server/              ✅ Listo
├── gateway-service/            🔨 WIP (Security)
│   └── SecurityConfig, CorsConfig, AuthController
├── user-service/               🔨 WIP (Implementation)
│   ├── entity/ (User, Role, Client)
│   ├── repository/
│   ├── service/
│   ├── controller/
│   ├── mapper/
│   ├── dto/
│   ├── exception/
│   └── security/
├── invoice-service/            🔨 WIP (Implementation)
│   └── Similar structure
├── document-service/           🔨 WIP (Implementation)
│   └── Similar structure
├── trace-service/              🔨 WIP (Implementation)
│   └── Similar structure
├── docker-compose.yml          📝 Todo
├── BUENAS_PRACTICAS_Y_RECOMENDACIONES.md   ✅ Creado
├── PLAN_ACCION_EJECUTIVO.md    ✅ Este documento
└── .gitignore                  📝 Critical
```

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

### FASE 1: SEGURIDAD (Semana 1)
```
SPRING SECURITY:
  [ ] Agregar spring-boot-starter-security a gateway pom.xml
  [ ] Agregar jjwt dependency
  [ ] Crear JwtTokenProvider.java
  [ ] Crear JwtAuthenticationFilter.java
  [ ] Crear SecurityConfig.java
  [ ] Crear CorsConfig.java

AUTHENTICATION:
  [ ] Crear AuthController (/auth/login)
  [ ] Crear LoginRequest y LoginResponse DTOs
  [ ] Crear UserDetailsServiceImpl
  [ ] Crear Role entity en user-service
  [ ] Conectar auth con DB (user-service)

TESTING:
  [ ] Test: login sin credenciales → 401
  [ ] Test: login correcto → token
  [ ] Test: sin token → 401 en endpoints
  [ ] Test: CORS headers presentes
```

### FASE 2: ENTIDADES (Semana 2)
```
USER-SERVICE:
  [ ] User entity
  [ ] Role entity
  [ ] Client entity
  [ ] UserRepository
  [ ] ClientRepository
  [ ] RoleRepository

INVOICE-SERVICE:
  [ ] Invoice entity
  [ ] InvoiceItem entity
  [ ] InvoiceRepository
  [ ] InvoiceItemRepository

DOCUMENT-SERVICE:
  [ ] Document entity
  [ ] DocumentRepository

TRACE-SERVICE:
  [ ] Trace entity
  [ ] TraceRepository

DATABASE:
  [ ] Crear 4 BDs separadas
  [ ] Configurar spring.datasource.url por servicio
  [ ] Scripts de inicialización
  [ ] Verificar conexiones
```

### FASE 3: SERVICIOS (Semana 2-3)
```
USER-SERVICE:
  [ ] UserService (CRUD)
  [ ] ClientService (CRUD)
  [ ] UserDetailsServiceImpl
  [ ] PasswordEncoder (BCrypt)

INVOICE-SERVICE:
  [ ] InvoiceService (CRUD)
  [ ] Cálculo de totales
  [ ] Cambios de estado
  [ ] InvoiceItemService
  [ ] KafkaProducer para eventos

DOCUMENT-SERVICE:
  [ ] DocumentService
  [ ] MinioConfig bean
  [ ] Upload/Download/Delete logic

TRACE-SERVICE:
  [ ] TraceService
  [ ] KafkaConsumer listener
```

### FASE 4: CONTROLADORES (Semana 3)
```
USER-SERVICE:
  [ ] UserController (GET /{id}, GET, POST, PUT, DELETE)
  [ ] ClientController (CRUD)
  [ ] Validación con @Valid

INVOICE-SERVICE:
  [ ] InvoiceController (CRUD)
  [ ] GET /invoices?page=0&size=10
  [ ] POST /invoices/generate-pdf
  [ ] Validación

DOCUMENT-SERVICE:
  [ ] DocumentController (POST, GET, DELETE)

TRACE-SERVICE:
  [ ] TraceController (GET con filtros)
```

### FASE 5: VALIDACIÓN Y ERRORES (Semana 3)
```
VALIDATION:
  [ ] Agregar spring-boot-starter-validation
  [ ] Validar todos los Request DTOs
  [ ] Custom validators si necesario
  [ ] @Valid en todos los endpoints

EXCEPTION HANDLING:
  [ ] GlobalExceptionHandler (@ControllerAdvice)
  [ ] ErrorResponse DTO
  [ ] Manejo: MethodArgumentNotValidException
  [ ] Manejo: EntityNotFoundException
  [ ] Manejo: Generic exceptions
```

### FASE 6: MAPSTRUCT (Semana 3)
```
MAPPERS:
  [ ] InvoiceMapper
  [ ] UserMapper
  [ ] ClientMapper
  [ ] DocumentMapper
  [ ] TraceMapper

DTOs:
  [ ] Todos los Response DTOs
  [ ] Todos los Request DTOs
  [ ] Validaciones en DTOs
```

### FASE 7: LOGGING (Semana 3-4)
```
CONFIGURATION:
  [ ] logback.xml
  [ ] Rolling files
  [ ] Niveles por paquete
  [ ] MDC configuration

IMPLEMENTATION:
  [ ] @Slf4j en servicios
  [ ] Logging estructurado
  [ ] Enmascarar datos sensibles
  [ ] Trazabilidad entre servicios
```

### FASE 8: TESTING (Semana 4)
```
UNIT TESTS (70%+ coverage):
  [ ] ServiceTests con Mockito
  [ ] Casos positivos
  [ ] Casos de error
  [ ] Validaciones

INTEGRATION TESTS:
  [ ] @SpringBootTest
  [ ] Flujos completos
  [ ] Con BD H2

CONTROLLER TESTS:
  [ ] @WebMvcTest
  [ ] Request/Response
  [ ] Validaciones
  [ ] Seguridad (sin token = 401)

REPOSITORY TESTS:
  [ ] @DataJpaTest
  [ ] Queries
  [ ] Relaciones
```

### FASE 9: DOCUMENTACIÓN (Semana 4)
```
CODE:
  [ ] JavaDoc en todas las clases públicas
  [ ] Comentarios en métodos complejos

PROJECT:
  [ ] README.md (setup, deployment)
  [ ] CONTRIBUTING.md
  [ ] Architecture.md (ADR)

API:
  [ ] OpenAPI specs actualizadas
  [ ] Swagger UI verificado
  [ ] Postman collection
  [ ] Ejemplos de request/response
```

### FASE 10: INFRAESTRUCTURA (Semana 4)
```
DOCKER:
  [ ] Dockerfile por servicio
  [ ] docker-compose.yml
  [ ] 4 PostgreSQL containers
  [ ] Kafka container
  [ ] Eureka server
  [ ] Config server

CONFIGURATION:
  [ ] .gitignore completamente
  [ ] Variables de entorno (.env.example)
  [ ] application-prod.yml
  [ ] application-test.yml
  [ ] Health checks (/actuator/health)
```

---

## 💻 COMANDOS DE INICIO

```bash
# Clonar/Actualizar
git clone https://github.com/jefmonjor/invoices-back.git
cd invoices-back

# Crear rama de trabajo
git fetch origin claude/revisa-que-011CV4nceDGc2JJoj53PvbEZ
git checkout claude/revisa-que-011CV4nceDGc2JJoj53PvbEZ

# Build
mvn clean package -DskipTests

# Executar servicios (una vez implementados)
docker-compose up -d

# Verificar servicios
curl http://localhost:8080/actuator/health
curl http://localhost:8761  # Eureka
curl http://localhost:8888  # Config server

# Login para obtener token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Usar token
curl http://localhost:8080/api/invoices/1 \
  -H "Authorization: Bearer <token>"
```

---

## 🎯 MÉTRICAS DE ÉXITO

### Semana 1
```
✅ Spring Security + JWT funcionando
✅ /auth/login devuelve token válido
✅ Sin token = 401 Unauthorized
✅ CORS headers en respuestas
```

### Semana 2-3
```
✅ 90+ clases Java implementadas
✅ Todos los CRUD endpoints existe
✅ Validación en todos los endpoints
✅ Tests: 50%+ coverage
```

### Semana 4
```
✅ 70%+ test coverage
✅ GlobalExceptionHandler implementado
✅ Documentación completa
✅ docker-compose.yml funcional
✅ Backend listo para integración
```

---

## 🔗 RECURSOS Y REFERENCIAS

### Documentación Oficial
- [Spring Security](https://spring.io/projects/spring-security)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Cloud Config](https://spring.io/projects/spring-cloud-config)
- [MapStruct](https://mapstruct.org/)
- [OpenAPI 3.0](https://spec.openapis.org/)

### Guías Internas
- `BUENAS_PRACTICAS_Y_RECOMENDACIONES.md` ← Leer primero
- OpenAPI specs en cada servicio

---

## 📞 SOPORTE Y PREGUNTAS

Si tienes dudas sobre implementación:
1. Consulta `BUENAS_PRACTICAS_Y_RECOMENDACIONES.md`
2. Revisa OpenAPI specs del servicio
3. Crea issue en el repositorio

---

**Última actualización:** 13 Noviembre 2025
**Versión:** 1.0
**Estado:** Ready to implement


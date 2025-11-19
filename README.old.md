# Sistema de Gestión de Facturas - Backend Monolítico

**Sistema de facturación empresarial** construido con arquitectura monolítica modular, Clean Architecture y Spring Boot 3.4.4 + Java 21.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Clean Architecture](https://img.shields.io/badge/Architecture-Clean-blue.svg)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
[![Code Coverage](https://img.shields.io/badge/Coverage-90%25+-success.svg)](https://www.jacoco.org/)

---

## 📋 Tabla de Contenidos

- [¿Qué es este sistema?](#-qué-es-este-sistema)
- [Características Principales](#-características-principales)
- [Arquitectura](#-arquitectura)
- [Stack Tecnológico](#-stack-tecnológico)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación y Ejecución](#-instalación-y-ejecución)
- [Docker Compose](#-docker-compose)
- [Variables de Entorno](#-variables-de-entorno)
- [API Endpoints](#-api-endpoints)
- [Testing](#-testing)
- [Deployment](#-deployment)

---

## 🎯 ¿Qué es este sistema?

Sistema **empresarial de gestión de facturas** (invoicing) que permite:

- **Crear, editar y eliminar facturas** con múltiples ítems
- **Generar PDFs profesionales** de facturas con JasperReports
- **Gestionar usuarios, clientes y empresas**
- **Almacenar documentos PDF** en MinIO/Cloudflare R2 (compatible S3)
- **Auditar todas las operaciones** con trazabilidad completa
- **Arquitectura modular** con Clean Architecture
- **Seguridad con JWT** y Spring Security
- **APIs REST documentadas** con OpenAPI 3.0

---

## ✨ Características Principales

### Funcionales
- ✅ **CRUD completo de facturas** (crear, leer, actualizar, eliminar)
- ✅ **Generación automática de PDFs** con plantillas JasperReports
- ✅ **Gestión de usuarios y autenticación** con JWT
- ✅ **Almacenamiento de documentos** en MinIO/Cloudflare R2
- ✅ **Trazabilidad de operaciones** con eventos Redis Streams
- ✅ **Validación de datos** con Bean Validation
- ✅ **Gestión de clientes y empresas**
- ✅ **Cálculo automático de totales** e impuestos

### Técnicas
- ✅ **Clean Architecture** (Domain, Infrastructure, Presentation)
- ✅ **Arquitectura monolítica modular** con 4 módulos independientes
- ✅ **Hexagonal Architecture** (Ports & Adapters)
- ✅ **Event-driven** con Redis Streams
- ✅ **Base de datos única** PostgreSQL con Flyway migrations
- ✅ **Tests unitarios y de integración** (>90% coverage con JaCoCo)
- ✅ **Documentación OpenAPI 3.0** con Swagger UI
- ✅ **Dependency Injection** con Spring Framework

---

## 🏗️ Arquitectura

### Arquitectura Monolítica Modular

```
┌──────────────────────────────────────────────────────┐
│                     FRONTEND                         │
│          (React, Angular, Vue, etc.)                 │
└───────────────────────┬──────────────────────────────┘
                        │
                        │ HTTP/REST (Puerto 8080)
                        ▼
┌────────────────────────────────────────────────────────┐
│           INVOICES MONOLITH (Puerto 8080)              │
│                                                        │
│  ┌──────────────────────────────────────────────┐    │
│  │     Security & CORS Configuration            │    │
│  │     (JWT, Spring Security, Filters)          │    │
│  └──────────────────────────────────────────────┘    │
│                                                        │
│  ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌───────┐  │
│  │  User   │  │ Invoice │  │ Document │  │ Trace │  │
│  │ Module  │  │ Module  │  │  Module  │  │Module │  │
│  │         │  │         │  │          │  │       │  │
│  │ Clean   │  │ Clean   │  │  Clean   │  │ Clean │  │
│  │  Arch   │  │  Arch   │  │   Arch   │  │  Arch │  │
│  └────┬────┘  └────┬────┘  └─────┬────┘  └───┬───┘  │
│       │            │              │            │      │
└───────┼────────────┼──────────────┼────────────┼──────┘
        │            │              │            │
        │            │              │            │
        ▼            ▼              ▼            ▼
┌──────────────────────────────────────────────────────┐
│            PostgreSQL Database (5432)                 │
│     (Tablas: users, invoices, documents, audit_logs) │
└──────────────────────────────────────────────────────┘

        ┌─────────────┐          ┌────────────────┐
        │   Redis     │          │ MinIO / R2     │
        │  Streams    │          │  (S3 Storage)  │
        │   (6379)    │          │     (9000)     │
        └─────────────┘          └────────────────┘
```

### Clean Architecture por Módulo

Cada módulo (User, Invoice, Document, Trace) sigue Clean Architecture:

```
module/
├── domain/                          # Capa de Dominio (Reglas de Negocio)
│   ├── entities/                    # Entidades de dominio (POJOs puros)
│   ├── usecases/                    # Casos de uso (lógica de negocio)
│   ├── ports/                       # Interfaces (inversión de dependencias)
│   ├── events/                      # Eventos de dominio
│   └── services/                    # Servicios de dominio
│
├── infrastructure/                  # Capa de Infraestructura (Frameworks)
│   ├── persistence/
│   │   ├── entities/                # Entidades JPA (@Entity)
│   │   ├── repositories/            # Spring Data JPA
│   │   └── mappers/                 # Mappers Domain ↔ JPA
│   ├── external/                    # Clientes externos (JasperReports)
│   ├── storage/                     # Almacenamiento (MinIO adapters)
│   ├── events/                      # Event consumers (Redis Streams)
│   ├── security/                    # Adaptadores de seguridad
│   └── config/                      # Configuración Spring
│
└── presentation/                    # Capa de Presentación (Controllers)
    ├── controllers/                 # REST Controllers
    ├── dto/                         # Data Transfer Objects
    └── mappers/                     # Mappers Domain ↔ DTO
```

### Principios de Clean Architecture Aplicados

1. **Independencia de Frameworks**: El dominio no depende de Spring, JPA, Redis, etc.
2. **Testabilidad**: La lógica de negocio es testeable sin frameworks
3. **Independencia de UI**: Los controllers pueden cambiarse sin afectar el dominio
4. **Independencia de BD**: Se puede cambiar PostgreSQL por MySQL sin cambiar el dominio
5. **Regla de Dependencia**: Las dependencias apuntan hacia adentro (hacia el dominio)

---

## 🛠️ Stack Tecnológico

### Backend
- **Java 21** - Lenguaje de programación
- **Spring Boot 3.4.4** - Framework de aplicación
- **Spring Data JPA** - ORM y acceso a datos
- **Spring Security** - Autenticación y autorización
- **JWT (jjwt)** - Tokens de seguridad

### Base de Datos
- **PostgreSQL 16** - Base de datos relacional
- **Flyway** - Migraciones de base de datos

### Mensajería y Cache
- **Redis 7** - Event streaming (reemplaza Kafka)
- **Redis Streams** - Event-driven communication

### Almacenamiento
- **MinIO** - Almacenamiento S3-compatible (desarrollo)
- **Cloudflare R2** - Producción (gratis hasta 10GB)
- **AWS SDK S3** - Cliente S3

### Reporting
- **JasperReports 6.21.3** - Generación de PDFs
- **iText 2.1.7** - Generación de documentos PDF

### Testing
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **JaCoCo** - Cobertura de código

### Documentación
- **Springdoc OpenAPI** - Documentación de APIs
- **Swagger UI** - Interfaz interactiva de APIs

### DevOps
- **Docker & Docker Compose** - Containerización
- **Fly.io** - Deployment en producción
- **GitHub Actions** - CI/CD (opcional)

---

## 📁 Estructura del Proyecto

```
invoices-back/
├── invoices-monolith/                     # Aplicación monolítica
│   ├── src/main/java/com/invoices/
│   │   ├── InvoicesApplication.java       # Main class
│   │   ├── config/                        # Configuración global
│   │   │   ├── CorsConfig.java
│   │   │   ├── JpaConfig.java
│   │   │   ├── RedisStreamConfig.java
│   │   │   ├── S3Config.java
│   │   │   └── OpenApiConfig.java
│   │   ├── security/                      # Seguridad JWT
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtUtil.java
│   │   │   └── SecurityConfig.java
│   │   ├── exception/                     # Exception handlers globales
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── ErrorResponse.java
│   │   │
│   │   ├── user/                          # Módulo User
│   │   │   ├── domain/
│   │   │   │   ├── entities/User.java
│   │   │   │   ├── ports/UserRepository.java
│   │   │   │   └── usecases/CreateUserUseCase.java (7 use cases)
│   │   │   ├── infrastructure/
│   │   │   │   ├── persistence/
│   │   │   │   │   ├── entities/UserJpaEntity.java
│   │   │   │   │   ├── repositories/UserRepositoryImpl.java
│   │   │   │   │   └── mappers/UserJpaMapper.java
│   │   │   │   └── config/UseCaseConfiguration.java
│   │   │   └── presentation/
│   │   │       ├── controllers/UserController.java
│   │   │       ├── dto/UserDTO.java (6 DTOs)
│   │   │       └── mappers/UserDtoMapper.java
│   │   │
│   │   ├── invoice/                       # Módulo Invoice
│   │   │   ├── domain/
│   │   │   │   ├── entities/Invoice.java, InvoiceItem.java
│   │   │   │   ├── ports/InvoiceRepository.java, PdfGenerator.java
│   │   │   │   └── usecases/ (8 use cases)
│   │   │   ├── infrastructure/
│   │   │   │   ├── persistence/
│   │   │   │   ├── external/JasperPdfGenerator.java
│   │   │   │   └── config/
│   │   │   └── presentation/
│   │   │
│   │   ├── document/                      # Módulo Document
│   │   │   ├── domain/
│   │   │   │   ├── entities/Document.java, FileContent.java
│   │   │   │   ├── ports/DocumentRepository.java, FileStorageService.java
│   │   │   │   ├── validation/PdfValidator.java
│   │   │   │   └── usecases/ (5 use cases)
│   │   │   ├── infrastructure/
│   │   │   │   ├── persistence/
│   │   │   │   ├── storage/MinioFileStorageService.java
│   │   │   │   └── config/
│   │   │   └── presentation/
│   │   │
│   │   └── trace/                         # Módulo Trace
│   │       ├── domain/
│   │       │   ├── entities/AuditLog.java
│   │       │   ├── events/InvoiceEvent.java
│   │       │   ├── ports/AuditLogRepository.java
│   │       │   ├── services/RetryPolicy.java
│   │       │   └── usecases/ (6 use cases)
│   │       ├── infrastructure/
│   │       │   ├── persistence/
│   │       │   ├── events/RedisInvoiceEventConsumer.java
│   │       │   └── config/
│   │       └── presentation/
│   │
│   ├── src/main/resources/
│   │   ├── application.yml                # Configuración principal
│   │   ├── db/migration/                  # Migraciones Flyway
│   │   │   ├── V1__create_users_tables.sql
│   │   │   ├── V2__create_invoices_tables.sql
│   │   │   ├── V3__create_documents_table.sql
│   │   │   ├── V4__create_audit_logs_table.sql
│   │   │   └── V5__add_indexes.sql
│   │   └── jasper-templates/              # Plantillas JasperReports
│   │       ├── invoice.jrxml
│   │       └── invoice.jasper
│   │
│   ├── src/test/java/                     # Tests (>90% coverage)
│   ├── pom.xml                            # Maven dependencies
│   ├── Dockerfile                         # Dockerfile multi-stage
│   └── fly.toml                           # Configuración Fly.io
│
├── docker-compose.yml                     # Orquestación Docker
├── .env.example                           # Variables de entorno ejemplo
├── .gitignore
├── README.md                              # Este archivo
├── TESTING_GUIDE.md                       # Guía de testing
├── ENVIRONMENT_VARIABLES.md               # Documentación de variables
└── MONOLITH_MIGRATION.md                  # Historia de la migración
```

---

## 🚀 Requisitos Previos

- **Java 21** (OpenJDK o Oracle)
- **Maven 3.9+**
- **Docker & Docker Compose** (para desarrollo local)
- **PostgreSQL 16** (opcional si usas Docker)
- **Redis 7** (opcional si usas Docker)
- **MinIO** (opcional si usas Docker)

---

## 📦 Instalación y Ejecución

### Opción 1: Desarrollo Local con Docker Compose (Recomendado)

```bash
# 1. Clonar repositorio
git clone <repo-url>
cd invoices-back

# 2. Copiar variables de entorno
cp .env.example .env

# 3. Levantar toda la infraestructura
docker-compose up -d

# 4. Ver logs del monolito
docker logs -f invoices-monolith

# 5. Acceder a la aplicación
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# MinIO Console: http://localhost:9001
```

### Opción 2: Desarrollo Local sin Docker

```bash
# 1. Instalar PostgreSQL, Redis y MinIO localmente

# 2. Configurar variables de entorno
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/invoices
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export REDIS_HOST=localhost
export S3_ENDPOINT=http://localhost:9000
export JWT_SECRET=your-super-secret-jwt-key-min-32-chars

# 3. Compilar y ejecutar
cd invoices-monolith
mvn clean install
mvn spring-boot:run

# 4. Acceder a la aplicación
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

---

## 🐳 Docker Compose

El archivo `docker-compose.yml` incluye:

### Servicios de Infraestructura
- **PostgreSQL** (puerto 5432) - Base de datos única
- **Redis** (puerto 6379) - Event streaming
- **MinIO** (puertos 9000/9001) - Almacenamiento S3
- **MinIO Setup** - Crea bucket inicial automáticamente

### Servicio de Aplicación
- **invoices-monolith** (puerto 8080) - Aplicación Spring Boot

### Comandos útiles

```bash
# Levantar servicios
docker-compose up -d

# Ver logs
docker-compose logs -f invoices-monolith

# Parar servicios
docker-compose down

# Parar y eliminar volúmenes (CUIDADO: borra datos)
docker-compose down -v

# Reconstruir imagen del monolito
docker-compose build invoices-monolith
docker-compose up -d invoices-monolith

# Ver estado de servicios
docker-compose ps

# Acceder a PostgreSQL
docker exec -it invoices-postgres psql -U postgres -d invoices
```

---

## 🔐 Variables de Entorno

### Variables Principales

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `SPRING_DATASOURCE_URL` | URL de PostgreSQL | `jdbc:postgresql://localhost:5432/invoices` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Password de PostgreSQL | `postgres` |
| `REDIS_HOST` | Host de Redis | `localhost` |
| `REDIS_PORT` | Puerto de Redis | `6379` |
| `S3_ENDPOINT` | Endpoint de MinIO/R2 | `http://localhost:9000` |
| `S3_ACCESS_KEY` | Access Key de S3 | `minioadmin` |
| `S3_SECRET_KEY` | Secret Key de S3 | `minioadmin123` |
| `S3_BUCKET_NAME` | Nombre del bucket | `invoices-documents` |
| `JWT_SECRET` | Secret para JWT (min 32 chars) | `your-super-secret...` |
| `JWT_EXPIRATION_MS` | Expiración del token en ms | `3600000` (1 hora) |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos | `http://localhost:3000` |

Ver archivo `ENVIRONMENT_VARIABLES.md` para la lista completa.

---

## 🌐 API Endpoints

### Documentación Interactiva
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Endpoints Principales

#### User & Auth
```
POST   /api/auth/register          # Registrar usuario
POST   /api/auth/login             # Login (obtener JWT)
GET    /api/users                  # Listar usuarios
GET    /api/users/{id}             # Obtener usuario
PUT    /api/users/{id}             # Actualizar usuario
DELETE /api/users/{id}             # Eliminar usuario
```

#### Invoice
```
GET    /api/invoices               # Listar facturas
POST   /api/invoices               # Crear factura
GET    /api/invoices/{id}          # Obtener factura
PUT    /api/invoices/{id}          # Actualizar factura
DELETE /api/invoices/{id}          # Eliminar factura
GET    /api/invoices/{id}/pdf      # Generar PDF
```

#### Document
```
POST   /api/documents              # Subir documento PDF
GET    /api/documents/{id}         # Obtener metadata
GET    /api/documents/{id}/download # Descargar PDF
GET    /api/documents?invoiceId=X  # Listar por factura
DELETE /api/documents/{id}          # Eliminar documento
```

#### Trace (Audit Logs)
```
GET    /api/traces                 # Listar logs (paginado)
GET    /api/traces/{id}            # Obtener log
GET    /api/traces?invoiceId=X     # Logs por factura
GET    /api/traces?clientId=Y      # Logs por cliente
GET    /api/traces?eventType=Z     # Logs por tipo
```

#### Actuator (Monitoreo)
```
GET    /actuator/health            # Health check
GET    /actuator/info              # Información de la app
```

---

## 🧪 Testing

### Ejecutar todos los tests

```bash
cd invoices-monolith
mvn clean test
```

### Ver reporte de cobertura

```bash
mvn jacoco:report
open target/site/jacoco/index.html
```

### Tests por módulo

```bash
# User module
mvn test -Dtest="com.invoices.user.**"

# Invoice module
mvn test -Dtest="com.invoices.invoice.**"

# Document module
mvn test -Dtest="com.invoices.document.**"

# Trace module
mvn test -Dtest="com.invoices.trace.**"
```

### Cobertura de Código

El monolito mantiene >90% de cobertura de código con JaCoCo:
- Tests unitarios para todos los Use Cases
- Tests de integración para Controllers
- Tests de mappers y validaciones
- Tests de event consumers

Ver `TESTING_GUIDE.md` para más detalles.

---

## 🚢 Deployment

### Deployment en Fly.io

```bash
# 1. Instalar Fly CLI
curl -L https://fly.io/install.sh | sh

# 2. Login
fly auth login

# 3. Configurar secrets
fly secrets set JWT_SECRET=<your-secret>
fly secrets set DB_PASSWORD=<db-password>
fly secrets set S3_ACCESS_KEY=<r2-key>
fly secrets set S3_SECRET_KEY=<r2-secret>

# 4. Deploy
cd invoices-monolith
fly deploy

# 5. Ver logs
fly logs
```

### Variables de Producción

En producción, usar servicios gestionados:
- **PostgreSQL**: Fly Postgres, Neon, Supabase
- **Redis**: Upstash Redis (gratis)
- **Storage**: Cloudflare R2 (gratis hasta 10GB)

---

## 📚 Documentación Adicional

- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Guía completa de testing
- **[ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md)** - Variables de entorno
- **[MONOLITH_MIGRATION.md](MONOLITH_MIGRATION.md)** - Historia de la migración

---

## 🏆 Ventajas de Clean Architecture

### 1. Testabilidad
- Lógica de negocio testeable sin frameworks
- Mocks fáciles de crear (interfaces)
- Tests rápidos (sin levantar Spring Context)

### 2. Mantenibilidad
- Separación clara de responsabilidades
- Código organizado por módulos funcionales
- Fácil de entender y modificar

### 3. Flexibilidad
- Cambiar PostgreSQL por MySQL: solo cambiar adapters
- Cambiar MinIO por S3: solo cambiar adapter
- Cambiar Redis por RabbitMQ: solo cambiar adapter

### 4. Escalabilidad
- Módulos independientes pueden extraerse a microservicios
- Fácil migración incremental
- Preparado para crecer

---

## 👥 Contribuciones

Este es un proyecto educativo. Para contribuir:

1. Fork del proyecto
2. Crear feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push al branch (`git push origin feature/AmazingFeature`)
5. Abrir Pull Request

---

## 📝 Licencia

Este proyecto es de código abierto y está disponible bajo licencia MIT.

---

## 📧 Contacto

Para preguntas o soporte, contactar al equipo de desarrollo.

---

## 🙏 Agradecimientos

- Clean Architecture por Robert C. Martin (Uncle Bob)
- Spring Framework Team
- JasperReports Community
- Fly.io por hosting gratuito
- Cloudflare por R2 gratuito
